package io.mapzone.controller.vm.provisions;

import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.utils.URIBuilder;

import io.mapzone.controller.http.ForwardRequest;
import io.mapzone.controller.http.HttpProxyProvision;
import io.mapzone.controller.ops.StartProcessOperation;
import io.mapzone.controller.ops.StopProcessOperation;
import io.mapzone.controller.provision.Context;
import io.mapzone.controller.provision.Provision;
import io.mapzone.controller.vm.repository.ProcessRecord;
import io.mapzone.controller.vm.repository.ProjectInstanceIdentifier;
import io.mapzone.controller.vm.repository.ProjectInstanceRecord;

/**
 * 
 * 
 * @author Falko Bräutigam
 */
public class ProcessRunning
        extends HttpProxyProvision {

    private static Log log = LogFactory.getLog( ProcessRunning.class );

    public static final String              NO_HOST = "_no_host_";

    private Context<URI>                    targetUri;
    
    private Context<ProjectInstanceRecord>  instance;

    private Context<ProcessRecord>          process;

    /** Prevents multiple runs. */
    private Context<ProcessRunning>         checked;

    private Status                          cause;

    
    @Override
    public boolean init( Provision failed, @SuppressWarnings("hiding") Status cause ) {
        this.cause = cause;
        return failed instanceof ForwardRequest
                && cause != null
                && !checked.isPresent()
                //&& process.isPresent()
                /*&& cause.getCause().equals( OkToForwardRequest.NO_PROCESS )*/;
    }

    
    @Override
    public Status execute() throws Exception {
        // find instance -> process
        ProjectInstanceIdentifier pid = new ProjectInstanceIdentifier( request.get() );
        instance.set( vmRepo().findInstance( pid )
                .orElseThrow( () -> new IllegalStateException( "No project instance found for: " + pid ) ) );

        instance.get().homePath.get();  // force (pessimistic) lock on instance
        process.set( instance.get().process.get() );

        // stop
        if (process.isPresent()) {
            log.warn( "Killing process without checking OS process!" );
            StopProcessOperation op = new StopProcessOperation();
            op.vmRepo.set( vmRepo() );
            op.process.set( process.get() );
            op.execute( null, null );
        }
        
        // start
        StartProcessOperation op2 = new StartProcessOperation();
        op2.instance.set( instance.get() );        
        op2.execute( null, null );
        process.set( op2.process.get() );

        targetUri.set( new URIBuilder().setScheme( "http" )
                .setHost( instance.get().host.get().inetAddress.get() )
                .setPort( process.get().port.get() )
                .build() );
        ProcessStarted.targetUris.remove( pid );

        checked.set( this );
        return OK_STATUS;
    }
    
}

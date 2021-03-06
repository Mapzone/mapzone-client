package io.mapzone.controller.vm.runtime;

import java.util.Properties;
import org.jclouds.Constants;
import org.jclouds.ContextBuilder;
import org.jclouds.byon.BYONApiMetadata;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.config.ComputeServiceProperties;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.OsFamily;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.ssh.SshClient;
import org.jclouds.sshj.config.SshjSshClientModule;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.ImmutableSet;

import org.polymap.core.runtime.Lazy;
import org.polymap.core.runtime.LockedLazyInit;
import org.polymap.core.runtime.cache.Cache;
import org.polymap.core.runtime.cache.CacheConfig;

/**
 * Configuration of the global JClouds contexts.
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class JCloudsRuntime {

    private static final Log log = LogFactory.getLog( JCloudsRuntime.class );
    
    public static final Lazy<JCloudsRuntime>  instance = new LockedLazyInit( () -> new JCloudsRuntime() );
    
    // instance *******************************************
    
    private ComputeServiceContext       context;
    
    private Cache<String,AutoDisconnectSshClient> sshClients;
    
    
    protected JCloudsRuntime() {
        Properties contextProperties = new Properties();

        StringBuilder nodes = new StringBuilder();
        nodes.append( "nodes:\n" );
        nodes.append( "    - id: local\n" );
        nodes.append( "      name: The local machine\n" );
        nodes.append( "      hostname: localhost\n" );
        nodes.append( "      os_arch: " ).append( System.getProperty( "os.arch" ) ).append( "\n" );
        nodes.append( "      os_family: " ).append( OsFamily.LINUX ).append( "\n" );
        nodes.append( "      os_description: " ).append( System.getProperty( "os.name" ) ).append( "\n" );
        nodes.append( "      os_version: " ).append( System.getProperty( "os.version" ) ).append( "\n" );
        nodes.append( "      group: " ).append( "ssh" ).append( "\n" );
        nodes.append( "      tags:\n" );
        nodes.append( "          - local\n" );
        nodes.append( "      username: " ).append( System.getProperty( "user.name" ) ).append( "\n" );
        nodes.append( "      credential_url: file://" ).append( System.getProperty( "user.home" ) ).append( "/.ssh/id_rsa" ).append( "\n" );

        contextProperties.setProperty( "byon.nodes", nodes.toString() );
        contextProperties.setProperty( Constants.PROPERTY_CONNECTION_TIMEOUT, "10000" );
        //contextProperties.setProperty( ComputeServiceProperties.TIMEOUT_NODE_RUNNING, "5000" );
        contextProperties.setProperty( ComputeServiceProperties.TIMEOUT_PORT_OPEN, "5000" );
        
        context = ContextBuilder.newBuilder( new BYONApiMetadata() )
                .overrides( contextProperties )
                .modules( ImmutableSet.of( new SshjSshClientModule(), new SLF4JLoggingModule() ) )
                .build( ComputeServiceContext.class );

        sshClients = CacheConfig.defaults().initSize( 32 ).createCache();
                
//        sshClients = CacheBuilder.newBuilder()
//                .initialCapacity( 32 )
//                .expireAfterAccess( 60, TimeUnit.SECONDS )
//                .removalListener( notification -> {
//                    log.info( "SSH: removed " + notification.getKey() );
//                    ((SshClient)notification.getValue()).disconnect();
//                })
//                .build();
    }


    public ComputeService computeService() {
        return context.getComputeService();
    }

    
    public SshClient sshForNode( String nodeId ) {
        return sshClients.get( nodeId, k -> {
            log.warn( "SSH: create new for " + nodeId );
            NodeMetadata node = computeService().getNodeMetadata( nodeId );
            SshClient sshClient = context.utils().sshForNode().apply( node );
            return new AutoDisconnectSshClient( sshClient );
        }).sshClient;
    }


    /**
     * 
     */
    class AutoDisconnectSshClient {
        
        public SshClient        sshClient;

        protected AutoDisconnectSshClient( SshClient sshClient ) {
            this.sshClient = sshClient;
        }

        @Override
        protected void finalize() throws Throwable {
            log.warn( "SSH: disconnect on finalize() -  " + sshClient.getHostAddress() );
            sshClient.disconnect();
            sshClient = null;
        }
    }
    
}

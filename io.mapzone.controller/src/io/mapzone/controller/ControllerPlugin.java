package io.mapzone.controller;

import org.eclipse.ui.plugin.AbstractUIPlugin;

import org.polymap.core.security.SecurityContext;
import org.polymap.core.security.StandardConfiguration;

import org.polymap.rhei.batik.toolkit.MinWidthConstraint;

import org.osgi.framework.BundleContext;

/**
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class ControllerPlugin extends AbstractUIPlugin {

	public static final String ID = "io.mapzone.controller"; //$NON-NLS-1$

	/** Default minimum width of sections. */
	public static final MinWidthConstraint MIN_WIDTH = new MinWidthConstraint( 500, 0 );
	
	private static ControllerPlugin        instance;
	

	public static ControllerPlugin instance() {
    	return instance;
    }

	
    public void start( BundleContext context ) throws Exception {
		super.start( context );
		instance = this;        
		
		// JAAS config: no dialog; let LoginDashlet create UI
        SecurityContext.registerConfiguration( () -> new StandardConfiguration() {
            @Override
            public String getConfigName() {
                return SecurityContext.SERVICES_CONFIG_NAME;
            }
        });

	}

    
	public void stop(BundleContext context) throws Exception {
		instance = null;
		super.stop(context);
	}

}

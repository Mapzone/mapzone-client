/*
 * polymap.org Copyright (C) 2016, the @authors. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software
 * Foundation; either version 3.0 of the License, or (at your option) any later
 * version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 */
package io.mapzone.arena.share;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Track {@link HttpService} instances and register {@link ShareInfoServlet} when
 * found.
 *
 * @author Steffen Stundzig
 */
public class ShareInfoServletStarter
        extends ServiceTracker<HttpService,Object> {

    private static Log         log   = LogFactory.getLog( ShareInfoServletStarter.class );

    public static final String ALIAS = "/shareinfo";

    private ShareInfoServlet   servlet;


    public ShareInfoServletStarter( BundleContext context ) {
        super( context, HttpService.class.getName(), null );
    }


    @Override
    public Object addingService( ServiceReference<HttpService> reference ) {
        HttpService service = (HttpService)super.addingService( reference );
        registerServlet( service );
        return service;
    }


    protected void registerServlet( HttpService service ) {
        try {
            assert servlet == null;
            // TODO load user/project default connection data or geocode
            // implementations
            servlet = new ShareInfoServlet();
            service.registerServlet( ALIAS, servlet, null, null );
            log.info( "Registered share info service at " + ALIAS );
        }
        catch (Exception e) {
            throw new RuntimeException( e );
        }
    }
}

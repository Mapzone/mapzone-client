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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.swt.widgets.Composite;

import org.polymap.rhei.batik.dashboard.Dashboard;
import org.polymap.rhei.batik.dashboard.DashletSite;
import org.polymap.rhei.batik.dashboard.DefaultDashlet;

/**
 * Share the map link with facebook
 *
 * @author Steffen Stundzig
 */
public class MapDashlet
        extends DefaultDashlet {

    private static Log log = LogFactory.getLog( MapDashlet.class );

    @Override
    public void init( DashletSite site ) {
        site.isExpandable.set( true );
        site.border.set( false );
        site.title.set( "map" );
        super.init( site );
    }
    
    @Override
    public void createContents( Composite parent ) {
        getSite().toolkit().createLabel( parent, "map" );
        Dashboard mapDashboard = new Dashboard( getSite().panelSite(), "foo" );
        mapDashboard.addDashlet( new IframeMapDashlet() );
        mapDashboard.addDashlet( new FacebookMapDashlet() );
        mapDashboard.createContents( parent );

    }
}

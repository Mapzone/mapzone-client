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

import java.util.List;
import com.google.common.collect.Lists;

import org.polymap.core.runtime.session.SessionSingleton;

import io.mapzone.arena.share.app.EMailSharelet;
import io.mapzone.arena.share.app.EmbedSharelet;
import io.mapzone.arena.share.app.FacebookSharelet;
import io.mapzone.arena.share.app.GeoServerSharelet;

/**
 * Extension point to add more sharelet classes.
 *
 * @author Steffen Stundzig
 */
public class Sharelets
        extends SessionSingleton {

    private final static List<Class<? extends Sharelet>> available = Lists.newArrayList( 
            FacebookSharelet.class,
            EMailSharelet.class,
            EmbedSharelet.class, 
            GeoServerSharelet.class );

    
    // instance *******************************************
    
    public final static Sharelets instance() {
        return instance( Sharelets.class );
    }


    public List<Sharelet> get() {
        List<Sharelet> sharelets = Lists.newArrayList();
        for (Class<? extends Sharelet> clazz : available) {
            Sharelet instance;
            try {
                instance = clazz.newInstance();
            }
            catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException( e );
            }
            sharelets.add( instance );
        }
        return sharelets;
    }
}

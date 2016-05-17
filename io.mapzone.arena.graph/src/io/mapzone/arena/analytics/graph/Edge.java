/* 
 * polymap.org
 * Copyright (C) 2016, the @authors. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3.0 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package io.mapzone.arena.analytics.graph;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * a single edge.
 * 
 *
 * @author Steffen Stundzig
 */
public class Edge {

    private final Node nodeA;

    private final Node nodeB;

    // private Map<String, String> properties;
    private final String key;


    public Edge( final String key, final Node nodeA, final Node nodeB ) {
        this.key = key;
        this.nodeA = nodeA;
        this.nodeB = nodeB;
    }


    public Node nodeA() {
        return nodeA;
    }


    public Node nodeB() {
        return nodeB;
    }
    //
    // public Map<String,String> properties() {
    // return properties;
    // }


    public String key() {
        return key;
    }
}
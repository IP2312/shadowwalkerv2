package org.example.shadowwalkerv2.model;

import lombok.Data;


import java.util.LinkedHashSet;

@Data
public class Path {
    private final long id;
    private LinkedHashSet<RouteNode> nodes;
    private final double length;
    private double shadePct;

    public Path(long id, LinkedHashSet<RouteNode> nodes, double length) {
        this.id = id;
        this.nodes = nodes;
        this.length = length;
    }

    public int getNrNodes(){
         return nodes.size();
    }

    public boolean isEmpty(){
        return nodes.isEmpty();
    }

}

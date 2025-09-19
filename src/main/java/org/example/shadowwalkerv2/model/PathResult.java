package org.example.shadowwalkerv2.model;

import java.util.List;

public class PathResult {
    public double length;
    public  double cost;
    public  List<Long> pathIds;

   public PathResult(double length, double cost, List<Long> pathIds) {
       this.length = length;
       this.cost = cost;
        this.pathIds = pathIds;
    }
}


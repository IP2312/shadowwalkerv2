package org.example.shadowwalkerv2.model;

import java.util.List;

public class PathResult {

    public  double cost;
    public  List<Long> pathIds;

   public PathResult(double cost, List<Long> pathIds) {
        this.cost = cost;
        this.pathIds = pathIds;
    }
}


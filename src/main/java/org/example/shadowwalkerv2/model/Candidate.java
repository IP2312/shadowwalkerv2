package org.example.shadowwalkerv2.model;

import java.util.List;

public class Candidate {

    public final double cost;
    public final List<Long> path;

   public Candidate(double cost, List<Long> path) {
        this.cost = cost;
        this.path = path;
    }
}


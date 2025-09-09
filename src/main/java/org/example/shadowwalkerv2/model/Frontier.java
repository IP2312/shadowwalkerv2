package org.example.shadowwalkerv2.model;


import org.springframework.stereotype.Component;



import java.util.Comparator;
import java.util.PriorityQueue;

@Component
public class Frontier {
    private static final Comparator<RouteNode> AStar =
            Comparator.comparingDouble(RouteNode::getFCost)
                    .thenComparingDouble(n -> -n.shadeRatio())
                    .thenComparingDouble(n -> -n.getCostToReachNode());

    private final PriorityQueue<RouteNode> pq = new PriorityQueue<>(AStar);
    // Tracks the current "live" node for each id
    private final java.util.Map<Long, RouteNode> inOpen = new java.util.HashMap<>();

    public void clear() {
        pq.clear();
    }

    public boolean isEmpty() {
        return pq.isEmpty();
    }

    public void addOrUpdateNode(RouteNode n) {
        if (n == null) return;
        pq.add(n);
    }

    public RouteNode removeNode() {
        // remove one
        return pq.poll();

    }
}


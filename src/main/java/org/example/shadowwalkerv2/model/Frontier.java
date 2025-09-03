package org.example.shadowwalkerv2.model;


import org.springframework.stereotype.Component;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
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
        inOpen.clear();
    }

    public boolean isEmpty() {
        return pq.isEmpty();
    }

    public void addOrUpdateNode(RouteNode n) {
        if (n == null) return;
        // Overwrite the live entry and push; we do NOT remove the old one from pq.
        // Old entries become "stale" and will be skipped when popped.
        inOpen.put(n.getId(), n);
        pq.add(n);
    }

    public RouteNode removeNode() {
        while (!pq.isEmpty()) {
            RouteNode top = pq.poll(); // remove one
            RouteNode live = inOpen.get(top.getId());
            if (live == top) {
                // This is the current best instance—make it "closed"
                inOpen.remove(top.getId());
                return top;
            }
            // else stale entry—skip and continue
        }
        return null;
    }
}


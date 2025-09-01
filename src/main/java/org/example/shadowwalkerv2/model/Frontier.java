package org.example.shadowwalkerv2.model;


import org.springframework.stereotype.Component;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;

@Component
public class Frontier {
    //private final ArrayList<RouteNode> frontier = new ArrayList<>();


    private static final Comparator<RouteNode> AStar =
            Comparator.comparingDouble(RouteNode::getFCost)
                    .thenComparingDouble(n -> -n.shadeRatio())
                    .thenComparingDouble(n -> -n.getCostToReachNode());


    private final PriorityQueue<RouteNode> pg = new PriorityQueue<>(AStar);
    private final HashSet<RouteNode> inOpen = new HashSet<>();

    public void clear(){
        pg.clear();
        inOpen.clear();
    }
    public boolean isEmpty(){
        return pg.isEmpty();
    }

    public void addOrUpdateNode(RouteNode node){
        if (node == null || node.isExplored()) return;


        if (inOpen.contains(node)){
            pg.remove(node);
        }else{
            inOpen.add(node);
        }

        pg.add(node);
    }


    public RouteNode removeNode(){
        RouteNode nextNode = pg.poll();

        if (nextNode != null){
            pg.remove(nextNode);
            //nextNode.setExplored(true); 
        }
        return nextNode;
    }

    /*public RouteNode removeNode(){
        double cost = Double.MAX_VALUE;
        RouteNode nextNode = null;
        int indexToRemove = 0;
        for (int i = 0; i < frontier.size(); i++) {
            if (cost > frontier.get(i).getFCost()) {
                cost = frontier.get(i).getFCost();
                indexToRemove = i;
            }
        }
        nextNode = frontier.get(indexToRemove);
        nextNode.setExplored(true);
        frontier.remove(indexToRemove);
        return nextNode;

    }*/

}

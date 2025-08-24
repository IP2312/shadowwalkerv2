package org.example.shadowwalkerv2.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Component
public class Frontier {
    private final ArrayList<RouteNode> frontier = new ArrayList<>();


    public void clear(){
        frontier.clear();
    }

    public void addNode(RouteNode node){
        if (!frontier.contains(node) && !node.isExplored())
        frontier.add(node);
    }


    //Todo PriorityList
    public RouteNode removeNode(){
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

    }

}

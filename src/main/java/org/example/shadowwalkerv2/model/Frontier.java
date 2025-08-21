package org.example.shadowwalkerv2.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Component
public class Frontier {
    private final ArrayList<RouteNode> frontier = new ArrayList<>();


    public void addNode(RouteNode node){
        frontier.add(node);
    }

    public void removeNode(){


    }

}

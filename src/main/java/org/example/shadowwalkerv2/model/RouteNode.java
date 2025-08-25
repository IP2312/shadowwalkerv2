package org.example.shadowwalkerv2.model;


import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Data

@NoArgsConstructor
@Component
public class RouteNode {
    private long id;
    private GeoCoordinate coordinate;

    private double costToReachNode;
    private double estimatedCostToGoal;
    private RouteNode parentNode;
    private boolean explored;

    public RouteNode(long id, GeoCoordinate coordinate) {
        this.id = id;
        this.coordinate = coordinate;
    }

    public double getFCost(){
        return estimatedCostToGoal + costToReachNode;
    }
}

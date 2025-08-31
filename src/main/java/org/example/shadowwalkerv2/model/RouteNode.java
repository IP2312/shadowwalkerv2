package org.example.shadowwalkerv2.model;


import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Data

@NoArgsConstructor
@Component
@Setter
@Getter
public class RouteNode {
    private long id;
    private GeoCoordinate coordinate;

    private double costToReachNode;
    private double estimatedCostToGoal;
    private RouteNode parentNode;
    private boolean explored;
    private int shadedCount = 0;
    private int totalCount = 0;

    public RouteNode(long id, GeoCoordinate coordinate) {
        this.id = id;
        this.coordinate = coordinate;
    }

    public double getFCost(){
        return estimatedCostToGoal + costToReachNode;
    }

    public double shadeRatio(){
        return totalCount == 0 ? 0.0 : (double) shadedCount/totalCount;
    }
}

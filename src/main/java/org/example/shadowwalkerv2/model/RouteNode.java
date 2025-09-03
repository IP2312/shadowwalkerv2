package org.example.shadowwalkerv2.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RouteNode {

    @EqualsAndHashCode.Include
    private long id;

    private GeoCoordinate coordinate;

    private double costToReachNode;
    private double estimatedCostToGoal;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private RouteNode parentNode;

    private boolean explored;

    private int shadedCount = 0;
    private int totalCount  = 0;

    public RouteNode(long id, GeoCoordinate coordinate) {
        this.id = id;
        this.coordinate = coordinate;
    }

    public double getFCost() {
        return estimatedCostToGoal + costToReachNode;
    }

    public double shadeRatio() {
        return totalCount == 0 ? 0.0 : (double) shadedCount / totalCount;
    }
}

package org.example.shadowwalkerv2.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Component
public class RouteNode {
    private long id;
    private GeoCoordinate coordinate;

    private double costToReachNode;
    private double estimatedCostToGaol;
    private RouteNode parentNode;
    private RouteNode childNode;
    private boolean explored;
}

package org.example.shadowwalkerv2.model;


import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class BuildingNode {
    private long id;
    private GeoCoordinate coordinate;
}

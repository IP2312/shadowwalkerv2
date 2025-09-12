package org.example.shadowwalkerv2.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.locationtech.jts.geom.Polygon;

@AllArgsConstructor
@Data
public class BuildingObject {
    private long id;
    private Polygon polygon;
    private double height;

}

package org.example.shadowwalkerv2.service;

import org.example.shadowwalkerv2.model.BuildingNode;
import org.example.shadowwalkerv2.model.BuildingWay;
import org.example.shadowwalkerv2.model.GeoCoordinate;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import org.locationtech.jts.geom.*;
import org.springframework.stereotype.Service;

@Service
public class GeometryService {

    private final MapService mapService;

    public GeometryService(MapService mapService) {
        this.mapService = mapService;
    }

    public boolean intersection(GeoCoordinate start, GeoCoordinate end, BuildingWay building, LinkedHashSet<BuildingNode> nodes, ZonedDateTime time, double azimuth, double elevation) {

        GeometryFactory gf = new GeometryFactory();

        // Index building nodes by id for fast lookup
        HashMap<Long, BuildingNode> nodeIndex = new HashMap<>(nodes.size());
        for (BuildingNode n : nodes) {
            long id = n.getId();
            nodeIndex.put(id, n);
        }

        List<Long> ids = building.getNodesId();
        if (ids == null || ids.size() < 3) {
            System.out.println("Not enough vertices to form a polygon");
            return false;
        }

        // Ensure ring is closed (first id == last id)
        boolean closed = ids.size() >= 4 && ids.get(0).equals(ids.get(ids.size() - 1));
        List<Long> ringIds = closed ? ids : new ArrayList<>(ids);
        if (!closed) {
            ringIds.add(ids.get(0));
        }

        // Build polygon coordinates (JTS expects x=lon, y=lat)
        Coordinate[] poly = new Coordinate[ringIds.size()];
        for (int i = 0; i < ringIds.size(); i++) {
            Long nid = ringIds.get(i);
            BuildingNode bn = nodeIndex.get(nid);
            if (bn == null || bn.getCoordinate() == null) {
                System.out.println("Missing node for id: " + nid);
                return false;
            }
            double lat = bn.getCoordinate().getLat();
            double lon = bn.getCoordinate().getLon();
            poly[i] = new Coordinate(lon, lat);
        }

        Polygon polygon;
        try {
            polygon = gf.createPolygon(poly);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid polygon geometry: " + e.getMessage());
            return false;
        }

        // Build line (lon, lat)
        Coordinate[] lineCoords = new Coordinate[] {
                new Coordinate(start.getLon(), start.getLat()),
                new Coordinate(end.getLon(), end.getLat())
        };
        LineString line = gf.createLineString(lineCoords);

        boolean intersects = polygon.intersects(line);
        Geometry intersection = intersects ? polygon.intersection(line) : null;

       /* System.out.println("Intersects? " + intersects);
        System.out.println("Intersection geometry: " + (intersection != null ? intersection : "—"));
        System.out.println("Distance: ");*/


        //Todo handle no intersection
        double heightSun = 0;
        double buildingHeight = 0;
        if (intersects){
            double distance = mapService.haversineDistance(start, new GeoCoordinate(intersection.getCoordinate().y, intersection.getCoordinate().x));
             heightSun = calculateHeightIncrease(distance, elevation);
             buildingHeight = getBuildingHeight(building);

            //System.out.println("Sun: " + heightSun);
            //System.out.println("Building: " + buildingHeight);
        }


        //System.out.println("In the Shadow!!");
        return buildingHeight > heightSun;
    }


    public double getBuildingHeight(BuildingWay building){
        double height;
        if (building.getHeight() <=0) {
            return building.getLevels() * 3.5;
        }
        return building.getHeight();
    }

    public double calculateHeightIncrease(double distanceMeters, double angleDegrees) {
        // Convert degrees to radians for Math.tan
        double angleRadians = Math.toRadians(angleDegrees);
        return distanceMeters * Math.tan(angleRadians);
    }

}

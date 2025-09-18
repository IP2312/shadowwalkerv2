package org.example.shadowwalkerv2.service;

import org.example.shadowwalkerv2.dto.OverpassElement;
import org.example.shadowwalkerv2.model.*;
import org.locationtech.jts.geom.Polygon;
import org.shredzone.commons.suncalc.SunPosition;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import org.locationtech.jts.geom.*;

@Service
public class SunService {
    private final GeometryService geometryService;
    private final OverpassService overpassService;

    public SunService(GeometryService geometryService, OverpassService overpassService) {
        this.geometryService = geometryService;
        this.overpassService = overpassService;
    }


    public List<Path> calculateShadeForRouts(ArrayList<Path> paths, ZonedDateTime time, GeoCoordinate start, GeoCoordinate goal) {
        System.out.println(time);
        List<Path> selectedPaths = selectPaths(paths);
        OverpassResponse buildingElements = overpassService.loadBuildings(start, goal);
        LinkedHashSet<BuildingNode> buildingNodes = new LinkedHashSet<>();
        ArrayList<BuildingWay> buildings = new ArrayList<>();
        for (OverpassElement element : buildingElements.getElements()) {
            if ("way".equals(element.type)) {
                BuildingWay newBuilding = new BuildingWay(element.id, "building", new ArrayList<>(element.nodes)); // FIX
                if (element.tags != null) {
                    newBuilding.setHeight(element.tags.get("height"));
                    newBuilding.setLevels(element.tags.get("building:levels"));
                }
                buildings.add(newBuilding);
            } else if ("node".equals(element.type)) {
                buildingNodes.add(new BuildingNode(element.id, new GeoCoordinate(element.lat, element.lon)));
            }
        }

        if (buildingNodes.isEmpty()) {
            System.out.println("No BuildingNodes");
            return selectedPaths;
        }
        System.out.println("buildings loaded");


        // Shade cache for this run
        RouteNode startNode = paths.get(0).getNodes().iterator().next();
//    GeoCoordinate rayEnd = calculateLineForSunray(startNode, time);
//    GeoCoordinate rayStart = startNode.getCoordinate();
        double azimuth = getAzimuth(startNode.getCoordinate().getLat(), startNode.getCoordinate().getLon(), time);
        double elevation = getElevation(startNode.getCoordinate().getLat(), startNode.getCoordinate().getLon(), time);

        HashSet<BuildingObject> buildingObjects = new HashSet<>();
        for (BuildingWay building : buildings){
            Polygon polygon = geometryService.buildPolygon(building, buildingNodes);
            double height = geometryService.getBuildingHeight(building);
            BuildingObject buildingObject = new BuildingObject(building.getId(),polygon,height);
            buildingObjects.add(buildingObject);
        }
        System.out.println(buildingObjects.size());
        Map<Long, Boolean> shadedCache = new HashMap<>();
        Function<RouteNode, Boolean> isShaded = rn ->
                shadedCache.computeIfAbsent(
                        rn.getId(),
                        id -> checkForShade(rn, buildingObjects, azimuth, elevation, time)
                );


        for (Path path : paths) {
            int shadedNodeNr = 0;
            for (RouteNode node : path.getNodes()) {
                if (isShaded.apply(node)) shadedNodeNr++;
            }
            double shadePct = (path.isEmpty() ? 0.0 : 100.0 * shadedNodeNr / path.getNrNodes());
            path.setShadePct(shadePct);

        }

        selectedPaths = selectPaths(paths);
        return selectedPaths;
    }


    public boolean checkForShade(RouteNode currentNode, HashSet<BuildingObject> buildings, double azimuth, double elevation, ZonedDateTime time) {


        GeoCoordinate rayStart = currentNode.getCoordinate();
        GeoCoordinate rayEnd = calculateLineForSunray(currentNode, time, azimuth);
//todo return if true
        for (BuildingObject building : buildings) {
            if (geometryService.intersection(rayStart, rayEnd, building, elevation)) {
                return true;
            }
        }
        return false;
    }

    public GeoCoordinate calculateLineForSunray(RouteNode node, ZonedDateTime time, double azimuth) {
        double lat = node.getCoordinate().getLat();
        double lon = node.getCoordinate().getLon();
        //System.out.println("Azimuth: " + azimuth);
        //System.out.println("Elevation" + getElevation(lat,lon,time));
        double distanceMeters = 200;
        double R = 6371000.0; // Earth radius in meters
        double bearing = Math.toRadians(azimuth);

        double lat1 = Math.toRadians(lat);
        double lon1 = Math.toRadians(lon);

        double lat2 = Math.asin(
                Math.sin(lat1) * Math.cos(distanceMeters / R) +
                        Math.cos(lat1) * Math.sin(distanceMeters / R) * Math.cos(bearing)
        );

        double lon2 = lon1 + Math.atan2(
                Math.sin(bearing) * Math.sin(distanceMeters / R) * Math.cos(lat1),
                Math.cos(distanceMeters / R) - Math.sin(lat1) * Math.sin(lat2)
        );

        return new GeoCoordinate(Math.toDegrees(lat2), Math.toDegrees(lon2));
    }

    public double getAzimuth(double lat, double lon, ZonedDateTime time) {
        SunPosition position = SunPosition.compute()
                .at(lat, lon)
                .on(time)
                .execute();
        System.out.println("Azimuth");
        return position.getAzimuth();
    }

    public double getElevation(double lat, double lon, ZonedDateTime time) {
        SunPosition position = SunPosition.compute()
                .at(lat, lon)
                .on(time)
                .execute();
        System.out.println("Elevation");
        return position.getAltitude();
    }

    public ArrayList<Path> selectPaths(ArrayList<Path> paths) {
        ArrayList<Path> selectedPaths = new ArrayList<>();
        int deltaS = 5;
        double minShade = paths.get(0).getShadePct();
        selectedPaths.add(paths.get(0));
        System.out.println("ShortestPath: " + paths.get(0).getId() + " " +
                "shade: " + paths.get(0).getShadePct() + "%");
        for (Path path : paths) {
            if (path.getShadePct() > minShade) {

                selectedPaths.add(path);
                minShade = path.getShadePct() + deltaS;
                System.out.println("Path: " + path.getId() + " shade: " + path.getShadePct() + "%");
            }

        }
        System.out.println("Selected Paths: " + selectedPaths.size());
        return selectedPaths;
    }
}

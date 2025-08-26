package org.example.shadowwalkerv2.service;

import org.example.shadowwalkerv2.model.BuildingWay;
import org.example.shadowwalkerv2.model.GeoCoordinate;
import org.example.shadowwalkerv2.model.RouteNode;
import org.shredzone.commons.suncalc.SunPosition;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.ArrayList;

@Service
public class SunService {

    public void checkForShade(RouteNode currentNode, ArrayList<BuildingWay> buildings,ZonedDateTime time) {
        GeoCoordinate RayEnd = calculateLineForSunray(currentNode, time);
        GeoCoordinate RayStart = currentNode.getCoordinate();
        for (BuildingWay buildingWay : buildings) {
            intersection.intersection(RayStart, RayEnd, buildingWay, buildingNodes);

        }
    }

    public GeoCoordinate calculateLineForSunray(RouteNode node, ZonedDateTime time){
        double lat = node.getCoordinate().getLat();
        double lon = node.getCoordinate().getLon();
        double azimuth = getAzimuth(lat,lon, time);
        System.out.println("Azimuth: " + azimuth);
        System.out.println("Elevation" + getElevation(lat,lon,time));
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

    public double getAzimuth(double lat, double lon, ZonedDateTime time){
        SunPosition position = SunPosition.compute()
                .at(lat, lon)
                .on(time)
                .execute();

        return position.getAzimuth();
    }

    public double getElevation(double lat, double lon, ZonedDateTime time){
        SunPosition position = SunPosition.compute()
                .at(lat, lon)
                .on(time)
                .execute();

        return position.getAltitude();
    }
}

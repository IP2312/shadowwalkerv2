package org.example.shadowwalkerv2.service;

import org.example.shadowwalkerv2.model.GeoCoordinate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class MapService {

    public HashMap<String,Double> calculateBorders(GeoCoordinate start, GeoCoordinate goal){
        //todo   add function to calculate minimum box if very small
        double minLat = Math.min(start.getLat(), goal.getLat());
        double maxLat = Math.max(start.getLat(), goal.getLat());
        double minLon = Math.min(start.getLon(), goal.getLon());
        double maxLon = Math.max(start.getLon(), goal.getLon());

        double marginMeters = haversineDistance(start, goal) * 0.5;

        //  Convert meters → degrees
        double midLat = (minLat + maxLat) / 2.0; // use mean latitude for lon scaling
        double marginLatDeg = metersToLatDegrees(marginMeters);
        double marginLonDeg = metersToLonDegrees(marginMeters, midLat);

        double sBorder = minLat - marginLatDeg;
        double nBorder = maxLat + marginLatDeg;
        double wBorder = minLon - marginLonDeg;
        double eBorder = maxLon + marginLonDeg;

        HashMap<String, Double> borders = new HashMap<>();
        borders.put("sBorder", sBorder);
        borders.put("nBorder", nBorder);
        borders.put("wBorder", wBorder);
        borders.put("eBorder", eBorder);
        return borders;

    }
    private static double metersToLatDegrees(double meters) {
        // ~111,320 m per degree of latitude
        return meters / 111_320.0;
    }

    private static double metersToLonDegrees(double meters, double atLatDeg) {
        // ~111,320 * cos(latitude) m per degree of longitude
        double metersPerDeg = 111_320.0 * Math.cos(Math.toRadians(atLatDeg));
        // guard against poles
        if (metersPerDeg < 1e-6) metersPerDeg = 1e-6;
        return meters / metersPerDeg;
    }

    public double haversineDistance(GeoCoordinate coordinate1, GeoCoordinate coordinate2) {

        double lat1 = coordinate1.getLat();
        double lon1 = coordinate1.getLon();

        double lat2 = coordinate2.getLat();
        double lon2 = coordinate2.getLon();


        final int R = 6371000; // Radius of the earth in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // in meters
    }

}

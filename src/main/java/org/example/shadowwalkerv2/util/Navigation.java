package org.example.shadowwalkerv2.util;


import org.example.shadowwalkerv2.model.GeoCoordinate;
import org.example.shadowwalkerv2.model.RouteNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class Navigation {
    public ArrayList<GeoCoordinate> findeRoute(GeoCoordinate start, GeoCoordinate goal){
        ArrayList<GeoCoordinate> routeCoordinates = new ArrayList<>();

        routeCoordinates.add(start);
        routeCoordinates.add(new GeoCoordinate(48.310661362874455, 14.291979911708491));
        routeCoordinates.add(new GeoCoordinate(48.3111889542115, 14.29258036938026));
        routeCoordinates.add(goal);


        return routeCoordinates;
    }

    public void loadRouteNodes(){

    }


}

package org.example.shadowwalkerv2.service;


import org.example.shadowwalkerv2.model.GeoCoordinate;
import org.example.shadowwalkerv2.model.OverpassResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class Navigation {
    private final OverpassService overpassService;

    public Navigation() {
        this.overpassService = new OverpassService();
    }

    public ArrayList<GeoCoordinate> findeRoute(GeoCoordinate start, GeoCoordinate goal){
        ArrayList<GeoCoordinate> routeCoordinates = new ArrayList<>();
        OverpassResponse routElements = overpassService.loadRouts(start, goal);

        routeCoordinates.add(start);
        routeCoordinates.add(new GeoCoordinate(48.310661362874455, 14.291979911708491));
        routeCoordinates.add(new GeoCoordinate(48.3111889542115, 14.29258036938026));
        routeCoordinates.add(goal);


        return routeCoordinates;
    }

    public void loadRouteNodes(){

    }


}

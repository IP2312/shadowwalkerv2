package org.example.shadowwalkerv2.service;


import org.example.shadowwalkerv2.dto.OverpassElement;
import org.example.shadowwalkerv2.model.*;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;

@Service
public class Navigation {
    private final OverpassService overpassService;
    private final MapService mapService;
    private final Frontier frontier;
    private final SunService sunService;

    public Navigation(SunService sunService) {
        this.sunService = sunService;
        this.overpassService = new OverpassService();
        this.mapService = new MapService();
        this.frontier = new Frontier();

    }

    public ArrayList<ArrayList<GeoCoordinate>> findeRoutes(GeoCoordinate start, GeoCoordinate goal) {
        System.out.println("Start:");
        ZonedDateTime time = ZonedDateTime.now().minusHours(1);

        ArrayList<ArrayList<GeoCoordinate>> routes = new ArrayList<>();

        // --- Suggested waypoints in Vienna 1st district (lat, lon) ---
        GeoCoordinate GRABEN_E        = new GeoCoordinate(48.208900, 16.371700);
        GeoCoordinate KOHLMARKT       = new GeoCoordinate(48.207600, 16.368900);
        GeoCoordinate MICHAELERPLATZ  = new GeoCoordinate(48.206520, 16.365680); // same as typical 'goal'
        GeoCoordinate KAERNTNER_STR   = new GeoCoordinate(48.205900, 16.372500);
        GeoCoordinate ALBERTINA       = new GeoCoordinate(48.203600, 16.368900);
        GeoCoordinate AUGUSTINERKIRCHE= new GeoCoordinate(48.205500, 16.370200);
        GeoCoordinate PETERSKIRCHE    = new GeoCoordinate(48.208500, 16.372500);
        GeoCoordinate AM_HOF          = new GeoCoordinate(48.210000, 16.369200);
        GeoCoordinate MINORITENPLATZ  = new GeoCoordinate(48.209300, 16.366100);
        GeoCoordinate TUCHLAUBEN      = new GeoCoordinate(48.210300, 16.371000);
        GeoCoordinate FREYUNG         = new GeoCoordinate(48.212000, 16.366700);
        GeoCoordinate CAFE_CENTRAL    = new GeoCoordinate(48.210600, 16.365800);
        GeoCoordinate ROTENTURMSTR    = new GeoCoordinate(48.208900, 16.377900);
        GeoCoordinate HOHER_MARKT     = new GeoCoordinate(48.212100, 16.373200);
        GeoCoordinate JUDENPLATZ      = new GeoCoordinate(48.211100, 16.370000);
        GeoCoordinate NEUER_MARKT     = new GeoCoordinate(48.205000, 16.372100);
        GeoCoordinate ALBERTINAPLATZ  = new GeoCoordinate(48.203300, 16.368600);
        GeoCoordinate BURGGARTEN      = new GeoCoordinate(48.203900, 16.366100);
        GeoCoordinate HELDENPLATZ     = new GeoCoordinate(48.206900, 16.363400);
        GeoCoordinate SINGERSTR       = new GeoCoordinate(48.207200, 16.375000);
        GeoCoordinate FRANZISKANERPL  = new GeoCoordinate(48.206600, 16.372900);
        GeoCoordinate AUGUSTINERSTR   = new GeoCoordinate(48.205800, 16.370900);

        // Route 1: start → Graben → Kohlmarkt → goal
        routes.add(new ArrayList<>(Arrays.asList(
                start, GRABEN_E, KOHLMARKT, goal
        )));

        // Route 2: start → Kärntner Straße → Albertina → Augustinerkirche → goal
        routes.add(new ArrayList<>(Arrays.asList(
                start, KAERNTNER_STR, ALBERTINA, AUGUSTINERKIRCHE, goal
        )));

        // Route 3: start → Peterskirche → Am Hof → Minoritenplatz → goal
        routes.add(new ArrayList<>(Arrays.asList(
                start, PETERSKIRCHE, AM_HOF, MINORITENPLATZ, goal
        )));

        // Route 4: start → Tuchlauben → Freyung → Café Central → goal
        routes.add(new ArrayList<>(Arrays.asList(
                start, TUCHLAUBEN, FREYUNG, CAFE_CENTRAL, goal
        )));

        // Route 5: start → Rotenturmstraße → Hoher Markt → Judenplatz → Kohlmarkt → goal
        routes.add(new ArrayList<>(Arrays.asList(
                start, ROTENTURMSTR, HOHER_MARKT, JUDENPLATZ, KOHLMARKT, goal
        )));

        // Route 6: start → Neuer Markt → Albertinaplatz → Burggarten → Heldenplatz → goal
        routes.add(new ArrayList<>(Arrays.asList(
                start, NEUER_MARKT, ALBERTINAPLATZ, BURGGARTEN, HELDENPLATZ, goal
        )));

        // Route 7: start → Singerstraße → Franziskanerplatz → Augustinerstraße → goal
        routes.add(new ArrayList<>(Arrays.asList(
                start, SINGERSTR, FRANZISKANERPL, AUGUSTINERSTR, goal
        )));

        return routes;
    }
}

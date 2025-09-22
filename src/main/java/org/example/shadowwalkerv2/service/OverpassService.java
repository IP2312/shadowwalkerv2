package org.example.shadowwalkerv2.service;

import org.example.shadowwalkerv2.dto.OverpassElement;
import org.example.shadowwalkerv2.model.GeoCoordinate;
import org.example.shadowwalkerv2.model.OverpassResponse;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

@Service
public class OverpassService {
    private final RestTemplate restTemplate;
    private final MapService mapService;

    public OverpassService() {
        this.mapService = new MapService();
        this.restTemplate = new RestTemplate();
    }


    public OverpassResponse loadRouts(GeoCoordinate start, GeoCoordinate goal) {
        HashMap<String, Double> borders = mapService.calculateBorders(start, goal);
        String bbox = String.format(
                Locale.US, "%.8f, %.8f, %.8f, %.8f",
                borders.get("sBorder"), borders.get("wBorder"),
                borders.get("nBorder"), borders.get("eBorder")
        );

        String query = """
                [out:json][timeout:25];
                // Union of walkable ways inside the bbox
                (
                  // A) Core pedestrian ways
                  way(%s)
                    ["highway"]["highway"~"footway|path|pedestrian|living_street|steps"]
                    ["foot"!~"no|private"];
                
                  // B) Sidewalks & crossings mapped as footway
                  way(%s)
                    ["highway"="footway"]["footway"~"sidewalk|crossing"];
                
                  // C) Cycleways that also permit walking
                  way(%s)
                    ["highway"="cycleway"]["foot"~"designated|yes|permissive|official"];
                
                  // D) Platforms (often pedestrian surfaces)
                  way(%s)["railway"="platform"];
                  way(%s)["public_transport"="platform"];
                
                  // E) Non-motorways explicitly allowing foot
                  way(%s)
                    ["highway"]["highway"!~"motorway|trunk|motorway_link|trunk_link"]
                    ["foot"~"designated|yes|permissive|official"];
                
                  // F) Streets with sidewalks
                  way(%s)
                    ["highway"]["highway"!~"motorway|trunk|motorway_link|trunk_link"]
                    ["sidewalk"~"both|left|right|separate|yes"];
                );
                out body;
                >;
                out skel qt;
                """;

        query = String.format(Locale.US, query, bbox, bbox, bbox, bbox, bbox, bbox, bbox);

        return sendQuery(query);

    }

    public OverpassResponse loadBuildings(GeoCoordinate start, GeoCoordinate goal) {
        HashMap<String, Double> borders = mapService.calculateBorders(start, goal);
        //todo add relational buildings
        String query = String.format(Locale.US, """
                [out:json][timeout:25];
                (
                    way(%.8f, %.8f, %.8f, %.8f) ["building"]
                    (if: t["height"] || t["building:levels"]);
                );
                out body;
                >;
                out skel qt;
                """, borders.get("sBorder"), borders.get("wBorder"), borders.get("nBorder"), borders.get("eBorder"));
        return sendQuery(query);

    }


    private OverpassResponse sendQuery(String query) {
        String url = "https://overpass-api.de/api/interpreter";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);

        HttpEntity<String> request = new HttpEntity<>(query, headers);

        ResponseEntity<OverpassResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                OverpassResponse.class
        );

        return response.getBody();
    }


}

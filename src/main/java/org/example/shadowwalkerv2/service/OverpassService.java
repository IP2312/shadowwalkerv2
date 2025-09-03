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
        this.restTemplate =  new RestTemplate();
    }




    public OverpassResponse loadRouts(GeoCoordinate start, GeoCoordinate goal){
        HashMap<String,Double> borders = mapService.calculateBorders(start, goal);
        //todo sidewalks attached to streets
        String query = String.format(Locale.US, """
            [out:json][timeout:25];
            // Bounding Box: [South, West, North, East]
            (
              way(%.8f, %.8f, %.8f, %.8f)
                ["highway"]["highway"~"footway|pedestrian|path|living_street"]
                ["foot"!~"no|private"];
            );
            out body;
            >;
            out skel qt;
            """, borders.get("sBorder"), borders.get("wBorder"), borders.get("nBorder"), borders.get("eBorder"));
        return sendQuery(query);
    }
    public OverpassResponse loadBuildings(GeoCoordinate start, GeoCoordinate goal) {
        HashMap<String,Double> borders = mapService.calculateBorders(start, goal);
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

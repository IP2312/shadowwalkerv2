package org.example.shadowwalkerv2.service;

import org.example.shadowwalkerv2.dto.OverpassElement;
import org.example.shadowwalkerv2.model.GeoCoordinate;
import org.example.shadowwalkerv2.model.OverpassResponse;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;

@Service
public class OverpassService {
    private final RestTemplate restTemplate;

    public OverpassService() {
        this.restTemplate =  new RestTemplate();
    }
    public OverpassResponse loadRouts(GeoCoordinate start, GeoCoordinate goal){
        return null;
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

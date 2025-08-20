package org.example.shadowwalkerv2.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OverpassService {
    private final RestTemplate restTemplate;

    public OverpassService() {
        this.restTemplate =  new RestTemplate();
    }




}

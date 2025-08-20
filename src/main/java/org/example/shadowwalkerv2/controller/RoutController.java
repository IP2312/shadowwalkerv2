package org.example.shadowwalkerv2.controller;

import org.example.shadowwalkerv2.dto.CoordinateDTO;
import org.example.shadowwalkerv2.model.GeoCoordinate;
import org.example.shadowwalkerv2.util.Navigation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/api")
public class RoutController {
    private final Navigation navigation;

    public RoutController(Navigation navigation) {
        this.navigation = navigation;
    }

    @GetMapping("/test")
    public String test(){
        return "test";
    }

    @GetMapping("/nodes")
    public List<CoordinateDTO> getNodes(
            @RequestParam double startLat,
            @RequestParam double startLon,
            @RequestParam double endLat,
            @RequestParam double endLon) {

        GeoCoordinate start = new GeoCoordinate(startLat, startLon);
        GeoCoordinate end   = new GeoCoordinate(endLat, endLon);

        System.out.println("Api");
        System.out.println(start);
        System.out.println(end);

        return navigation.findeRoute(start, end)
                .stream()
                .map(gc -> new CoordinateDTO(gc.getLat(), gc.getLon()))
                .toList();
    }

}

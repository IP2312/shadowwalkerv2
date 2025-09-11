package org.example.shadowwalkerv2.controller;

import org.example.shadowwalkerv2.dto.CoordinateDTO;
import org.example.shadowwalkerv2.model.GeoCoordinate;
import org.example.shadowwalkerv2.model.Path;
import org.example.shadowwalkerv2.model.RouteDTO;
import org.example.shadowwalkerv2.service.Navigation;
import org.example.shadowwalkerv2.service.SunService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping("/api")
public class RoutController {
    private final Navigation navigation;
    private final SunService sunService;

    public RoutController(Navigation navigation, SunService sunService) {
        this.navigation = navigation;
        this.sunService = sunService;
    }

//    @GetMapping("/test")
//    public String test() {
//        return "test";
//    }

    @GetMapping("/nodes") // consider renaming to /routes
    public List<RouteDTO> getNodes(
            @RequestParam double startLat,
            @RequestParam double startLon,
            @RequestParam double endLat,
            @RequestParam double endLon) {

        GeoCoordinate start = new GeoCoordinate(startLat, startLon);
        GeoCoordinate end   = new GeoCoordinate(endLat, endLon);

        // K shortest paths
        List<Path> paths = navigation.findeKRouts(start, end, 500);

        // compute/set shadePct inside each Path (your method can mutate Path.shadePct)
        List<Path> selected = sunService.calculateShadeForRouts(
                new ArrayList<>(paths), ZonedDateTime.now(), start, end);

        // map to DTOs
        return selected.stream()
                .map(this::toRouteDTO)
                .toList();
    }

    private RouteDTO toRouteDTO(Path p) {
        // Path.nodes is a LinkedHashSet; iterate in insertion order and project to coords
        var coords = p.getNodes().stream()
                .map(rn -> new CoordinateDTO(
                        rn.getCoordinate().getLat(),
                        rn.getCoordinate().getLon()))
                .toList();

        return new RouteDTO(p.getId(), p.getLength(), p.getShadePct(), coords);
    }

}

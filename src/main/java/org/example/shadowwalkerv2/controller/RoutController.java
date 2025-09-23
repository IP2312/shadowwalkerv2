package org.example.shadowwalkerv2.controller;

import org.example.shadowwalkerv2.dto.CoordinateDTO;
import org.example.shadowwalkerv2.model.GeoCoordinate;
import org.example.shadowwalkerv2.model.Path;
import org.example.shadowwalkerv2.model.RouteDTO;
import org.example.shadowwalkerv2.service.Navigation;
import org.example.shadowwalkerv2.service.SunService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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

    @GetMapping("/nodes") // consider renaming to /routes
    public List<RouteDTO> getNodes(
            @RequestParam double startLat,
            @RequestParam double startLon,
            @RequestParam double endLat,
            @RequestParam double endLon,
            @RequestParam(required = false, name = "time") String timeStr, // <-- raw string
            @RequestParam(defaultValue = "100") int k
    ) {
        System.out.println("time param raw = " + timeStr); // should print "06:11"

        LocalTime time = null;
        if (timeStr != null && !timeStr.isBlank()) {
            try {
                time = LocalTime.parse(timeStr.trim(),
                        DateTimeFormatter.ofPattern("HH:mm[:ss]"));
            } catch (DateTimeParseException ex) {
                System.out.println("Invalid time format: " + ex.getMessage());
            }
        }

        ZoneId zone = ZoneId.of("Europe/Vienna");
        ZonedDateTime zdt = (time != null)
                ? LocalDate.now(zone).atTime(time).atZone(zone)
                : ZonedDateTime.now(zone);

        GeoCoordinate start = new GeoCoordinate(startLat, startLon);
        GeoCoordinate end   = new GeoCoordinate(endLat, endLon);

        List<Path> paths = navigation.findeKRouts(start, end, k);
        if (paths.isEmpty()) return List.of();

        List<Path> selected = sunService.calculateShadeForRouts(new ArrayList<>(paths), zdt, start, end);

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

package org.example.shadowwalkerv2.controller;

import org.example.shadowwalkerv2.dto.CoordinateDTO;
import org.example.shadowwalkerv2.model.GeoCoordinate;
import org.example.shadowwalkerv2.model.Path;
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

    @GetMapping("/nodes")
    public List<List<CoordinateDTO>> getNodes(
            @RequestParam double startLat,
            @RequestParam double startLon,
            @RequestParam double endLat,
            @RequestParam double endLon) {

        GeoCoordinate start = new GeoCoordinate(startLat, startLon);
        GeoCoordinate end = new GeoCoordinate(endLat, endLon);



        List<Path> paths = navigation.findeKRouts(start, end, 200);
        List<Path> selectedPaths = sunService.calculateShadeForRouts((ArrayList<Path>) paths, ZonedDateTime.now(),start,end);



        // map Path -> List<CoordinateDTO>
        List<List<CoordinateDTO>> routes = selectedPaths.stream()
                // optional: ensure sorted by cost if you want
                // .sorted(Comparator.comparingDouble(Path::getLength))
                .map(p -> p.getNodes().stream()
                        .map(rn -> {
                            var c = rn.getCoordinate();
                            return new CoordinateDTO(c.getLat(), c.getLon());
                        })
                        .toList()
                )
                .toList();

        return routes;
    }


}

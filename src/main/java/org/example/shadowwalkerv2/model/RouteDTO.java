package org.example.shadowwalkerv2.model;

import org.example.shadowwalkerv2.dto.CoordinateDTO;
import org.locationtech.jts.geom.Coordinate;

import java.util.List;

public record RouteDTO(
        long id,
        double length,        // meters (same as Path.length)
        double shadowPct,     // 0..100, filled by SunService
        List<CoordinateDTO> coords
) {}
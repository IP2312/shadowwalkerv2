package org.example.shadowwalkerv2.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.shadowwalkerv2.model.GeoCoordinate;

@Data
@NoArgsConstructor
public class CoordinateDTO extends GeoCoordinate {

    public CoordinateDTO(double lat, double lon) {
        super(lat, lon);
    }
}

package org.example.shadowwalkerv2.dto;


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.example.shadowwalkerv2.model.GeoCoordinate;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class CoordinateDTO extends GeoCoordinate {

    public CoordinateDTO(double lat, double lon) {
        super(lat, lon);
    }
}

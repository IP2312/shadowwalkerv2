package org.example.shadowwalkerv2.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeoCoordinate {
    private double lat;
    private double lon;
}

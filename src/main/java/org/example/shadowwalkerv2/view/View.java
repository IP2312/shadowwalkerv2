package org.example.shadowwalkerv2.view;

import org.example.shadowwalkerv2.model.GeoCoordinate;
import org.springframework.stereotype.Component;

@Component
public class View {
    private final GeoCoordinate startPoint = new GeoCoordinate(48.3107120116412, 14.292525938461889);
    private final GeoCoordinate destinationPoint = new GeoCoordinate(48.31259894701574, 14.295000192971447);

    public GeoCoordinate getStartPoint(){
        return startPoint;
    }

    public GeoCoordinate getDestinationPoint(){
        return  destinationPoint;
    }
}

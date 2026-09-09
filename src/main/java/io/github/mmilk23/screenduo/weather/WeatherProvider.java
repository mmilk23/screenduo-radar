package io.github.mmilk23.screenduo.weather;

import io.github.mmilk23.screenduo.location.GeoPoint;
import java.io.IOException;

public interface WeatherProvider {

    WeatherConditions currentConditions(GeoPoint location)
            throws IOException, InterruptedException;
}

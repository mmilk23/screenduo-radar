package io.github.mmilk23.screenduo.weather.display;

import io.github.mmilk23.screenduo.weather.WeatherConditions;
import java.util.Objects;

public record WeatherScreenData(String city, WeatherConditions conditions) {

    public WeatherScreenData {
        city = Objects.requireNonNull(city, "city").trim();
        conditions = Objects.requireNonNull(conditions, "conditions");
        if (city.isEmpty()) {
            city = "LOCATION";
        }
    }
}

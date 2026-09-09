package io.github.mmilk23.screenduo.clock.display;

import io.github.mmilk23.screenduo.weather.WeatherConditions;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

public record AirportClockScreenData(String city, WeatherConditions conditions, Instant now, ZoneId zone) {
    public AirportClockScreenData {
        city = Objects.requireNonNull(city, "city").trim();
        conditions = Objects.requireNonNull(conditions, "conditions");
        now = Objects.requireNonNull(now, "now");
        zone = Objects.requireNonNull(zone, "zone");
        if (city.isEmpty()) {
            city = "LOCATION";
        }
    }
}

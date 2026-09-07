package io.github.mmilk23.screenduo.weather;

import java.time.Instant;

public record WeatherConditions(
        Instant observedAt,
        Double temperatureCelsius,
        Double apparentTemperatureCelsius,
        Integer relativeHumidityPercent,
        Double precipitationMillimeters,
        Integer weatherCode,
        Integer cloudCoverPercent,
        Double pressureHectopascals,
        Double windSpeedKilometersPerHour,
        Double windDirectionDegrees) {
}

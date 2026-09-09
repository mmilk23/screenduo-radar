package io.github.mmilk23.screenduo.aircraft.display;

import io.github.mmilk23.screenduo.aircraft.NearbyAircraft;
import java.text.Normalizer;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class AircraftScreenText {

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT).withZone(ZoneOffset.UTC);

    private AircraftScreenText() {
    }

    static String flight(NearbyAircraft aircraft) {
        return aircraft.callsign().isBlank() ? aircraft.icao24() : aircraft.callsign();
    }

    static String airline(NearbyAircraft aircraft) {
        return aircraft.airlineName().isBlank() ? "UNKNOWN OPERATOR" : aircraft.airlineName();
    }

    static String fit(String value, int limit) {
        String text = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toUpperCase(Locale.ROOT);
        return text.length() <= limit ? text : text.substring(0, limit - 3) + "...";
    }

    static String number(Double value, String format) {
        return value == null || !Double.isFinite(value)
                ? "N/A" : String.format(Locale.ROOT, format, value);
    }

    static String bearing(double degrees) {
        if (!Double.isFinite(degrees)) {
            return "N/A";
        }
        double normalized = (degrees % 360.0 + 360.0) % 360.0;
        String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        int index = (int) Math.round(normalized / 45.0) % directions.length;
        return String.format(Locale.ROOT, "%s %03d DEG",
                directions[index], Math.round(normalized) % 360);
    }

    static String snapshot(Instant loadedAt) {
        return "SNAPSHOT " + TIME.format(loadedAt) + " UTC";
    }
}
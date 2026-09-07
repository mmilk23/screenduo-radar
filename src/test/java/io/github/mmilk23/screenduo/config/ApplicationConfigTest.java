package io.github.mmilk23.screenduo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ApplicationConfigTest {

    @Test
    void readsLocationAndUsesDefaultRadius() {
        ApplicationConfig config = ApplicationConfig.fromEnvironment(Map.of(
                "SCREENDUO_LATITUDE", "-22.9068",
                "SCREENDUO_LONGITUDE", "-43.1729"));

        assertEquals(-22.9068, config.location().latitude());
        assertEquals(-43.1729, config.location().longitude());
        assertEquals(50.0, config.aircraftRadiusKm());
    }

    @Test
    void rejectsMissingLocation() {
        assertThrows(IllegalArgumentException.class,
                () -> ApplicationConfig.fromEnvironment(Map.of()));
    }
}

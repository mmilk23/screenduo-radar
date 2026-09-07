package io.github.mmilk23.screenduo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ApplicationConfigTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void readsLocationAndUsesDefaultRadii() throws IOException {
        Path file = writeConfig("""
                [location]
                latitude = -22.9068
                longitude = -43.1729
                """);

        ApplicationConfig config = ApplicationConfig.fromFile(file);

        assertEquals(-22.9068, config.location().latitude());
        assertEquals(-43.1729, config.location().longitude());
        assertEquals(50.0, config.aircraftRadiusKm());
        assertEquals(100.0, config.airportRadiusKm());
    }

    @Test
    void readsConfiguredRadii() throws IOException {
        Path file = writeConfig("""
                [location]
                latitude = -22.9068
                longitude = -43.1729
                aircraft_radius_km = 75
                airport_radius_km = 125
                """);

        ApplicationConfig config = ApplicationConfig.fromFile(file);

        assertEquals(75.0, config.aircraftRadiusKm());
        assertEquals(125.0, config.airportRadiusKm());
    }

    @Test
    void rejectsMissingLocation() throws IOException {
        Path file = writeConfig("[location]\n");

        assertThrows(IllegalArgumentException.class,
                () -> ApplicationConfig.fromFile(file));
    }

    private Path writeConfig(String content) throws IOException {
        return Files.writeString(temporaryDirectory.resolve("config.ini"), content);
    }
}

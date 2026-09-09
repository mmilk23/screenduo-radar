package io.github.mmilk23.screenduo.airline.logo;

import io.github.mmilk23.screenduo.display.DisplayGeometry;
import io.github.mmilk23.screenduo.display.RgbFrame;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class FileSystemAirlineLogoProvider implements AirlineLogoProvider {

    public static final DisplayGeometry LOGO_GEOMETRY = RawRgbLogoStore.LOGO_GEOMETRY;

    private final Path directory;
    private final Map<String, Optional<RgbFrame>> cache = new HashMap<>();

    public FileSystemAirlineLogoProvider() {
        this(Path.of("data", "cache", "airline-logos"));
    }

    public FileSystemAirlineLogoProvider(Path directory) {
        this.directory = directory;
    }

    @Override
    public Optional<RgbFrame> findLogo(String airlineIcaoCode) {
        String normalized = normalize(airlineIcaoCode);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        synchronized (cache) {
            return cache.computeIfAbsent(normalized, this::loadLogo);
        }
    }

    private Optional<RgbFrame> loadLogo(String airlineIcaoCode) {
        Path file = directory.resolve(airlineIcaoCode + ".rgb");
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(RawRgbLogoStore.read(file));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }


    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() != 3) {
            return "";
        }
        for (int index = 0; index < normalized.length(); index++) {
            if (!Character.isLetter(normalized.charAt(index))) {
                return "";
            }
        }
        return normalized;
    }
}

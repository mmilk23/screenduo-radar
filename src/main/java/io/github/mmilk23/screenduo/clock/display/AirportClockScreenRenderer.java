package io.github.mmilk23.screenduo.clock.display;

import io.github.mmilk23.screenduo.display.DisplayGeometry;
import io.github.mmilk23.screenduo.display.FrameCanvas;
import io.github.mmilk23.screenduo.display.FrameScaler;
import io.github.mmilk23.screenduo.display.SevenSegmentText;
import io.github.mmilk23.screenduo.display.RgbColor;
import io.github.mmilk23.screenduo.display.RgbFrame;
import io.github.mmilk23.screenduo.display.ScreenRenderer;
import io.github.mmilk23.screenduo.weather.WeatherConditions;
import java.text.Normalizer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class AirportClockScreenRenderer implements ScreenRenderer<AirportClockScreenData> {

    private static final DisplayGeometry GEOMETRY = new DisplayGeometry(320, 240);
    private static final RgbColor BACKGROUND = new RgbColor(5, 12, 28);
    private static final RgbColor HEADER = new RgbColor(12, 39, 70);
    private static final RgbColor PRIMARY = new RgbColor(238, 246, 255);
    private static final RgbColor SECONDARY = new RgbColor(121, 175, 213);
    private static final RgbColor ACCENT = new RgbColor(53, 200, 255);
    private static final RgbColor AMBER = new RgbColor(222, 151, 55);
    private static final RgbColor SUN = new RgbColor(255, 205, 64);
    private static final RgbColor RAIN = new RgbColor(70, 160, 255);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE dd MMM yyyy", Locale.ROOT);

    @Override
    public RgbFrame render(DisplayGeometry geometry, AirportClockScreenData data) {
        RgbFrame frame = new RgbFrame(GEOMETRY);
        FrameCanvas canvas = new FrameCanvas(frame);
        canvas.clear(BACKGROUND);

        WeatherConditions weather = data.conditions();
        canvas.fillRectangle(0, 0, GEOMETRY.width(), 34, HEADER);
        drawCityTitle(canvas, data.city());
        canvas.drawText("LIVE", GEOMETRY.width() - 31, 13, 1, ACCENT);

        drawTemperature(canvas, weather.temperatureCelsius());
        canvas.drawText(description(weather.weatherCode()), 17, 89, 2, ACCENT);
        drawWeatherIcon(canvas, weather, GEOMETRY.width() - 57, 72);

        drawClock(frame, data.now(), data.zone());
        return GEOMETRY.equals(geometry) ? frame : FrameScaler.fit(frame, geometry, BACKGROUND);
    }

    private static void drawClock(RgbFrame frame, Instant instant, ZoneId zone) {
        String time = TIME.withZone(zone).format(instant);
        int font = SevenSegmentText.largestFittingSize(time, 284, 54, 34);
        int timeWidth = SevenSegmentText.width(time, font);
        SevenSegmentText.draw(frame, time, (GEOMETRY.width() - timeWidth) / 2, 126, font, PRIMARY);

        String date = normalize(DATE.withZone(zone).format(instant));
        FrameCanvas canvas = new FrameCanvas(frame);
        int dateScale = canvas.textWidth(date, 2) <= 286 ? 2 : 1;
        int dateWidth = canvas.textWidth(date, dateScale);
        canvas.drawText(date, (GEOMETRY.width() - dateWidth) / 2, dateScale == 2 ? 193 : 197, dateScale, AMBER);
    }

    private static void drawTemperature(FrameCanvas canvas, Double value) {
        String label = temperature(value);
        canvas.drawText(label, 17, 54, 2, PRIMARY);
        canvas.drawText("TEMP", 17, 73, 1, SECONDARY);
    }

    private static void drawWeatherIcon(FrameCanvas canvas, WeatherConditions weather, int centerX, int centerY) {
        String condition = description(weather.weatherCode());
        if ("CLEAR".equals(condition)) {
            if (Boolean.FALSE.equals(weather.isDay())) {
                drawMoon(canvas, centerX, centerY, weather.moonPhase());
            } else {
                drawSun(canvas, centerX, centerY);
            }
            return;
        }
        if ("STORM".equals(condition)) {
            drawCloud(canvas, centerX, centerY);
            canvas.drawLine(centerX, centerY + 15, centerX - 7, centerY + 29, SUN);
            canvas.drawLine(centerX - 7, centerY + 29, centerX + 3, centerY + 27, SUN);
            return;
        }

        drawCloud(canvas, centerX, centerY);
        if ("RAIN".equals(condition) || "DRIZZLE".equals(condition)) {
            for (int offset = -16; offset <= 16; offset += 16) {
                canvas.drawLine(centerX + offset, centerY + 16, centerX + offset - 4, centerY + 25, RAIN);
            }
        } else if ("SNOW".equals(condition)) {
            for (int offset = -14; offset <= 14; offset += 14) {
                canvas.fillCircle(centerX + offset, centerY + 22, 2, PRIMARY);
            }
        }
    }

    private static void drawSun(FrameCanvas canvas, int centerX, int centerY) {
        canvas.fillCircle(centerX, centerY, 13, SUN);
        for (int[] ray : new int[][] {
            {0, -23, 0, -17}, {0, 17, 0, 23}, {-23, 0, -17, 0}, {17, 0, 23, 0},
            {-17, -17, -13, -13}, {13, 13, 17, 17},
            {13, -13, 17, -17}, {-17, 17, -13, 13}
        }) {
            canvas.drawLine(centerX + ray[0], centerY + ray[1], centerX + ray[2], centerY + ray[3], SUN);
        }
    }

    private static void drawMoon(FrameCanvas canvas, int centerX, int centerY, Double phase) {
        int radius = 17;
        canvas.fillCircle(centerX, centerY, radius, SUN);
        canvas.fillCircle(centerX, centerY, radius - 2, BACKGROUND);

        int phaseIndex = moonPhaseIndex(phase);
        for (int y = -radius + 2; y <= radius - 2; y++) {
            for (int x = -radius + 2; x <= radius - 2; x++) {
                if (x * x + y * y > (radius - 2) * (radius - 2)) {
                    continue;
                }
                if (isMoonPixelLit(phaseIndex, x)) {
                    canvas.fillRectangle(centerX + x, centerY + y, 1, 1, SUN);
                }
            }
        }
    }

    private static int moonPhaseIndex(Double phase) {
        double normalized = phase == null ? 0.5 : Math.max(0.0, Math.min(1.0, phase));
        return (int) Math.floor(normalized * 8.0 + 0.5) % 8;
    }

    private static boolean isMoonPixelLit(int phaseIndex, int x) {
        return switch (phaseIndex) {
            case 0 -> false;
            case 1 -> x >= 8;
            case 2 -> x >= 0;
            case 3 -> x >= -8;
            case 4 -> true;
            case 5 -> x <= 8;
            case 6 -> x <= 0;
            case 7 -> x <= -8;
            default -> true;
        };
    }

    private static void drawCloud(FrameCanvas canvas, int centerX, int centerY) {
        canvas.fillCircle(centerX - 13, centerY + 2, 11, SECONDARY);
        canvas.fillCircle(centerX, centerY - 5, 15, SECONDARY);
        canvas.fillCircle(centerX + 15, centerY + 3, 10, SECONDARY);
        canvas.fillRectangle(centerX - 24, centerY, 48, 13, SECONDARY);
    }

    private static void drawCityTitle(FrameCanvas canvas, String city) {
        String label = normalize(city);
        int maximumWidth = 267;
        int scale = canvas.textWidth(label, 3) <= maximumWidth
                ? 3
                : canvas.textWidth(label, 2) <= maximumWidth ? 2 : 1;

        int maximumCharacters = (maximumWidth + scale) / (6 * scale);
        if (label.length() > maximumCharacters) {
            label = label.substring(0, maximumCharacters - 3) + "...";
        }

        int y = switch (scale) {
            case 3 -> 8;
            case 2 -> 10;
            default -> 13;
        };
        canvas.drawText(label, 12, y, scale, PRIMARY);
    }

    private static String description(Integer code) {
        if (code == null) {
            return "NO DATA";
        }
        if (code == 0) {
            return "CLEAR";
        }
        if (code <= 3) {
            return "CLOUDY";
        }
        if (code == 45 || code == 48) {
            return "FOG";
        }
        if (code >= 51 && code <= 57) {
            return "DRIZZLE";
        }
        if ((code >= 61 && code <= 67) || (code >= 80 && code <= 82)) {
            return "RAIN";
        }
        if ((code >= 71 && code <= 77) || (code >= 85 && code <= 86)) {
            return "SNOW";
        }
        if (code >= 95) {
            return "STORM";
        }
        return "VARIABLE";
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT);
    }

    private static String temperature(Double value) {
        return value == null ? "--.- C" : String.format(Locale.ROOT, "%.1f C", value);
    }
}

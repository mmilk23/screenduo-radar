package io.github.mmilk23.screenduo.weather.display;

import io.github.mmilk23.screenduo.display.DisplayGeometry;
import io.github.mmilk23.screenduo.display.FrameCanvas;
import io.github.mmilk23.screenduo.display.FrameScaler;
import io.github.mmilk23.screenduo.display.RgbColor;
import io.github.mmilk23.screenduo.display.RgbFrame;
import io.github.mmilk23.screenduo.display.ScreenRenderer;
import io.github.mmilk23.screenduo.weather.WeatherConditions;
import java.text.Normalizer;
import java.util.Locale;

public final class WeatherScreenRenderer implements ScreenRenderer<WeatherScreenData> {

    private static final DisplayGeometry DASHBOARD_GEOMETRY = new DisplayGeometry(320, 240);
    private static final RgbColor BACKGROUND = new RgbColor(5, 12, 28);
    private static final RgbColor HEADER = new RgbColor(12, 39, 70);
    private static final RgbColor CARD = new RgbColor(11, 28, 48);
    private static final RgbColor PRIMARY = new RgbColor(238, 246, 255);
    private static final RgbColor SECONDARY = new RgbColor(121, 175, 213);
    private static final RgbColor ACCENT = new RgbColor(53, 200, 255);
    private static final RgbColor SUN = new RgbColor(255, 205, 64);
    private static final RgbColor RAIN = new RgbColor(70, 160, 255);

    @Override
    public RgbFrame render(DisplayGeometry geometry, WeatherScreenData data) {
        RgbFrame dashboard = renderDashboard(data);
        if (DASHBOARD_GEOMETRY.equals(geometry)) {
            return dashboard;
        }
        return FrameScaler.fit(dashboard, geometry, BACKGROUND);
    }

    private static RgbFrame renderDashboard(WeatherScreenData data) {
        WeatherConditions weather = data.conditions();
        RgbFrame frame = new RgbFrame(DASHBOARD_GEOMETRY);
        FrameCanvas canvas = new FrameCanvas(frame);
        canvas.clear(BACKGROUND);
        canvas.fillRectangle(0, 0, DASHBOARD_GEOMETRY.width(), 34, HEADER);
        drawCityTitle(canvas, data.city());
        canvas.drawText("LIVE", DASHBOARD_GEOMETRY.width() - 31, 13, 1, ACCENT);

        canvas.drawText(temperature(weather.temperatureCelsius()), 15, 49, 4, PRIMARY);
        canvas.drawText(description(weather.weatherCode()), 17, 87, 2, ACCENT);
        drawWeatherIcon(canvas, weather.weatherCode(), DASHBOARD_GEOMETRY.width() - 57, 72);

        canvas.fillRectangle(10, 119, DASHBOARD_GEOMETRY.width() - 20, 1, SECONDARY);
        drawMetric(canvas, 14, 135, "FEELS", temperature(weather.apparentTemperatureCelsius()));
        drawMetric(canvas, 166, 135, "HUMIDITY", percent(weather.relativeHumidityPercent()));
        drawMetric(canvas, 14, 181, "WIND", wind(weather));
        drawMetric(canvas, 166, 181, "PRESSURE", pressure(weather.pressureHectopascals()));

        canvas.drawText(
                "RAIN " + millimeters(weather.precipitationMillimeters()),
                14, DASHBOARD_GEOMETRY.height() - 13, 1, SECONDARY);
        canvas.drawText(
                "OPEN-METEO",
                DASHBOARD_GEOMETRY.width() - 70,
                DASHBOARD_GEOMETRY.height() - 13,
                1,
                SECONDARY);
        return frame;
    }

    public static String description(Integer code) {
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

    private static void drawMetric(
            FrameCanvas canvas, int x, int y, String label, String value) {
        canvas.fillRectangle(x - 5, y - 7, 142, 39, CARD);
        canvas.drawText(label, x, y, 1, SECONDARY);
        canvas.drawText(value, x, y + 13, 2, PRIMARY);
    }

    private static void drawWeatherIcon(
            FrameCanvas canvas, Integer weatherCode, int centerX, int centerY) {
        String condition = description(weatherCode);
        if ("CLEAR".equals(condition)) {
            drawSun(canvas, centerX, centerY);
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
                canvas.drawLine(
                        centerX + offset, centerY + 16,
                        centerX + offset - 4, centerY + 25, RAIN);
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
            canvas.drawLine(
                    centerX + ray[0], centerY + ray[1],
                    centerX + ray[2], centerY + ray[3], SUN);
        }
    }

    private static void drawCloud(FrameCanvas canvas, int centerX, int centerY) {
        canvas.fillCircle(centerX - 13, centerY + 2, 11, SECONDARY);
        canvas.fillCircle(centerX, centerY - 5, 15, SECONDARY);
        canvas.fillCircle(centerX + 15, centerY + 3, 10, SECONDARY);
        canvas.fillRectangle(centerX - 24, centerY, 48, 13, SECONDARY);
    }

    private static void drawCityTitle(FrameCanvas canvas, String city) {
        String label = normalizeCity(city);
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

    private static String normalizeCity(String city) {
        return Normalizer.normalize(city, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT);
    }

    private static String temperature(Double value) {
        return value == null ? "--.- C" : String.format(Locale.ROOT, "%.1f C", value);
    }

    private static String percent(Integer value) {
        return value == null ? "-- %" : value + " %";
    }

    private static String millimeters(Double value) {
        return value == null ? "-- MM" : String.format(Locale.ROOT, "%.1f MM", value);
    }

    private static String pressure(Double value) {
        return value == null ? "---- HPA" : String.format(Locale.ROOT, "%.0f HPA", value);
    }

    private static String wind(WeatherConditions weather) {
        if (weather.windSpeedKilometersPerHour() == null) {
            return "-- KM/H";
        }
        return compass(weather.windDirectionDegrees())
                + " "
                + String.format(Locale.ROOT, "%.0f KM/H", weather.windSpeedKilometersPerHour());
    }

    private static String compass(Double degrees) {
        if (degrees == null) {
            return "";
        }
        String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        int index = (int) Math.round((degrees % 360.0) / 45.0) % directions.length;
        return directions[index];
    }
}

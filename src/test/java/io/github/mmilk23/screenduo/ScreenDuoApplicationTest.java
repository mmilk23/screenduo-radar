package io.github.mmilk23.screenduo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mmilk23.screenduo.aircraft.NearbyAircraft;
import io.github.mmilk23.screenduo.airport.NearbyAirport;
import io.github.mmilk23.screenduo.location.GeoPoint;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScreenDuoApplicationTest {

    @Test
    void findsArgumentsAndFormatsValues() throws Exception {
        assertTrue((Boolean) invoke("hasArgument", new Class<?>[] {String[].class, String.class},
                new String[] {"--dashboard", "--trace-navigation"}, "--dashboard"));
        assertFalse((Boolean) invoke("hasArgument", new Class<?>[] {String[].class, String.class},
                new String[] {"--dashboard"}, "--api-test"));

        assertEquals("short", invoke("fit", new Class<?>[] {String.class, int.class}, "short", 10));
        assertEquals("1234567...", invoke("fit", new Class<?>[] {String.class, int.class}, "123456789012", 10));
        assertEquals("n/a", invoke("value", new Class<?>[] {Number.class, String.class}, null, "%.1f"));
        assertEquals("12.3", invoke("value", new Class<?>[] {Number.class, String.class}, 12.34, "%.1f"));
    }

    @Test
    void printsAircraftWithFallbacksAndTruncatesLongOperator() throws Exception {
        NearbyAircraft aircraft = new NearbyAircraft(
                "e40001", "", "", "An Airline Operator Name That Is Deliberately Longer Than Twenty Eight Characters",
                "Brazil", new GeoPoint(-22.9, -43.2), 12.34, 87.0,
                null, null, null, false);

        String output = captureStdout(() -> invoke("printAircraft",
                new Class<?>[] {NearbyAircraft.class}, aircraft));

        assertTrue(output.contains("e40001"));
        assertTrue(output.contains("..."));
        assertTrue(output.contains("altitude n/a m"));
        assertTrue(output.contains("speed n/a km/h"));
    }

    @Test
    void printsUnknownAircraftOperator() throws Exception {
        NearbyAircraft aircraft = new NearbyAircraft(
                "e40002", "GLO1234", "GLO", "", "Brazil",
                new GeoPoint(-22.9, -43.2), 3.0, 180.0, 1000.0, 500.0, 180.0, false);

        String output = captureStdout(() -> invoke("printAircraft",
                new Class<?>[] {NearbyAircraft.class}, aircraft));

        assertTrue(output.contains("GLO1234"));
        assertTrue(output.contains("unknown operator"));
        assertTrue(output.contains("altitude 1000 m"));
        assertTrue(output.contains("speed 500 km/h"));
    }

    @Test
    void printsAirportUsingIataAndFallbackFields() throws Exception {
        NearbyAirport withIata = new NearbyAirport(
                "SBGL", "GIG", "Rio de Janeiro Galeao International Airport", "Rio de Janeiro",
                "BR", "large_airport", new GeoPoint(-22.8, -43.25), 15.0, 42.0, 9.0);
        NearbyAirport fallback = new NearbyAirport(
                "SBXX", "", "Airport Without Municipality", "", "BR", "small_airport",
                new GeoPoint(-22.0, -43.0), 20.0, 90.0, null);

        String first = captureStdout(() -> invoke("printAirport", new Class<?>[] {NearbyAirport.class}, withIata));
        String second = captureStdout(() -> invoke("printAirport", new Class<?>[] {NearbyAirport.class}, fallback));

        assertTrue(first.contains("GIG"));
        assertTrue(first.contains("Rio de Janeiro"));
        assertTrue(second.contains("SBXX"));
        assertTrue(second.contains("BR"));
    }

    @Test
    void printsAircraftGroupLimitAndOverflowMessage() throws Exception {
        NearbyAircraft first = aircraft("AAA001");
        NearbyAircraft second = aircraft("BBB002");

        String output = captureStdout(() -> invoke("printAircraftGroup",
                new Class<?>[] {String.class, List.class, int.class},
                "Nearby", List.of(first, second), 1));

        assertTrue(output.contains("Nearby: 2"));
        assertTrue(output.contains("AAA001"));
        assertFalse(output.contains("BBB002"));
        assertTrue(output.contains("... and 1 more."));
    }

    private static NearbyAircraft aircraft(String callsign) {
        return new NearbyAircraft(
                "e40003", callsign, "AAA", "Airline", "Brazil",
                new GeoPoint(-22.9, -43.2), 1.0, 1.0, 1000.0, 200.0, 1.0, false);
    }

    private static Object invoke(String name, Class<?>[] parameterTypes, Object... arguments) throws Exception {
        Method method = ScreenDuoApplication.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method.invoke(null, arguments);
    }

    private static String captureStdout(ThrowingRunnable action) throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream replacement = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(replacement);
            action.run();
        } finally {
            System.setOut(original);
        }
        return output.toString(StandardCharsets.UTF_8);
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}

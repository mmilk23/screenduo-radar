package io.github.mmilk23.screenduo.airline.logo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mmilk23.screenduo.data.TextDataSource;
import io.github.mmilk23.screenduo.display.RgbFrame;
import io.github.mmilk23.screenduo.testsupport.StubHttpClient;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CachedAirlineLogoProviderTest {

    @TempDir
    Path tempDirectory;

    @Test
    void rejectsInvalidIcaoWithoutLoadingMetadata() {
        AtomicInteger metadataReads = new AtomicInteger();
        TextDataSource unused = () -> {
            metadataReads.incrementAndGet();
            throw new IOException("must not be called");
        };
        var provider = provider(unused, unused, new StubHttpClient(200, png()), iata -> URI.create("https://fallback/" + iata));

        assertTrue(provider.findLogo(null).isEmpty());
        assertTrue(provider.findLogo("").isEmpty());
        assertTrue(provider.findLogo("AB").isEmpty());
        assertTrue(provider.findLogo("A1C").isEmpty());
        assertEquals(0, metadataReads.get());
    }

    @Test
    void readsExistingRgbLogoAndCachesResult() throws Exception {
        Path logoFile = tempDirectory.resolve("logos/LAN.rgb");
        RgbFrame expected = new RgbFrame(RawRgbLogoStore.LOGO_GEOMETRY);
        expected.setPixel(0, 0, 10, 20, 30);
        RawRgbLogoStore.write(logoFile, expected);

        TextDataSource unused = () -> {
            throw new IOException("must not be called");
        };
        var provider = provider(unused, unused, new StubHttpClient(500, new byte[0]), iata -> URI.create("https://fallback/" + iata));

        RgbFrame first = provider.findLogo(" lan ").orElseThrow();
        RgbFrame second = provider.findLogo("LAN").orElseThrow();

        assertEquals(10, first.redAt(0, 0));
        assertEquals(20, first.greenAt(0, 0));
        assertEquals(30, first.blueAt(0, 0));
        assertTrue(first == second);
    }

    @Test
    void returnsEmptyForCorruptCachedRgbLogo() throws Exception {
        Path logoFile = tempDirectory.resolve("logos/LAN.rgb");
        Files.createDirectories(logoFile.getParent());
        Files.writeString(logoFile, "corrupt");

        TextDataSource unused = () -> {
            throw new IOException("must not be called");
        };
        var provider = provider(unused, unused, new StubHttpClient(200, png()), iata -> URI.create("https://fallback/" + iata));

        assertTrue(provider.findLogo("LAN").isEmpty());
    }

    @Test
    void downloadsLogoFromPassengerMetadataAndPersistsRgbCache() throws Exception {
        Path openFlights = write("airlines.dat", "1,Latam,x,LA,LAN\n");
        Path passenger = write("passenger.json", "[{\"iata\":\"la\",\"logo\":\"https://logos.example/latam.png\"}]");
        StubHttpClient http = new StubHttpClient(200, png());
        var provider = provider(() -> passenger, () -> openFlights, http, iata -> URI.create("https://fallback.example/" + iata + ".png"));

        RgbFrame frame = provider.findLogo("lan").orElseThrow();

        assertEquals(RawRgbLogoStore.LOGO_GEOMETRY, frame.geometry());
        assertEquals(URI.create("https://logos.example/latam.png"), http.lastRequest().uri());
        assertEquals("image/png,image/*", http.lastRequest().headers().firstValue("Accept").orElseThrow());
        assertEquals("screenduo-radar/0.1", http.lastRequest().headers().firstValue("User-Agent").orElseThrow());
        assertTrue(Files.isRegularFile(tempDirectory.resolve("logos/LAN.rgb")));
    }

    @Test
    void usesFallbackLogoWhenPassengerMetadataHasNoMatchingLogo() throws Exception {
        Path openFlights = write("airlines.dat", "1,Latam,x,LA,LAN\n");
        Path passenger = write("passenger.json", "[{\"iata\":\"JJ\",\"logo\":\"https://logos.example/other.png\"}]");
        StubHttpClient http = new StubHttpClient(200, png());
        var provider = provider(() -> passenger, () -> openFlights, http,
                iata -> URI.create("https://fallback.example/" + iata + ".png"));

        assertTrue(provider.findLogo("LAN").isPresent());
        assertEquals(URI.create("https://fallback.example/LA.png"), http.lastRequest().uri());
    }

    @Test
    void returnsEmptyWhenIataMappingIsMissingOrInvalid() throws Exception {
        Path openFlights = write("airlines.dat",
                "1,TooShort,x,X,AAA\n"
                        + "2,Missing,x,\\N,BBB\n"
                        + "3,BadIcao,x,CC,B1C\n"
                        + "4,Incomplete\n");
        Path passenger = write("passenger.json", "[]");
        StubHttpClient http = new StubHttpClient(200, png());
        var provider = provider(() -> passenger, () -> openFlights, http, iata -> URI.create("https://fallback/" + iata));

        assertTrue(provider.findLogo("AAA").isEmpty());
        assertTrue(provider.findLogo("BBB").isEmpty());
        assertEquals(null, http.lastRequest());
    }

    @Test
    void returnsEmptyForInvalidPassengerMetadataAndHttpFailure() throws Exception {
        Path openFlights = write("airlines.dat", "1,Latam,x,LA,LAN\n");
        Path invalidPassenger = write("passenger-invalid.json", "{}");
        var invalidMetadataProvider = provider(
                () -> invalidPassenger,
                () -> openFlights,
                new StubHttpClient(200, png()),
                iata -> URI.create("https://fallback/" + iata));

        assertTrue(invalidMetadataProvider.findLogo("LAN").isEmpty());

        Path passenger = write("passenger.json", "[]");
        StubHttpClient failingHttp = new StubHttpClient(404, new byte[0]);
        var failingDownloadProvider = provider(
                () -> passenger,
                () -> openFlights,
                failingHttp,
                iata -> URI.create("https://fallback.example/" + iata + ".png"));

        assertTrue(failingDownloadProvider.findLogo("LAN").isEmpty());
        assertNotNull(failingHttp.lastRequest());
    }

    @Test
    void restoresInterruptFlagWhenMetadataLoadingIsInterrupted() {
        TextDataSource interrupted = () -> {
            throw new InterruptedException("interrupted");
        };
        var provider = provider(interrupted, interrupted, new StubHttpClient(200, png()), iata -> URI.create("https://fallback/" + iata));

        try {
            assertTrue(provider.findLogo("LAN").isEmpty());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
        assertFalse(Thread.currentThread().isInterrupted());
    }

    private CachedAirlineLogoProvider provider(
            TextDataSource passengerData,
            TextDataSource openFlightsData,
            StubHttpClient httpClient,
            java.util.function.Function<String, URI> fallbackLogoUri) {
        return new CachedAirlineLogoProvider(
                tempDirectory.resolve("logos"),
                passengerData,
                openFlightsData,
                new ObjectMapper(),
                httpClient,
                fallbackLogoUri);
    }

    private Path write(String name, String content) throws IOException {
        Path path = tempDirectory.resolve(name);
        Files.writeString(path, content);
        return path;
    }

    private static byte[] png() {
        try {
            BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
            image.setRGB(0, 0, 0x00FF6600);
            image.setRGB(1, 0, 0x0000AAFF);
            image.setRGB(0, 1, 0x0000AAFF);
            image.setRGB(1, 1, 0x00FF6600);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}

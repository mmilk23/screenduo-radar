package io.github.mmilk23.screenduo.airline.logo;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.zip.Deflater;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemAirlineLogoProviderTest {

    @TempDir
    Path directory;

    @Test
    void loadsRgbLogoByIcaoCode() throws Exception {
        byte[] bytes = new byte[FileSystemAirlineLogoProvider.LOGO_GEOMETRY.pixelCount() * 3];
        bytes[0] = 10;
        bytes[1] = 20;
        bytes[2] = 30;
        Files.write(directory.resolve("TAM.rgb"), bytes);

        var logo = new FileSystemAirlineLogoProvider(directory).findLogo("tam");

        assertTrue(logo.isPresent());
        assertEquals(FileSystemAirlineLogoProvider.LOGO_GEOMETRY, logo.orElseThrow().geometry());
        assertEquals(10, logo.orElseThrow().redAt(0, 0));
        assertEquals(20, logo.orElseThrow().greenAt(0, 0));
        assertEquals(30, logo.orElseThrow().blueAt(0, 0));
    }

    @Test
    void dynamicallyDownloadsAndCachesLogoWhenMissing() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/TAM.png", exchange -> {
            byte[] png = png();
            exchange.getResponseHeaders().add("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, png.length);
            exchange.getResponseBody().write(png);
            exchange.close();
        });
        server.start();
        try {
            String logoUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/TAM.png";
            Path passenger = directory.resolve("passenger.json");
            Path openFlights = directory.resolve("airlines.dat");
            Files.writeString(passenger,
                    "[{\"iata\":\"JJ\",\"name\":\"LATAM Brasil\",\"logo\":\"" + logoUrl + "\"}]",
                    StandardCharsets.UTF_8);
            Files.writeString(openFlights,
                    "1,\"LATAM Brasil\",\\N,JJ,TAM,Brazil,Y,Y\n",
                    StandardCharsets.UTF_8);
            Path logos = directory.resolve("logos");
            var provider = new CachedAirlineLogoProvider(logos,
                    () -> passenger,
                    () -> openFlights,
                    new ObjectMapper(),
                    HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());

            var logo = provider.findLogo("TAM");

            assertTrue(logo.isPresent());
            assertTrue(Files.isRegularFile(logos.resolve("TAM.rgb")));
            assertEquals(FileSystemAirlineLogoProvider.LOGO_GEOMETRY, logo.orElseThrow().geometry());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void fallsBackToKiwiIataUrlWhenDotmarnCatalogDoesNotListAirline() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/airlines/64/G3.png", exchange -> {
            byte[] png = png();
            exchange.getResponseHeaders().add("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, png.length);
            exchange.getResponseBody().write(png);
            exchange.close();
        });
        server.start();
        try {
            Path passenger = directory.resolve("passenger-empty.json");
            Path openFlights = directory.resolve("openflights-glo.dat");
            Files.writeString(passenger, "[]", StandardCharsets.UTF_8);
            Files.writeString(openFlights,
                    "1,\"Gol Transportes Aereos\",\\N,G3,GLO,Brazil,Y,Y\n",
                    StandardCharsets.UTF_8);
            Path logos = directory.resolve("glo-logos");
            var provider = new CachedAirlineLogoProvider(logos,
                    () -> passenger,
                    () -> openFlights,
                    new ObjectMapper(),
                    HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build(),
                    iata -> java.net.URI.create("http://127.0.0.1:" + server.getAddress().getPort()
                            + "/airlines/64/" + iata + ".png"));

            var logo = provider.findLogo("GLO");

            assertTrue(logo.isPresent());
            assertTrue(Files.isRegularFile(logos.resolve("GLO.rgb")));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void ignoresMissingInvalidOrNonIcaoLogoFiles() throws Exception {
        Files.write(directory.resolve("AZU.rgb"), new byte[] {1, 2, 3});
        FileSystemAirlineLogoProvider provider = new FileSystemAirlineLogoProvider(directory);

        assertTrue(provider.findLogo("GLO").isEmpty());
        assertTrue(provider.findLogo("AZU").isEmpty());
        assertTrue(provider.findLogo("12A").isEmpty());
        assertTrue(provider.findLogo(null).isEmpty());
    }

    private static byte[] png() throws IOException {
        byte[] raw = new byte[] {
                0, (byte) 220, 20, 30, 20, (byte) 220, 30,
                0, 20, 30, (byte) 220, (byte) 220, (byte) 220, 30
        };
        Deflater deflater = new Deflater();
        deflater.setInput(raw);
        deflater.finish();
        byte[] buffer = new byte[128];
        int count = deflater.deflate(buffer);
        deflater.end();

        ByteArrayOutputStream png = new ByteArrayOutputStream();
        png.write(new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10});
        chunk(png, "IHDR", ByteBuffer.allocate(13).order(ByteOrder.BIG_ENDIAN)
                .putInt(2).putInt(2).put((byte) 8).put((byte) 2)
                .put((byte) 0).put((byte) 0).put((byte) 0).array());
        chunk(png, "IDAT", java.util.Arrays.copyOf(buffer, count));
        chunk(png, "IEND", new byte[0]);
        return png.toByteArray();
    }

    private static void chunk(ByteArrayOutputStream png, String type, byte[] data) throws IOException {
        png.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(data.length).array());
        png.write(type.getBytes(StandardCharsets.US_ASCII));
        png.write(data);
        png.write(new byte[4]);
    }
}

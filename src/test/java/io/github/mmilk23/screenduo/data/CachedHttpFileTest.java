package io.github.mmilk23.screenduo.data;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CachedHttpFileTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void requestsJsonCachesItAndFallsBackToStaleFileOnHttpError() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        AtomicReference<String> accept = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/schedule", exchange -> {
            accept.set(exchange.getRequestHeaders().getFirst("Accept"));
            if (requests.incrementAndGet() == 1) {
                byte[] body = "[]".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } else {
                exchange.sendResponseHeaders(503, -1);
            }
            exchange.close();
        });
        server.start();
        try {
            Path file = temporaryDirectory.resolve("siros/schedule.json");
            var cache = new CachedHttpFile(
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/schedule"),
                    file, Duration.ofHours(6), "application/json");
            assertEquals("[]", Files.readString(cache.get()));
            assertEquals("application/json", accept.get());
            assertEquals(file, cache.get());
            assertEquals(1, requests.get());

            Files.setLastModifiedTime(file, FileTime.from(Instant.now().minus(Duration.ofHours(7))));
            assertEquals("[]", Files.readString(cache.get()));
            assertEquals(2, requests.get());
            assertThrows(java.io.IOException.class, () -> new CachedHttpFile(
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/schedule"),
                    temporaryDirectory.resolve("missing.json"), Duration.ofHours(6),
                    "application/json").get());
        } finally {
            server.stop(0);
        }
    }
}
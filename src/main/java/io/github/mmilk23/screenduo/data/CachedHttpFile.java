package io.github.mmilk23.screenduo.data;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;

public final class CachedHttpFile implements TextDataSource {

    private final URI source;
    private final Path cacheFile;
    private final Duration maximumAge;
    private final HttpClient httpClient;

    public CachedHttpFile(URI source, Path cacheFile, Duration maximumAge) {
        this.source = source;
        this.cacheFile = cacheFile;
        this.maximumAge = maximumAge;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public Path get() throws IOException, InterruptedException {
        if (isFresh()) {
            return cacheFile;
        }

        try {
            download();
        } catch (IOException exception) {
            if (Files.isRegularFile(cacheFile)) {
                return cacheFile;
            }
            throw exception;
        }
        return cacheFile;
    }

    private boolean isFresh() throws IOException {
        if (!Files.isRegularFile(cacheFile)) {
            return false;
        }
        FileTime lastModified = Files.getLastModifiedTime(cacheFile);
        return lastModified.toInstant().plus(maximumAge).isAfter(Instant.now());
    }

    private void download() throws IOException, InterruptedException {
        Path parent = cacheFile.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temporaryFile = Files.createTempFile(parent, cacheFile.getFileName().toString(), ".part");

        try {
            HttpRequest request = HttpRequest.newBuilder(source)
                    .timeout(Duration.ofSeconds(60))
                    .header("Accept", "text/csv,text/plain")
                    .header("User-Agent", "screenduo-radar/0.1")
                    .GET()
                    .build();
            HttpResponse<InputStream> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                response.body().close();
                throw new IOException(source.getHost() + " returned HTTP "
                        + response.statusCode() + ".");
            }
            try (InputStream body = response.body()) {
                Files.copy(body, temporaryFile, StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(temporaryFile, cacheFile,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException exception) {
                Files.move(temporaryFile, cacheFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }
}

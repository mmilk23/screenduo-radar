package io.github.mmilk23.screenduo.airline.openflights;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OpenFlightsAirlineResolverTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void resolvesActiveAirlineFromCallsignPrefix() throws IOException, InterruptedException {
        Path data = Files.writeString(temporaryDirectory.resolve("airlines.dat"),
                "1,\"JetBlue Airways\",\\N,B6,JBU,JETBLUE,United States,Y\n");
        OpenFlightsAirlineResolver resolver =
                new OpenFlightsAirlineResolver(() -> data);

        assertEquals("JetBlue Airways", resolver.resolve("JBU1936").orElseThrow());
        assertTrue(resolver.resolve("N958JB").isEmpty());
    }
}

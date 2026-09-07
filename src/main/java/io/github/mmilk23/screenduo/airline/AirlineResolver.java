package io.github.mmilk23.screenduo.airline;

import java.io.IOException;
import java.util.Optional;

public interface AirlineResolver {

    Optional<String> resolve(String callsign) throws IOException, InterruptedException;
}

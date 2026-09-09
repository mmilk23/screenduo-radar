package io.github.mmilk23.screenduo.airport;

import java.io.IOException;
import java.util.Optional;

public interface AirportCityProvider {

    Optional<AirportCity> findAirportCity(String airportIcao) throws IOException, InterruptedException;

    default Optional<String> findCity(String airportIcao) throws IOException, InterruptedException {
        return findAirportCity(airportIcao).map(AirportCity::city);
    }
}

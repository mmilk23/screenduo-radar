package io.github.mmilk23.screenduo.airline.logo;

import io.github.mmilk23.screenduo.display.RgbFrame;
import java.util.Optional;

public interface AirlineLogoProvider {

    AirlineLogoProvider NONE = airlineIcaoCode -> Optional.empty();

    Optional<RgbFrame> findLogo(String airlineIcaoCode);
}

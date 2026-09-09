package io.github.mmilk23.screenduo.airport;

public record AirportCity(String city, String displayName) {

    public AirportCity {
        city = city == null ? "" : city.trim();
        displayName = displayName == null ? city : displayName.trim();
        if (displayName.isBlank()) {
            displayName = city;
        }
    }
}

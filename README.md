# ScreenDUO Radar

A small Java 24 application that turns the ASUS ScreenDUO into an aircraft, weather and nearby-airport dashboard.

## Configuration

Copy the example file in the project root:

```powershell
Copy-Item config.ini.example config.ini
```

Edit `config.ini` with your location:

```ini
[location]
latitude = -22.9068
longitude = -43.1729
aircraft_radius_km = 50
airport_radius_km = 100
```

Both radii are optional and default to 50 km for aircraft and 100 km for airports. The real `config.ini` is ignored by Git because future sections may contain API credentials.

## API smoke test

```powershell
mvn exec:java "-Dexec.args=--api-test"
```

The command displays:

- current weather from Open-Meteo;
- nearby airborne and on-ground aircraft from OpenSky;
- airline names resolved from the ICAO callsign prefix;
- nearby airports from OurAirports.

Reference datasets are downloaded on demand to `data/cache/`. Airport data is refreshed after seven days and airline data after 30 days. A stale cache remains usable if a refresh temporarily fails.

OpenSky anonymous access may apply rate limits.

## Data sources and attribution

- [Open-Meteo](https://open-meteo.com/) for weather data.
- [OpenSky Network](https://opensky-network.org/) for live aircraft state vectors.
- [OurAirports](https://ourairports.com/data/) for public-domain airport data.
- [OpenFlights](https://openflights.org/data.php) airline data, available under the Open Database License.

Airline resolution is based on the first three letters of a flight callsign. Private registrations and unknown or outdated designators are shown as `unknown operator`.

## Hardware diagnostics

```powershell
mvn exec:java
mvn exec:java "-Dexec.args=--test-pattern"
mvn exec:java "-Dexec.args=--button-test"
```

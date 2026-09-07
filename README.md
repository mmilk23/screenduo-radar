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
```

The radius is optional and defaults to 50 km. The real `config.ini` is ignored by Git because future sections may contain API credentials.

## API smoke test

The first data integration uses Open-Meteo for current weather and the OpenSky Network for nearby aircraft:

```powershell
mvn exec:java "-Dexec.args=--api-test"
```

OpenSky is queried anonymously in this first version and may apply rate limits.

## Hardware diagnostics

```powershell
mvn exec:java
mvn exec:java "-Dexec.args=--test-pattern"
mvn exec:java "-Dexec.args=--button-test"
```

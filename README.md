# ScreenDUO Radar

A small Java 24 application that turns the ASUS ScreenDUO into an aircraft, weather and nearby-airport dashboard.

## API smoke test

The first data integration uses Open-Meteo for current weather and the OpenSky Network for nearby aircraft. No coordinates or credentials are stored in the repository.

In PowerShell:

```powershell
$env:SCREENDUO_LATITUDE="-22.9068"
$env:SCREENDUO_LONGITUDE="-43.1729"
$env:SCREENDUO_RADIUS_KM="50"
mvn exec:java "-Dexec.args=--api-test"
```

The radius is optional and defaults to 50 km. OpenSky is queried anonymously in this first version and may apply rate limits.

## Hardware diagnostics

```powershell
mvn exec:java
mvn exec:java "-Dexec.args=--test-pattern"
mvn exec:java "-Dexec.args=--button-test"
```

# ScreenDUO Radar

ScreenDUO Radar is a Java 24 application that gives the classic ASUS ScreenDUO a new job as an interactive aircraft, weather and nearby-airport dashboard.

The application currently runs on Windows 11 through libusb/usb4java and communicates directly with the ScreenDUO USB device (`1043:3100`). Its display and data-source boundaries are deliberately hardware-independent, so another target such as a digital photo frame can be added later without rewriting the screens or application rules.

## Current status

The following features have been tested on the physical ScreenDUO:

- USB device detection and descriptor reporting;
- native 320x240 RGB image transfer;
- classic television test pattern;
- button input for `UP`, `DOWN`, `CONFIRM` and `BACK`;
- current-weather dashboard with an automatically fitted city title;
- nearby-airport list with selection, pagination and button debouncing;
- airport selection: `CONFIRM` opens that airport's weather and `BACK` returns to the list;
- recovery and synchronization between a button transaction and the following redraw.

The API smoke test also queries nearby aircraft and resolves an airline name when the flight callsign contains a known ICAO operator prefix. A dedicated aircraft screen and airport arrivals/departures are planned.

## Requirements

- Windows 11 for the currently tested hardware setup;
- [GraalVM JDK 24](https://www.graalvm.org/downloads/) or another JDK 24+;
- Maven 3.9+;
- the ScreenDUO connected through a libusb-compatible Windows driver.

The project includes an experimental GraalVM Native Image Maven profile. JVM execution is the currently validated path.

Verify the toolchain:

```powershell
java -version
mvn -version
mvn test
```

Both commands must report JDK 24 or newer.

## Configuration

Copy the example file in the project root:

```powershell
Copy-Item config.ini.example config.ini
```

Edit `config.ini` with the reference location:

```ini
[location]
latitude = -22.9068
longitude = -43.1729
city = Rio de Janeiro
aircraft_radius_km = 50
airport_radius_km = 100
```

Both radii are optional and default to 50 km for aircraft and 100 km for airports. The real `config.ini` is ignored by Git because future sections may contain API credentials.

## Running

### Weather screen

```powershell
mvn exec:java "-Dexec.args=--weather-screen"
```

The renderer creates an RGB frame without Swing or JavaFX. The 320x240 dashboard is automatically fitted and letterboxed for displays with other resolutions or aspect ratios.

### Nearby-airport browser

```powershell
mvn exec:java "-Dexec.args=--airport-screen"
```

Controls:

- `UP` and `DOWN`: move the highlighted selection;
- `CONFIRM`: query and display current weather at the selected airport;
- `BACK`: return from weather to the list, or exit while already on the list.

The list shows five airports per page and preserves the selected position when returning from weather.

### API smoke test

```powershell
mvn exec:java "-Dexec.args=--api-test"
```

The command displays:

- current weather from Open-Meteo;
- nearby airborne and on-ground aircraft from OpenSky;
- airline names resolved from the ICAO callsign prefix;
- nearby large and medium airports from OurAirports;
- nearby small airports as a fallback when no large or medium airport exists.

Reference datasets are downloaded on demand to `data/cache/`. Airport data is refreshed after seven days and airline data after 30 days. A stale cache remains usable if a refresh temporarily fails. OpenSky anonymous access may apply rate limits.

### Hardware diagnostics

```powershell
mvn exec:java
mvn exec:java "-Dexec.args=--test-pattern"
mvn exec:java "-Dexec.args=--button-test"
```

The first command only detects the device and prints its descriptors. Use the test pattern before debugging application screens, and use the 30-second button test to inspect physical input.

## Architecture and protocol

- [Architecture](docs/ARCHITECTURE.md): components, extension points, state flow and roadmap.
- [ScreenDUO USB protocol](docs/SCREENDUO-PROTOCOL.md): descriptors, frame format, transfers, buttons and synchronization rules.

The most important hardware rule is that button responses must be fully drained before clearing the IN endpoint and starting an image transfer. Changing that order can leave the device stalled with `USB error 9: Pipe error`.

## Data sources and attribution

- [Open-Meteo](https://open-meteo.com/) for weather data.
- [OpenSky Network](https://opensky-network.org/) for live aircraft state vectors.
- [OurAirports](https://ourairports.com/data/) for public-domain airport data.
- [OpenFlights](https://openflights.org/data.php) airline data, available under the Open Database License.

Airline resolution is based on the first three letters of a flight callsign. Private registrations and unknown or outdated designators are shown as `unknown operator`. The country supplied by OpenSky is the aircraft registration country, not the flight origin.

## Development workflow

Development work is integrated into `development` through short-lived feature branches. `main` represents the stable line.

Before committing:

```powershell
mvn test
```

Hardware-facing changes must additionally be tested on the physical ScreenDUO. Automated tests cannot reproduce USB timing, endpoint stalls or Windows driver behavior.

## Native Image experiment

With GraalVM selected:

```powershell
mvn -Pnative package
```

Native compilation is a future optimization goal. usb4java/JNI and any reflection used by dependencies may require additional Native Image metadata before this command produces a fully functional executable.

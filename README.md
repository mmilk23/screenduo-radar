# ScreenDUO Radar

[![Maven CI](https://github.com/mmilk23/screenduo-radar/actions/workflows/maven.yaml/badge.svg)](https://github.com/mmilk23/screenduo-radar/actions/workflows/maven.yaml)
[![Native Windows Build](https://github.com/mmilk23/screenduo-radar/actions/workflows/native-windows.yaml/badge.svg)](https://github.com/mmilk23/screenduo-radar/actions/workflows/native-windows.yaml)
[![OWASP Dependency Check](https://github.com/mmilk23/screenduo-radar/actions/workflows/dependency-check.yaml/badge.svg)](https://github.com/mmilk23/screenduo-radar/actions/workflows/dependency-check.yaml)
[![Snyk](https://github.com/mmilk23/screenduo-radar/actions/workflows/snyk.yaml/badge.svg)](https://github.com/mmilk23/screenduo-radar/actions/workflows/snyk.yaml)
[![Last Updated](https://img.shields.io/github/last-commit/mmilk23/screenduo-radar.svg)](https://github.com/mmilk23/screenduo-radar/commits/main)

ScreenDUO Radar turns the classic ASUS ScreenDUO into a small aviation dashboard. It shows a local weather clock, nearby aircraft, nearby airports, airport weather and scheduled arrival/departure boards on the original 320x240 USB display.

The current hardware target is Windows 11 with the ASUS ScreenDUO USB device (`1043:3100`) using a libusb-compatible driver. The application talks directly to the device; no ASUS software is required while ScreenDUO Radar is running.

## For regular users

Download the latest Windows package from the project releases page:

- [ScreenDUO Radar releases](https://github.com/mmilk23/screenduo-radar/releases)

Download `screenduo-radar-windows-native.zip`, extract it to a folder such as `C:\ScreenDUO Radar`, and keep the `.exe` and any included `.dll` files together in the same folder.

Create a `config.ini` file next to `screenduo-radar.exe` with your location:

```ini
[location]
latitude = -22.9068
longitude = -43.1729
city = Rio de Janeiro
aircraft_radius_km = 50
airport_radius_km = 100
```

Then run:

```powershell
.\screenduo-radar.exe
```

The device briefly shows a classic TV test pattern and then opens the clock screen. Use the physical ScreenDUO buttons:

| Action | Button |
| --- | --- |
| Open local weather | Button 1 |
| Open nearby airports | Button 2 |
| Open nearby aircraft from the clock or weather screen | `CONFIRM` |
| Move through lists | `UP` / `DOWN` |
| Open the selected item | `CONFIRM` |
| Go back | `BACK` |

Some data comes from public online services. Weather comes from Open-Meteo, nearby aircraft from OpenSky, airport data from OurAirports, airline names from OpenFlights, airline logo links from dotmarn/Airlines and Brazilian-territory scheduled routes/boards from ANAC SIROS. SIROS data is planned schedule data, not live operational confirmation.

If there is no release yet, open the [Native Windows Build workflow](https://github.com/mmilk23/screenduo-radar/actions/workflows/native-windows.yaml), choose a successful run, and download the `screenduo-radar-windows-native` artifact.

## For developers

ScreenDUO Radar is a Java 24+ application built with Maven. The code is organized around display and input interfaces so another target, such as a digital photo frame, can be added later without rewriting the weather, aircraft, airport or flight-board features.

## Current status

The following features have been tested on the physical ScreenDUO:

- USB device detection and descriptor reporting;
- native 320x240 RGB image transfer;
- classic television test pattern;
- button input for `UP`, `DOWN`, `CONFIRM` and `BACK`;
- local weather clock dashboard with a digital clock, amber CGA-style date and day/night-aware weather icon;
- current-weather dashboard with an automatically fitted city title;
- nearby-airport list with selection, pagination and button debouncing;
- airport selection: `CONFIRM` opens that airport's weather and `BACK` returns to the list;
- recovery and synchronization between a button transaction and the following redraw.

The API smoke test also queries nearby aircraft and resolves an airline name when the flight callsign contains a known ICAO operator prefix. The nearby-aircraft browser, integrated dashboard and SIROS airport schedule boards are implemented and covered by automated tests; the main dashboard navigation has been validated on the physical ScreenDUO during development.

## Development requirements

- Windows 11 for the currently tested hardware setup;
- [GraalVM JDK 24](https://www.graalvm.org/downloads/) or another JDK 24+;
- Maven 3.9+;
- the ScreenDUO connected through a libusb-compatible Windows driver.

The project includes a GraalVM Native Image Maven profile. Native compilation on Windows should use GraalVM JDK 25.0.4 or newer; with explicit usb4java and DirectBuffer JNI metadata, the native executable has been validated for the test pattern and the dashboard startup clock. Full native dashboard navigation still needs longer hardware validation.

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

### Integrated dashboard

```powershell
mvn exec:java "-Dexec.args=--dashboard"
```

The dashboard starts on the local weather clock and uses the ScreenDUO's two dedicated action buttons as global shortcuts:

| Screen / action | Control |
| --- | --- |
| Open local weather from any screen | Physical button 1 (`ACTION_1`, existing native code 12) |
| Return to the clock after opening weather | Press physical button 1 twice in sequence |
| Open nearby airports from any screen | Physical button 2 (`ACTION_2`, existing native code 13) |
| Clock or local weather: open nearby aircraft | `CONFIRM` |
| Aircraft or airport list: move selection | `UP` / `DOWN` |
| Aircraft list: open aircraft details and planned route | `CONFIRM` |
| Airport list: open selected airport's weather | `CONFIRM` |
| Airport list or airport weather: open its flight board | `RIGHT` |
| Flight board: arrivals / departures | `LEFT` / `RIGHT` (`CONFIRM` also toggles) |
| Flight board: move selection and page | `UP` / `DOWN` |
| Data error: retry | `CONFIRM` |
| Return to the parent screen | `BACK` |
| Clock: stay on the clock | `BACK` |

Returning from aircraft details preserves the aircraft selection. Returning from airport weather preserves the airport selection. The flight board returns to whichever screen opened it (airport list or airport weather) and preserves separate arrival/departure selections while the same airport and date remain loaded. `BACK` from local weather or either nearby list returns to the clock. Button 1 and button 2 also work from details, boards and error screens.

The flight board shows the selected airport's **scheduled** arrivals or departures for the UTC date shown on screen, sorted by the corresponding arrival/departure time. Each row contains the planned time in UTC, operating callsign and the other airport, including its city when available from OurAirports. It includes the full UTC day, including earlier flights; it does not indicate whether they actually operated. An empty board means no matching SIROS schedule was found, not proof that the airport has no traffic.

SIROS coverage is limited to registrations for operations involving Brazilian territory. The board is not a live airport operations feed and does not confirm delays, cancellations, diversions, actual arrivals or actual departures. See the detailed SIROS limitations below.

The clock shows the local system time with a seven-segment digital style and refreshes while it is visible. Its weather icon uses Open-Meteo day/night data, showing a moon phase for clear nighttime conditions when moon data is available. Weather, nearby lists and route results are loaded on demand and retained during the dashboard session; there is no automatic live refresh for remote snapshots. A flight board reloads when opened for a different airport or UTC date, using the shared SIROS cache. Exit and restart the dashboard for fresh snapshots. The on-screen footer shows contextual controls and both global shortcuts.

To diagnose a button that appears unresponsive, compile the current source and enable navigation tracing:

```powershell
mvn compile exec:java "-Dexec.args=--dashboard --trace-navigation"
```

The console records the native code, decoded button, current view, debounce decision, requested transitions and completed frame sends. For local weather, a recognized `CONFIRM` should request `LOCAL_WEATHER -> AIRCRAFT_LIST`. Tracing does not change USB polling, drainage or endpoint synchronization.

### Native Windows executable

```cmd
scripts\build-native-windows.cmd
```

The native build produces `target\screenduo-radar.exe` plus companion DLLs generated by Native Image. Keep those DLLs next to the executable when copying it elsewhere. See [Running ScreenDUO Radar as a Windows service](docs/WINDOWS-SERVICE.md) for the WinSW service wrapper layout and install commands.

The native executable has been validated through startup, usb4java native library loading and ScreenDUO device open. If Windows returns access denied, adjust the driver permissions or the Windows account used to run the process.
The dedicated legacy modes below remain available. Running without arguments shows a short startup test pattern and then opens the integrated dashboard.

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

### Nearby-aircraft browser

```powershell
mvn exec:java "-Dexec.args=--aircraft-screen"
```

Uses the location and `aircraft_radius_km` from `config.ini`. The list includes airborne and on-ground aircraft, ordered by distance, with four aircraft per page.

- `UP` and `DOWN`: move the selection, wrapping at the ends of the list.
- `CONFIRM`: open details for the selected aircraft.
- `BACK`: return to the list with the selection preserved, or exit from the list.
- On a data error, `CONFIRM` retries the query and `BACK` exits.

The list shows callsign, airline and distance. Details show callsign, airline, distance in kilometers, bearing from the configured location, altitude in meters, speed in kilometers per hour and airborne/ground status. Bearing describes where the aircraft is relative to the observer, not its heading. Missing callsigns fall back to the ICAO24 identifier; unknown airlines and unavailable measurements are labeled explicitly.

Data is fetched once on entry. The snapshot label shows the query completion time in UTC, not the aircraft observation time. There is no automatic refresh in this increment; exit and reopen the browser to fetch a new snapshot. List navigation uses the loaded aircraft data. Opening details also resolves a planned route through SIROS, as described below.

### Planned routes from ANAC SIROS: Brazilian territory only

The aircraft browser uses the public [ANAC SIROS API](https://sas.anac.gov.br/sas/siros_api) to enrich flight details with a **planned** origin and destination, displayed as city names when available from OurAirports, adding the IATA code only for cities with multiple scheduled airports when that helps disambiguation (for example, `Rio de Janeiro / GIG > Paris / CDG`) under `PLANNED ROUTE`. This requires no API subscription or credentials and is enabled automatically in `--aircraft-screen`.

**Coverage is limited to SIROS registrations for operations involving Brazilian territory. It is not a worldwide route service.** These records can include domestic flights and international operations involving Brazil; they do not guarantee a route for every aircraft seen over Brazil. Foreign aircraft are not excluded by nationality, and the application does not use a geographic bounding box as proof of SIROS coverage. Private flights, missing registrations, nonmatching callsigns and flights outside this coverage may have no route.

SIROS contains airline-submitted schedules, not live confirmation of a flight's actual origin or destination. Delays, cancellations, diversions and schedule changes may make the planned route differ from reality. OpenSky remains the source for aircraft positions; SIROS only supplies the planned route.

Matching rules:

- Match the operating airline's three-letter ICAO code and numeric flight number to the OpenSky callsign, ignoring case, surrounding whitespace and leading zeros.
- Use the aircraft snapshot's query time in UTC, with a two-hour tolerance before scheduled departure and after scheduled arrival. This is a matching heuristic, not proof that a flight is operating.
- Fetch schedules for the preceding UTC day, reference day and following day to handle overnight flights.
- Require a single distinct matching schedule. Overlapping stages or conflicting registrations produce `ROUTE UNAVAILABLE`; the first result is never chosen arbitrarily.
- Do not guess routes from alphanumeric callsigns, aircraft nationality, direction of travel or codeshare numbers.

The first detail view for a callsign may show `LOADING ROUTE`. Schedules are cached under `data/cache/siros/`, keyed by the reference UTC date. On first access, files older than six hours are refreshed; an existing file remains usable if the download fails. The parsed schedule is reused for the browser snapshot. Lookup results, including missing routes and failures, are remembered during that browser session. Exit and reopen the browser to retry or refresh.

If SIROS is unavailable or no unambiguous match exists, `ROUTE UNAVAILABLE` is shown and aircraft details and navigation remain usable. The browser performs network and display operations serially and preserves the validated USB implementation.

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

### Startup mode

```powershell
mvn exec:java
```

With no arguments, the application sends the classic TV test pattern briefly and then opens the integrated dashboard on the clock screen. This acts as a simple power-on visual check before the normal dashboard loop starts. `--trace-navigation` by itself follows the same startup path with navigation logs enabled.

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
- [ANAC SIROS](https://sas.anac.gov.br/sas/siros_api) for planned flight routes in its Brazilian-territory coverage. Schedule data is supplied by the airlines.
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

## GitHub Actions

The repository includes workflows for Maven CI, dependency review, OWASP Dependency-Check, Snyk and Windows Native Image builds. The Maven workflow runs on Ubuntu and Windows with GraalVM JDK 25.

The native Windows workflow can be started manually from the Actions tab, and also runs for `v*` tags and published GitHub releases. It builds `target/screenduo-radar.exe`, packages it with any companion DLLs and uploads `screenduo-radar-windows-native.zip` as a workflow artifact. When triggered by a published GitHub release, the same zip is attached to the release for direct download.

## Future work

- Publish Linux builds for users who want to run ScreenDUO Radar from a small Linux box or desktop machine.
- Publish Raspberry Pi builds, especially for kiosk-style dashboards attached to small HDMI displays.
- Add display adapters beyond the ASUS ScreenDUO, keeping renderers based on `RgbFrame` and isolating each transport in its own adapter.
- Add a desktop/file preview display so layouts can be reviewed without USB hardware.
- Evaluate a Raspberry Pi Zero 2 W or Raspberry Pi 5 with a 5-inch 800x480 HDMI display as the first non-ScreenDUO target. This path is likely simpler than reverse engineering closed digital photo frames, and it avoids the memory/rendering constraints of ESP32 boards.
- Investigate e-paper frames for slow-refresh dashboards. They are attractive for wall displays, but less suitable for live aircraft lists or clock seconds because refresh time and ghosting become visible.

## Native Image

With GraalVM selected and the Visual Studio C++ x64 build tools available:

```powershell
mvn -Pnative package
```

On Windows, `scripts\build-native-windows.cmd` prepares the x64 Visual Studio environment before running Maven. The current Native Image metadata includes usb4java native resources and usb4java JNI access. Keep the generated companion DLLs next to `screenduo-radar.exe` when copying the executable.

# Running ScreenDUO Radar as a Windows service

The recommended wrapper is [WinSW](https://github.com/winsw/winsw). It runs an executable as a Windows service, keeps the service definition in a small XML file, and provides simple `install`, `start`, `stop`, `status` and `uninstall` commands.

ScreenDUO Radar can currently be installed as a JVM service. The JVM path remains the most exercised hardware path for rendering and button navigation. Native Image should be built with GraalVM JDK 25.0.4 or newer; with explicit JNI metadata, the native executable can send the test pattern and start the dashboard clock, while full navigation still needs longer hardware validation.

## Build the JVM service payload

Use JDK 24 or newer:

```cmd
scripts\package-windows-service.cmd
```

The script prepares `target\service` with:

- `screenduo-radar.jar`
- runtime dependencies under `lib\`
- `screenduo-radar-service.xml`
- `config.ini`, when present in the project root

The service XML expects `JAVA_HOME` to point to a JDK 24+ installation because the executable is `%JAVA_HOME%\bin\java.exe`.

## Prepare the service directory

Create an installation directory, for example `C:\ScreenDUO-Radar`, and copy the contents of `target\service` into it.

Download a WinSW v3 x64 executable from the WinSW releases page and place it in the same directory. Rename it to match the XML file base name:

```text
screenduo-radar-service.exe
screenduo-radar-service.xml
```

The XML uses `%BASE%`, which WinSW expands to the directory containing the wrapper. ScreenDUO Radar will therefore read `config.ini` from the same service directory.

## Install and control the service

Run these commands from an elevated PowerShell or Command Prompt inside the service directory:

```cmd
.\screenduo-radar-service.exe install
.\screenduo-radar-service.exe start
.\screenduo-radar-service.exe status
```

To stop and remove the service:

```cmd
.\screenduo-radar-service.exe stop
.\screenduo-radar-service.exe uninstall
```

## USB access notes

The service must run under an account that can open the ScreenDUO libusb device. Test the same command used by the service before installing it:

```cmd
%JAVA_HOME%\bin\java.exe -cp "screenduo-radar.jar;lib\*" io.github.mmilk23.screenduo.ScreenDuoApplication --test-pattern
```

If the service starts but the hardware does not update, check the WinSW logs and verify that the service account can access the libusb driver. Windows services run outside the interactive desktop session. That is acceptable for ScreenDUO Radar because rendering is sent directly over USB and no application window is expected.

## Native Image status

Native compilation is still useful to track, but it is not the recommended service runtime yet.

The native executable currently validates these steps:

- starts without the usb4java native-access warning;
- loads the bundled usb4java native library;
- detects and opens the ScreenDUO;
- prints the USB descriptors.

With GraalVM JDK 25.0.4 and the current JNI metadata, `--test-pattern` sends a frame successfully and `--trace-navigation` by itself performs the no-argument startup path, sends the brief test pattern and keeps the dashboard running on the clock screen. Full native navigation still needs longer hardware validation before switching the service wrapper from JVM to native.

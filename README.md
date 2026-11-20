# FTP Media

A modern Android app for browsing and streaming video/audio straight from **any FTP or
FTPS server you add yourself**. It ships with **no servers preconfigured** — you bring
your own (your NAS, a home server, a lawful public directory), and it plays what's there.

> [!IMPORTANT]
> Use this app only with servers and content you are allowed to access and play.
> The app contains no sources, no scraping of any site's catalog, and no attempt to
> disguise or unlock access on any network. What you point it at is your responsibility.

## Features

- Add any number of FTP/FTPS sources (host, port, credentials, start path)
- Test-connection button with live result
- Fast directory browsing with folders-first sorting, search/filter, sort by
  name / size / date
- Streams media **directly** from the server — nothing is fully downloaded first
- Seeking supported via FTP REST (server-dependent)
- Hardware-accelerated playback (ExoPlayer) with transport controls
- Material 3 UI, dark and light themes

## Build

This project was authored to be built with a standard Android toolchain
(cloud sandboxes without the Android SDK cannot compile it).

1. Install [Android Studio](https://developer.android.com/studio) (it bundles a JDK 17).
2. `File → Open…` and select this repository.
3. Let Gradle sync — it downloads the Android SDK components and dependencies
   (AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01, Media3 1.5.1, commons-net 3.11.1).
4. `Run ▶` on a device or emulator (minSdk 24 / Android 7.0).

Or from the command line, with `sdk.dir` set in `local.properties` or `ANDROID_HOME`:

```
gradle :app:assembleDebug   # produces app/build/outputs/apk/debug/app-debug.apk
```

## Usage

1. Tap **Add source**, enter host/port (default 21), credentials (or leave blank for
   anonymous access), and a start path.
2. Optional: enable **FTPS (explicit TLS)** for encrypted control/data connections.
3. Save — the source appears as a card. Tap it to browse.
4. Tap a media file to play. Common containers (mp4, mkv, webm, mp3, flac, …) work
   through the platform codecs; exotic formats may need a transcoding server instead.

## Notes and limitations

- Passwords are stored in app-private storage in plain JSON. Fine for a personal phone,
  not a secure credential vault.
- Streaming/seeking quality depends on the server: passive mode and REST support are
  assumed. Very old FTP servers may not seek or report sizes.
- FTPS uses the device's default trust store; servers with self-signed certificates
  will be rejected rather than silently trusted.
- Subtitle sidecar files (.srt/.vtt next to the video) are not auto-loaded yet.

# Watch Cartoons Online

<img src="wco.png" alt="Watch Cartoons Online Logo" width="202"/>

**Watch Cartoons Online** is an unofficial Android TV client, built with Jetpack Compose and Media3.

## Disclaimer

**This is an unofficial app.** No copyrighted material is hosted in the app, or by the app developer. All content is accessed directly from third-party streaming services.

## About Watch Cartoons Online

Watch Cartoons Online (often found under various domain names such as WCOStream, TheWatchCartoonOnline, or similar variations) is a popular unofficial streaming website dedicated to animated content. It hosts a vast library of material, ranging from Western cartoons (including shows from networks like Cartoon Network, Nickelodeon, and Disney) to Japanese anime (available in both dubbed and subtitled formats).

The site is widely known for its extensive catalog, which includes not only current series and movies but also a significant collection of older, retro cartoons that may be difficult to find on mainstream legal platforms.

## Features

- **Native Android TV Experience**: Designed specifically for Leanback devices (Google TV, Android TV).
- **Extensive Catalog**: Browse through thousands of cartoons and anime.
- **Modern Player**: High-quality playback using Android Media3 (ExoPlayer).
- **Search and Favorites**: Easily find and save your favorite shows.
- **Lightweight**: Fast performance with Jetpack Compose UI.

## Development

### Building the Project

To build the project and check for errors, use the Gradle wrapper:

```bash
./gradlew assembleDebug
```

### Running in Emulator

To run the app within a Google TV emulator:

```bash
./run_emulator.sh
```

### Logging

To monitor logs from the emulator or device:

```bash
adb logcat -s WCO_TV WCO_TV_JS WCO_TV_PLAYER
```

## Technologies Used

- **UI**: Jetpack Compose with Compose for TV
- **Networking**: OkHttp & JSoup (for content parsing)
- **Playback**: Android Media3 (ExoPlayer)
- **Image Loading**: Coil
- **JSON**: Gson
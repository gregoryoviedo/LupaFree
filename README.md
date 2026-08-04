# Lupa Free

Lupa Free is a small, open-source Android magnifier focused on privacy, clarity, and everyday accessibility. It has no ads, accounts, analytics, or network permission.

## Features

- Live rear-camera magnifier powered by CameraX.
- Hardware-supported zoom shown as a live ratio.
- Slider and pinch-to-zoom controls.
- Tap-to-focus with a temporary focus reticle.
- Flashlight control when the device provides a flash unit.
- Freeze the current frame in memory without writing it to storage.
- Camera error feedback and retry action.
- Spanish, English, German, and Portuguese translations.
- Automatic device-language selection with English as the fallback.
- OLED-friendly black interface.
- MIT license, GitHub access, and Binance Pay donation information.

The app deliberately uses CameraX only. The maximum zoom depends on the camera capabilities exposed by Android and the device manufacturer. It does not attempt to reproduce proprietary camera processing from vendor camera applications.

## Screenshots

Screenshots will be added under `docs/screenshots/`.

## Technology

| Layer | Technology |
| --- | --- |
| Language | Kotlin 2.4.10 |
| UI | Jetpack Compose |
| Material | Material 3 |
| Camera | CameraX 1.6.1 |
| Lifecycle | AndroidX Lifecycle 2.11.0 |
| Build | Android Gradle Plugin 9.3.0 |
| Minimum Android | Android 11 (API 30) |
| Compile SDK | 37 |
| Target SDK | 36 |

## Project Structure

```text
LupaFree/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── java/com/example/lupafree/
│       │   │   ├── MainActivity.kt
│       │   │   └── ui/
│       │   │       ├── MagnifierScreen.kt
│       │   │       ├── MagnifierViewModel.kt
│       │   │       └── theme/
│       │   └── res/
│       │       ├── drawable/
│       │       ├── mipmap-*/
│       │       └── values*/
│       ├── test/
│       └── androidTest/
├── gradle/libs.versions.toml
├── CONTRIBUTING.md
├── LICENCE.md
└── README.md
```

## Requirements

- Android Studio with support for AGP 9.3.0.
- JDK 11 or newer.
- Android SDK platform 37.
- A physical Android device or emulator with a rear camera.

## Build and Install

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

Run the unit tests with:

```bash
./gradlew :app:testDebugUnitTest
```

Run the Android tests on a connected device or emulator with:

```bash
./gradlew :app:connectedDebugAndroidTest
```

## Release Builds

Release signing reads credentials from `~/.gradle/gradle.properties` or environment variables. Never commit a keystore or passwords.

```properties
LUPAFREE_KEYSTORE_PATH=/path/to/lupafree-release.keystore
LUPAFREE_KEYSTORE_PASSWORD=your-password
LUPAFREE_KEY_ALIAS=lupafree
LUPAFREE_KEY_PASSWORD=your-password
```

Build a signed release when the keystore exists:

```bash
./gradlew :app:assembleRelease
./gradlew :app:bundleRelease
```

The release build enables R8 and resource shrinking. Verify the generated artifact on a physical device before distribution.

## Usage

1. Open Lupa Free and grant camera access.
2. Adjust zoom with the slider or pinch gesture.
3. Tap the preview to focus on a specific point.
4. Use the flashlight button when additional light is needed.
5. Freeze and resume the current frame with the pause button.
6. Open More options for the license, permissions, GitHub, or donation information.

## Privacy and Permissions

The app requests only camera access at runtime. The flashlight permission is declared for device compatibility. No Internet, storage, location, microphone, account, or tracking permission is used.

Frozen frames remain in memory and are not saved to disk. The app contains no analytics, advertising SDK, or remote service.

## Donation

Donations are supported through Binance Pay.

**Binance Pay ID:** `371811579`

## Configuration

The application links are defined in `app/src/main/java/com/example/lupafree/ui/MagnifierScreen.kt`.

Translations are stored in:

- `app/src/main/res/values/strings.xml` for English and fallback text.
- `app/src/main/res/values-es/strings.xml` for Spanish.
- `app/src/main/res/values-de/strings.xml` for German.
- `app/src/main/res/values-pt/strings.xml` for Portuguese.

Android selects the closest matching device locale. English is used when no supported translation matches.

## Known Limitations

- Only the rear camera is selected.
- Zoom is limited to the range reported by CameraX and the device camera HAL.
- The app does not use manufacturer-specific camera APIs or proprietary super-resolution.
- Frozen frames are intentionally not persisted.
- Video recording and image export are not included.

## Roadmap

- Optional camera selector.
- Optional image capture with explicit storage behavior.
- Improved focus and exposure feedback.
- Additional accessibility refinements.
- More device compatibility testing.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for build, testing, and design guidelines.

## License

Lupa Free is distributed under the MIT License. See [LICENCE.md](LICENCE.md).

Copyright (c) 2026 Gregory Oviedo.

## Credits

- CameraX, Jetpack Compose, Material 3, and AndroidX are provided by Google and the Android Open Source Project.
- The launcher icon is an original minimal vector asset for Lupa Free.

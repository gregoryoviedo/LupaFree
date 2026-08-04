# Contributing to Lupa Free

## Before Opening a Pull Request

Run the relevant checks:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Run connected Android tests when a device or emulator is available:

```bash
./gradlew :app:connectedDebugAndroidTest
```

## Project Guidelines

- Keep CameraX as the camera abstraction unless a documented product requirement changes.
- Do not add advertising, analytics, tracking, or network dependencies.
- Keep frozen images in memory unless storage behavior is explicitly designed and documented.
- Put user-facing text in Android string resources.
- Add translations for English, Spanish, German, and Portuguese when adding UI text.
- Preserve the black, minimal, accessibility-focused interface.
- Test camera behavior on a physical device when changing lifecycle, focus, torch, or zoom code.

## Pull Requests

Describe the user-visible change, the affected Android versions, and the commands used for verification. Include screenshots for UI changes and note any device-specific camera behavior.

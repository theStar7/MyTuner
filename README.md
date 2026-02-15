# MyTuner

A minimal real-time instrument tuner for Android.

Listens to your microphone, detects pitch using a YIN algorithm, and shows the nearest note with cent deviation on an animated gauge.

## Features

- Real-time pitch detection (YIN autocorrelation, no third-party libs)
- Animated tuner gauge with sharp/flat indication
- Live frequency history graph
- Dark theme, single-screen UI

## Tech Stack

- Kotlin + Jetpack Compose (Material 3)
- MVVM (ViewModel + StateFlow)
- AudioRecord API (44100 Hz, mono)
- Min SDK 24 / Target SDK 34

## Build

```
./gradlew assembleDebug
```

## Permissions

- `RECORD_AUDIO` -- required for microphone access

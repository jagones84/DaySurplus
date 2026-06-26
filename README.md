# DaySurp

DaySurp is an Android app for tracking surplus, expenses, incomes, grouped category history, and financial charts.

## Requirements

- Android Studio
- JDK 17
- Android SDK matching the Gradle project configuration

## Install

1. Clone the repository.
2. Open the project in Android Studio.
3. Let Gradle sync the Android dependencies.

## Run

1. Connect an Android device or start an emulator.
2. Run the `app` configuration from Android Studio.

CLI build:

```bash
./gradlew assembleDebug
```

Unit tests:

```bash
./gradlew testDebugUnitTest
```

## Features

- daily surplus tracking
- expense and income categorization
- custom categories for expenses and incomes
- collapsible grouped history by category
- expense and income pie charts
- expense ratio and saving ratio analytics

## Data

Data is stored locally on-device with DataStore Preferences and JSON serialization. Existing transactions are normalized on load so taxonomy updates do not lose prior entries.

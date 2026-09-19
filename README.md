# DaySurplus

DaySurplus is an Android app for tracking surplus, expenses, incomes, grouped category history, and financial charts.

## Requirements

- Android Studio (or JDK 17+ on the CLI)
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

- daily surplus tracking with an idempotent WorkManager chain (no double-apply after restarts)
- expense and income categorization with keyword-based inference
- custom categories for expenses and incomes (create, rename, delete)
- collapsible grouped history by category with search, scope, and category filters
- global date range filter with preset windows (7/30/90/180/365 days)
- expense and income pie charts with monthly averages
- expense ratio and saving ratio analytics with weekly/monthly/yearly aggregation (ISO week keys)
- JSON backup export/import (replace or merge mode) and CSV transaction export
- daily automatic backup with 7-file rotation (app external files dir, `backups/`)
- structured logging (Logcat + realtime daily file in app `files/logs/`)
- English and Italian localization

## Data

Data is stored locally on-device with DataStore Preferences and JSON serialization. Existing transactions are normalized on load so taxonomy updates do not lose prior entries. All writes that touch the running total are performed as single atomic DataStore edits.

Backup files are validated (schema version, finite values, non-negative dates) before import. The daily automatic backup writes to the app-specific external directory, so no storage permission is required.

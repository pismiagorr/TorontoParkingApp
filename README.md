# Toronto Parking App

This repository contains an Android app for Toronto parking. It includes:
- Login flow
- Free vs paid parking filter
- Map view with route line
- Save favorite locations
- Search and nearby parking cards

## Requirements
- Android Studio Koala or later
- JDK 17
- Google Maps API key

## Setup
1. Open the project in Android Studio.
2. Replace `YOUR_GOOGLE_MAPS_API_KEY` in `app/src/main/AndroidManifest.xml` with a valid API key.
3. Sync Gradle.
4. Run the app on an Android device or emulator.

## Features
- Email/password login screen
- Persistent login state with DataStore
- Favorites saved locally
- Filter by all, free, and paid parking
- Map markers and route visualization

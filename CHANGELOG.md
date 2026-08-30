# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); this project
uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

First public release, prepared for Google Play. Development up to this point happened in a
private repository.

### Added
- English and German localisation. English is the default; the UI previously existed only
  in German.
- An **About → Show licenses** screen that displays the full third-party attribution.

### Changed
- Minimum Android version lowered from 15 to **11** (`minSdk` 35 → 30). The app never used
  any Android 15 API.
- `targetSdk` raised to 36 (Android 16), with the toolchain moved to AGP 8.13 / Gradle
  8.14 / Kotlin 2.2.
- The screen-orientation setting was removed. From `targetSdk` 36 Android ignores
  orientation locks on displays of 600dp and wider — that is, on the tablets this app is
  built for — so the setting could only have looked broken. The layout was designed for
  both orientations from the start.
- The wordmark is now generated from the Montserrat font by `tools/generate_wordmark.py`,
  giving it a complete and reproducible provenance.
- The chess pieces now elect the **BSD 3-Clause** option of Cburnett's multi-licensed
  Staunton set instead of CC BY-SA 3.0, which removes a share-alike obligation on the
  artwork without losing the attribution.

### Fixed
- The third-party licence file was silently dropped from every release build by the
  resource shrinker, because no code referenced it. It is now shown in the app, which both
  fixes the attribution and keeps the shrinker from removing it.

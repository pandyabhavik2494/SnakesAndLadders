# Snakes & Ladders

The board game for two, built for the Galaxy Z Fold 8 (Android 11 and later): pass the phone between
two players, or play against the phone, which rolls for itself.

- Five complete looks (Jade Palace, Midnight Gold, Rosewood, Sakura, Noir), four piece shapes, seven
  piece colours per player and five dice. Everything is drawn in code: no images.
- Three boards (Classic, Gentle, Wild) and house rules: exact roll, bounce back or any roll to finish,
  and whether a six rolls again.
- Every roll is felt and heard: the dice rattle in the hand and land on felt, each square is a soft
  tap, ladders climb on rising marimba notes and snakes slide down with a hiss. Sounds and haptics each
  have their own switch.
- Lays itself out for both screens: stacked on the cover screen, board beside the controls on the inner
  screen. Close the app and come back: the game carries on where you left it.
- No accounts, no network, no ads, nothing leaves the phone.

## Layout

- `core/`: the rules, in plain Kotlin with unit tests: board layouts, turns (hops, ladders, snakes,
  finishing rules, rolling again), per-player stats and the saved-game format.
- `app/`: the Android app: Compose UI, the board drawn on a canvas, DataStore for the saved game and
  settings. Fonts (Playfair Display, Manrope; SIL Open Font License) are bundled in `res/font`.
- `ci/sounds.py`: synthesises every sound in `app/src/main/res/raw` (`python3 ci/sounds.py app/src/main/res/raw`).
- `app/src/androidTest/`: screen tests that play turns for two players and against the phone, switch
  themes and check the game resumes, saving screenshots as they go.

## How a build happens

Every push to `main` (or a manual run of the workflow):

1. Runs the unit tests and builds the app.
2. Publishes a GitHub release, `build-N`, with `SnakesAndLadders-ci.apk` (signed with the runner's
   throwaway debug key, so not the one to install).
3. Starts two emulators at the Fold's cover-screen size (1080×2520, large text) and inner-screen size
   (1968×2184), installs the APK, runs the screen tests, and publishes the screenshots as
   `build-N-cover-screen` and `build-N-inner-screen` releases.

The file to install, `SnakesAndLadders.apk`, is the CI file re-signed with the game's own key (kept
outside this repository by the owner) and attached to the same `build-N` release. Every build signed
with that key installs straight over the previous one and keeps the game in progress.

## Build it yourself

Open the folder in Android Studio and run the `app` configuration. To run the unit tests from a
terminal: `./gradlew :core:test`.

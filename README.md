# Vibe Player

Vibe Player is an Android media player project developed by Shan Palia.

## This build
- Refreshed compact video-library UI
- White/light home interface with cleaner cards
- Automatic landscape mode for landscape videos (resolution-aware; unknown video sizes default to landscape)
- Android status/navigation bars remain visible in the player
- MediaSessionService starts when playback begins so playback can continue after Back/Home
- Android media notification with play/pause controls and a tap-to-return action
- Notification permission requested on Android 13+
- Playback metadata is exposed to the system media notification
- Gradle 9.3.1 / Codemagic signed release setup

## Build
Use Android Studio or Codemagic. The release workflow uses the `paliaapk-release` signing identity configured in Codemagic.

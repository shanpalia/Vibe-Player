# Vibe Player

Vibe Player — Android video player with a clean white/purple interface.

- Bottom navigation includes Home, Folders, Recent, Favorites and Settings.
- App content respects Android system status/navigation bars so controls do not sit underneath phone system UI.
- Launcher and in-app branding use the Vibe Player icon.
- Splash screen includes “Developer by ShanPalia”.
- Release build is configured for Codemagic with the `paliaapk-release` signing reference.


## Phone safe-area fix
Normal screens use top status-bar insets so the header never overlaps Android system icons. Player remains separately managed for landscape playback.

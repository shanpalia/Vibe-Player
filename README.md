# Vibe Player

**Vibe Player — Developed by Shan Palia**

Native Android video player built with Kotlin, Jetpack Compose and AndroidX Media3.

## Codemagic signed APK

1. Push this project to GitHub with `codemagic.yaml` at repository root.
2. In Codemagic, add the GitHub repository as a native Android project.
3. Upload your Android release keystore under **Code signing identities → Android keystores** and use the reference name `vibe_player_keystore` (or change the YAML reference to your chosen name).
4. Codemagic injects `CM_KEYSTORE_PATH`, `CM_KEYSTORE_PASSWORD`, `CM_KEY_ALIAS`, and `CM_KEY_PASSWORD` during the build.
5. Run the **Vibe Player Android Signed APK** workflow.

**Never commit the keystore or passwords to GitHub. Keep the original keystore safe for future updates.**

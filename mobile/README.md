# Dozy Android prototype

This standalone Android app is a small Jetpack Compose implementation of `design/home.html`.

Open `mobile/` in Android Studio, or build it from the repository root with:

```sh
./mobile/gradlew -p mobile assembleDebug
```

The screen uses local images from `design/images` and the bundled Nunito Sans, Playfair Display, and JetBrains Mono variable fonts. Their SIL Open Font License files are included under `app/src/main/res/raw/licenses`.

Shared app colors, typography, shapes, and the 8 dp spacing scale live in `app/src/main/java/com/dozy/mobile/ui/theme`.

## Shared debug signing

Every debug build uses the checked-in `signing/debug.keystore`, resolved from the
mobile Gradle project root regardless of the shell's working directory. Local,
Orbit/Codex, and future CI builds therefore share one signing identity without
creating a user debug keystore or changing `ANDROID_USER_HOME` or `GRADLE_USER_HOME`.

This is a **public test key, never a production signing key**. Its alias is
`androiddebugkey`; both the store and key passwords are `android`. Keep this file
stable: regenerating it changes the certificate. Release signing is unchanged;
production keys and passwords must stay outside Git and use CI secrets when
release distribution is introduced.

From the repository root, run tests and build the APK:

```sh
bash test-all
./mobile/gradlew -p mobile :app:assembleDebug
```

The APK is `mobile/app/build/outputs/apk/debug/app-debug.apk`. Inspect the key and
APK certificates with the JDK's `keytool` and Android SDK's `apksigner` (adjust
the SDK path/build-tools version for your installation):

```sh
keytool -list -v -keystore mobile/signing/debug.keystore -alias androiddebugkey -storepass android
"$ANDROID_HOME/build-tools/36.0.0/apksigner" verify --print-certs mobile/app/build/outputs/apk/debug/app-debug.apk
```

Both SHA-256 fingerprints must match (ignoring colons and letter case):
`1C:90:8D:4E:64:86:DB:C3:68:F7:56:4B:57:6D:B4:1B:DA:CA:17:D7:CB:79:75:B6:39:F1:A5:67:D5:40:37:96`.
A fresh checkout needs only the normal JDK/Android SDK setup; no existing user
debug keystore is needed.

A future Firebase App Distribution APK job should check out this file, run the
same `:app:assembleDebug` task, and distribute the resulting debug APK. It should
use this existing debug signing configuration without generating or substituting
a key. No deployment workflow or Firebase distribution is configured here.

Updating an installed app also requires the same application ID (currently
`com.dozy.mobile`) and an appropriate `versionCode` (increment it for distributed
updates). An APK already installed with a different signing key may require a
one-time uninstall before installing this build; uninstalling removes its local data.

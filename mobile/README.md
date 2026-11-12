# Dozy Android prototype

This standalone Android app is a small Jetpack Compose implementation of `design/home.html`.

Open `mobile/` in Android Studio, or build it from the repository root with:

```sh
./mobile/gradlew -p mobile --no-daemon :app:assembleDebug
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
./mobile/gradlew -p mobile --no-daemon :app:assembleDebug
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

## KMP source ownership and validation

Versions are pinned in `gradle/libs.versions.toml`. Builds use JDK 21, Gradle
8.14.3 and AGP 8.13.2; Android emits JVM 17 bytecode (minSdk 26,
compileSdk 36, targetSdk 35). Preserve inherited `GRADLE_OPTS` and use
`--no-daemon` in task clones.

- `:shared/commonMain`: existing character, place and story domain models,
  validated inputs, errors, repository contracts and use cases, plus ApiConfig.
  Packages and constructors are preserved. IDs remain unchanged. ID generators
  and StoryCompletionNotifier are injected contracts with Android implementations
  in `:app`; no iOS notification implementation is supplied.
- `:foundation/commonMain`: the existing cancellation-safe runCatchingSuspend.
  Shared domain consumes this helper through the foundation dependency.
- `:foundation/androidMain`: Compose, MVI, UiText and state persistence until KMP3.
- `:app`: Android data/API/Room, DI, WorkManager, notifications and presentation.
  KMP2 can move data adapters without duplicating the shared domain. KMP4 owns
  shared UI/image loading; the existing fake avatar behavior remains unchanged.
- `commonTest` in shared/foundation: portable domain/validation/coroutine tests.
  Shared `commonTest/kotlin/fixtures` is also a test source directory of `:app`,
  so Android tests reuse the same domain fakes. The separate FakePlaceDao stays
  in app tests. Mixed data/presentation integration tests remain Android-hosted;
  foundation SavedStateHandle tests remain in `androidHostTest`.

From the repository root:

```sh
bash test-all
./mobile/gradlew -p mobile --no-daemon ktlintFormat
./mobile/gradlew -p mobile --no-daemon :app:assembleDebug :app:assembleRelease
./mobile/gradlew -p mobile --no-daemon :foundation:compileAndroidMain :shared:compileAndroidMain
./mobile/gradlew -p mobile --no-daemon :foundation:compileKotlinIosArm64 :foundation:compileKotlinIosSimulatorArm64 :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64
./mobile/gradlew -p mobile --no-daemon :foundation:compileTestKotlinIosArm64 :foundation:compileTestKotlinIosSimulatorArm64 :shared:compileTestKotlinIosArm64 :shared:compileTestKotlinIosSimulatorArm64
./mobile/gradlew -p mobile --no-daemon :shared:linkDebugFrameworkIosArm64 :shared:linkDebugFrameworkIosSimulatorArm64
```

`test-all` runs backend tests, all app unit-test variants, and the foundation/shared
Android host tests (including common tests). For mobile only, use
`:app:test :foundation:testAndroidHostTest :shared:testAndroidHostTest`.
Native compilation requires macOS with full Xcode 26.0 and its iOS SDK selected
through xcode-select, plus Kotlin/Native toolchain download access. Both libraries
set iOS deployment target 15.0. Shared publishes a static `DozyShared` framework;
there is no Xcode host, signing, simulator launch or iOS app entry point.

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
  Real repositories, DTOs, mapping, Ktor APIs, Room DAOs/entities/database and data
  Koin modules also live here. Repositories directly use DAOs and APIs.
  Packages and constructors are preserved. IDs remain unchanged. ID generators
  and StoryCompletionNotifier are injected contracts with Android implementations
  in `:app`; iOS uses an explicitly unsupported notifier returning false.
- `:foundation/commonMain`: cancellation-safe runCatchingSuspend, Compose theme/spacing,
  MVI, UiText with Compose StringResource and resolver, and typed JSON state persistence.
  Lifecycle 2.10.0 and SavedState 1.4.0 provide supported multiplatform APIs.
- `:shared/androidMain`: the database builder keeps Android's existing `dozy.db`
  path. `:shared/iosMain`: Documents-directory Room builder and Darwin engine DI.
  Both platforms use bundled SQLite, generated Room constructors, and the same
  HTTP JSON configuration and 5000 ms request timeout. Koin closes the singleton
  database/client, and the client closes its owned engine.
- `:shared/commonMain`: all feature ViewModels, state, actions/effects, presentation
  mapping and feature Koin modules. `composeResources` owns screen strings/plurals,
  artwork and variable fonts; exported accessors use `com.dozy.mobile.resources`.
  Android screens resolve these shared resources directly without duplicating the catalog.
- `:app`: Android navigation/screens, localized bundled place catalog, ID generators,
  WorkManager and notifications. Feature modules include shared data modules.
  The null-base-URL fake generator preserves `bundled-avatar://` references;
  Android presentation resolves them to shared DrawableResource values. KMP4 owns UI.
  App res retains manifest and notification-channel/title strings; notification body
  uses the shared resource. Typography still uses all original variable font weights.
- `shared/schemas`: unchanged database package, version 8, identity hash and all
  eight schemas; automatic migrations 1 through 8 are preserved. KSP runs per
  Android/iOS target. Android generated sources are wired explicitly for the
  pinned KSP/AGP versions, including on clean builds.
- iOS hosts can include `iosDataModule(ApiConfig(...), BundledPlaces(...))` and
  supply `CharacterIdGenerator`, `PlaceIdGenerator`, `GenerationKeyGenerator`
  when resolving write use cases. Include `iosPresentationModule` for the shared
  feature graph and the unsupported notification adapter. No iOS scheduler exists.
  Pass a `SavedStateHandle` containing route arguments with `parametersOf(handle)`
  when creating a route ViewModel. Retain it with its navigation owner; the default
  factory creates fresh handles for argument-free forms. SavedStatePersistence uses
  that in-memory handle on iOS with no cross-relaunch or process-death guarantee.
  Android keeps owner-provided SavedStateHandle and the same STATE key/JSON format.
- `commonTest` in shared/foundation: portable domain/validation/coroutine tests.
  Repository, mapping, fake-avatar, MockEngine, pure ViewModel/MVI and feature-graph
  lifetime/argument tests also run from commonTest.
  Shared `commonTest/kotlin/fixtures` is also a test source directory of `:app`,
  so Android tests reuse domain and DAO fakes. Real Room migration/reopen and
  mixed data/presentation integration tests remain Android-hosted;
  foundation SavedStateHandle tests remain in `androidHostTest`. Mixed form/reader
  recreation tests stay Android-hosted and reuse common fixtures.
  SharedResourcesTest checks Android loading, formatting, plurals and XML escapes.

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

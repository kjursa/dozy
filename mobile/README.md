# Dozy Android prototype

This standalone Android app is a small Jetpack Compose implementation of `design/home.html`.

Open `mobile/` in Android Studio, or build it from the repository root with:

```sh
./mobile/gradlew -p mobile assembleDebug
```

The screen uses local images from `design/images` and the bundled Nunito Sans, Playfair Display, and JetBrains Mono variable fonts. Their SIL Open Font License files are included under `app/src/main/res/raw/licenses`.

Shared app colors, typography, shapes, and the 8 dp spacing scale live in `app/src/main/java/com/dozy/mobile/ui/theme`.

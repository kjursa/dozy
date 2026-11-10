# Foundation guidance

- Keep this module independent of Dozy app screens, assets, navigation, and branding.
- Expose Material 3 configuration through parameters; avoid hard-coded app palettes.
- Prefer small stateless composables. Put reusable state handling in focused types.
- Keep the public API small and document public components and MVI contracts.
- Add an API here only when it can be reused by another app without Dozy-specific behavior.
- Organize APIs under `com.kjursa.foundation.coroutines`, `.presentation.text`, and `.compose` packages; keep coroutine helpers independent of Compose and Android imports.
- This is currently one Android library module. Keep platform-neutral logic ready to move to KMP common source sets when the module is migrated.

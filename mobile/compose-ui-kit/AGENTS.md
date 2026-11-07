# Compose UI Kit guidance

- Keep this module independent of Dozy app screens, assets, navigation, and branding.
- Expose Material 3 configuration through parameters; avoid hard-coded app palettes.
- Prefer small stateless composables. Put reusable state handling in focused types.
- Keep the public API small and document public components and MVI contracts.
- Add a component here only when it can be reused by another app without Dozy-specific behavior.

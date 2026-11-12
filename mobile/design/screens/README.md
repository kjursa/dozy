# Dozy screen references

Open `index.html` in a browser to select a screen and its variants. This directory is the visual reference for the mobile app. Use the rendered HTML for layout, typography, colors, and components; use `../../SPEC.MD` for behavior, states, actions, and navigation.

## Screen catalog

| File | Variants / actions |
| --- | --- |
| `home.html` | Saved content, first visit, generating, ready, and failed generation. |
| `stories.html` | Saved stories and an empty library. |
| `characters.html` | Character list and an empty list. |
| `character-form.html` | Empty form, description without an avatar, avatar loading/ready/error, and editing. |
| `character.html` | Character details, editing, new story, and deletion confirmation. |
| `places.html` | Custom places first, then predefined places; no custom places. |
| `place-form.html` | Empty form, example content, and editing. |
| `create-story.html` | Manual idea, automatic mode, reused cast, and no characters. |
| `generating.html` | Text and cover generation progress. |
| `reader.html` | Cover, story text, cover enlargement, text size, evening mode, and a new adventure with the same cast. |

## Preview boundaries

- The catalog, sidebar variant selectors, and simulation controls are review tools, not application screens or controls. On narrow windows, the review panel appears above the mockup.
- Query parameters select variants, for example `home.html?state=empty` or `character-form.html?avatar=ready`. Inspect each relevant rendered variant rather than inferring the design from example artwork or HTML source alone.
- The prototype uses example content and shared assets from `../images/`, `../icons/`, and `../brand.css`. Keep these assets available when opening the HTML files.
- No real generation, notifications, or durable database operations occur. Some form values may be stored in browser-tab session storage.
- Google Fonts are optional external dependencies with fallback fonts. No build step is required.
- For interaction details where prototype simulations or annotations differ, follow `../../SPEC.MD` and the task requirements. In particular, changing character fields does not require regenerating an existing avatar before saving.

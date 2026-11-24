# Dozy screen references

Open `index.html` in a browser to select a screen and its variants. This directory is the visual reference for the mobile app. Use the rendered HTML for layout, typography, colors, and components; use `../../SPEC.MD` for behavior, states, actions, and navigation.

## Screen catalog

| File | Variants / actions |
| --- | --- |
| `login-tutorial.html` | One-time introduction with three horizontally scrolling cards. |
| `login-welcome.html` | Welcome, email sign-in choice, and continue as guest. |
| `login-email.html` | Email entry, passwordless account creation, and terms notice. |
| `login-code.html` | Nine-digit code in three groups, custom keypad, clear/delete, and automatic submission. |
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
- The mockups load the same Nunito Sans and Playfair Display font files as Compose from the shared mobile resources. Open them from the repository so those files are available. No build step is required.
- For interaction details where prototype simulations or annotations differ, follow `../../SPEC.MD` and the task requirements. In particular, changing character fields does not require regenerating an existing avatar before saving.

## Shared UI

Open [the component reference](components.html) for the baseline screen styles.
Use `tokens.css` for shared colors, typography, and measurements. Use the
existing classes in `components.css` for common controls and surfaces. Keep
screen layout and state-specific rules with the screen.

| HTML pattern | Compose counterpart |
| --- | --- |
| Page title, section title, and card title | `MaterialTheme.typography.headlineLarge`, `headlineSmall`, and `titleLarge` |
| `.intro`, `.muted`, `.eyebrow`, and `.footnote` | `bodyLarge`, `bodyMedium`, `labelSmall`, and `DozyFootnote` |
| `.button.primary` and `.button.secondary` | `DozyActionButton` and Material `FilledTonalButton` |
| `.form-label` with `.form-field` or `textarea` | `DozyFormField` with normal, focused, error, and disabled states |
| `.form-card` and `.book-row` | Material `Surface` or `Card` with `DozyLayout.cardRadius` |
| `.create-banner h2`, `.story-copy`, and `.ending h2` | `DozyTextStyles.bannerTitle`, `readerBody`, and `readerEnding` |

Start with a 24 px screen gutter, 32 px between sections, 16 px between
related components, and 8 px between a label and its control. In Compose
these values are defined in dp in `ui.common.DozyLayout`. Use sp for text.
Keep form labels and story prose start-aligned. Center the text itself in
footnotes, empty states, and reader endings.

For a new screen, assemble the HTML from these patterns, then use the same
roles in Compose. If a style belongs to more than one screen, add it to the
shared stylesheet and the corresponding Compose theme or component. Compare
focused and error states as well as the default state, and check at a narrow
viewport and with larger Android text.

The shared app toolbars follow the selected Home, My Stories, or Characters
bottom-tab palette. A vertical gradient spans the status bar and toolbar, with
a crisp edge above the light content area. On the three main tabs, content
scrolls between the toolbar and bottom navigation.

## Sign-in prototype

Start at `login-tutorial.html` for the first visit, or `login-welcome.html` for subsequent visits. Email sign-in proceeds through `login-email.html` to `login-code.html`. These screens use `login.css` alongside the shared styles. Authentication and account creation are simulated; no email is sent.

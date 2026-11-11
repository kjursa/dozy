# Dozy — mobile application specification

## 1. What the app does

Dozy creates personalized bedtime stories. The app is primarily operated by a parent, who defines characters, prepares an adventure, and reads the finished story to their child. Heroes can be the parent's children, animals, or imaginary characters. Places help set the story in a familiar or fantastical world.

The user can provide an idea or choose “Surprise me” without typing anything. Generation produces text and one cover illustration. The same illustration serves as the library thumbnail and the opening image in the reader. No additional illustrations are generated within the story.

Each story is independent and saved automatically. It cannot be edited. “New adventure with these heroes” keeps the cast but does not continue the previous story's events. There are no numbered parts or collections of successive chapters in one plot.

### Document status

This document describes intended behavior based on the agreed requirements and current designs. It does not mean that every feature has already been implemented in the app or prototype.

Current mockups: [screen index](design/screens/index.html). Their English labels correspond to the names used here. Older proposals are not a source of requirements where they conflict with this document.

The HTML variants are described with each screen. Shared states and additional cases needed in the working app are described separately. Unresolved decisions are listed at the end of the document.

## 2. Core concepts and rules

| Concept | Meaning |
| --- | --- |
| Character | Name, type, personality, appearance, and an avatar generated from the description. Can be reused across stories. |
| Place / Places | Name, short description, and an optional description of who might be encountered there. A setting rather than a plot. |
| Predefined place | One of the app's suggestions available from the start. |
| Custom place | A place created by the user, which they can also edit. |
| Story | Saved text, title, cover, and information about the heroes used. |
| Generation | A job that prepares the text and cover independently of the open screen. |

- A place's inhabitants inspire the generator. They do not replace explicitly selected main heroes and do not require separate saved character records.
- Places is one collection: custom places first, then predefined ones. There are no headings separating them by source.
- Only custom places have “Edit this place”. Generated place illustrations are not required; current thumbnails are example assets.
- Places use a compact layout: a small square thumbnail, name, and description. Mockup thumbnails are 48 × 48 px.
- Editing character or place data does not rewrite the text or cover of previously saved stories.
- A story is ready when both its text and cover are complete. This is the current proposal assumption, pending confirmation.

### Local data storage

The app uses a single `DozyDatabase` in the `dozy.db` file. It contains character and place tables; DAOs and entities remain in their owning features. Data from the previous `characters.db` file is neither migrated nor copied — the new database may start empty.

## 3. Navigation and shared behavior

### Main sections

Bottom navigation contains **Home**, **My Stories**, and **Characters**. Places is accessible from Home and the story form, without a separate bottom tab. Switching tabs preserves the previous section's state and scroll position.

Other screens open above the main navigation. “Back” and system back return to the entry screen. Dismissing an overlay closes it while retaining the underlying screen.

### Forms and return paths

- Opening place selection or character creation from the story form preserves the selected cast, length, mode, and entered wishes.
- Saving a supporting object returns to the story form if the operation started there. A new character is selected; a new place becomes the selected setting.
- Going back without saving creates no object and preserves the previous selection in the story form. Persistence rules for unsaved forms need clarification.
- Validation errors appear beside the corresponding fields. An operation failure does not erase entered data.
- While saving or submitting a generation request, the action is disabled and shows progress to prevent duplicate operations from repeated taps.

### Shared states

The following states supplement the mockup variants and apply to screens using data or network operations:

| State | Behavior and actions |
| --- | --- |
| Initial loading | An indicator or placeholders; no brief “no data” message. Navigation remains available. |
| Refreshing known data | Preserve existing content instead of replacing it with an empty screen. |
| Empty | A message and an appropriate starting action, described for each screen. |
| Loading failure | A clear message and “Try again”; known data remains visible when available. |
| Offline | Saved, locally available data can be viewed. Operations requiring the network show an error and retry without losing the form. |
| Object unavailable | Explain that the object cannot be opened and provide a return to the collection instead of empty details. |

Not every combination needs a separate full-screen design. For example, Home can show saved stories and a new generation failure at the same time.

## 4. Home

Mockup: [home.html](design/screens/home.html).

### Content

A greeting, story creation action, recent stories, characters, and a **Places** section at the bottom with “See all”. Custom places precede predefined ones. A job status card appears above the content when a job is in progress or needs attention.

### Variants

| Variant | Content / behavior |
| --- | --- |
| Populated | Recent stories, heroes, and compact Places. |
| First launch | No characters or stories. The main prompt encourages creating the first character and briefly explains the flow. Predefined Places remain available. |
| Characters exist, no stories | Characters and places remain visible; the story section encourages creating the first story. An intended case without a separate HTML variant. |
| Generating | A card explains that a new story is being prepared; the user can open progress and continue using the app. |
| Story ready | A ready card with an action to open that specific story. The story is also in the library. |
| Generation failure | A message and retry with the existing configuration. Other data remains available. |
| Loading / data failure | Shared rules from section 3. |

### Actions and destinations

| Action | Destination |
| --- | --- |
| Create a story | Story creation; the no-heroes variant if there are no characters. |
| Story / Read your story | Reader for the selected story. |
| See all for stories | My Stories. |
| Character | Character details. |
| See all for characters | Characters. |
| Create the first character | Empty character form. |
| See all for Places | Place list. |
| Place card | Story creation with the selected setting. |
| View progress | Generation. |
| Try again on a job card | Retry the job; once accepted, return to the generating state. |
| Bottom tab | Corresponding main section. |

The mockup bell is a shortcut for previewing completion. A separate notification center has not been designed.

## 5. My Stories — story library

Mockup: [stories.html](design/screens/stories.html).

### Content and variants

- **Populated:** saved stories with covers, titles, and estimated reading times. Each item is an independent story.
- **Empty library:** explain that saved stories will appear here and offer “Create your first story”. The HTML variant assumes characters already exist; otherwise, the story form leads to hero creation.
- **Loading / failure:** shared rules apply; a failure is not an empty library.

### Actions and destinations

- Select a story → its reader.
- “＋” or create the first story → story form.
- “Back” → Home; bottom tabs → selected section.

There is no story editing, view of successive parts, or agreed story deletion action.

## 6. Characters — character list

Mockup: [characters.html](design/screens/characters.html).

### Content and variants

- **Populated:** avatars, names, and short descriptions; a card to add a new character.
- **Empty list:** encourage describing the first hero and explain that the avatar is generated from the description.
- **Loading:** show progress until the first local collection is read; do not flash empty copy.
- **Refreshing:** keep known cards visible with progress and disable repeated refreshes.
- **Failure:** keep known cards and offer Retry. Do not show the empty-state message for a failed initial refresh.
- **Offline / local-only:** show a notice and all locally saved characters, including pending saves; an empty local collection offers the creation CTA.
- Refresh uses the complete collection proposal in `CHARACTER_DATA.md`; pending local changes take precedence over the remote snapshot.

### Actions and destinations

- Character → its details.
- “＋”, “Add a character”, or the empty-state action → empty character form.
- “Back” → Home; bottom tabs → selected section.
- After an object is saved or deleted, the list shows current data. Deleting one of several characters must not put the entire collection into an empty state.

## 7. Creating and editing a character

Mockup: [character-form.html](design/screens/character-form.html).

### Fields and hints

| Field | Meaning |
| --- | --- |
| Type | Child, Animal, Fantasy, or Other. |
| Name | A given name or storybook nickname. |
| Personality | The hero's traits, interests, and behavior. |
| Appearance | A description from which the character and avatar can be imagined. |

The empty form includes a hint that the user's child can become a story hero. Placeholders suggest interests, temperament, and distinctive appearance. A photo of the child is not required; the avatar is generated from the description.

The current name restriction is a nonempty value of up to 50 characters (`String.length`). The HTML also requires personality and appearance; detailed limits for these descriptions have not been finalized.

### Variants

| Variant | Content / available actions |
| --- | --- |
| New character, empty form | An inspirational hint, placeholders, and space for a future avatar. |
| Input entered, no avatar | Descriptions are ready; generation can be started. |
| Validation failure | Field errors; correct the input and retry without starting generation for an invalid form. |
| Avatar in progress | Block form fields and other form actions. Replace the Generate avatar button content with a circular progress indicator; prevent duplicate generation and saving. Back/system back remains available to leave without saving. |
| Avatar ready | Preview, regenerate, and save. |
| Avatar failure | Unblock the form, retain descriptions, show an error, and allow retry. Do not save the character. Without a successfully generated avatar, Save remains disabled. |
| Fields changed after preview | Keep the current avatar and allow saving valid current input with that avatar. No field change automatically generates or invalidates the avatar. |
| Editing | Existing data and avatar, with editing and saving available. No inspirational hint for a new empty character. |
| Saving / save failure | Progress replaces the active save action; on failure, data and avatar remain and retry is available. |

A successfully generated avatar is required before a new character can be saved. Entering or changing fields never triggers generation. Only explicitly pressing Generate avatar (or Regenerate avatar) starts it, using the current input. Generation success updates the preview and enables Save; it never saves the character automatically. After success, changing any fields keeps the current avatar and does not disable Save as long as the input is valid. Saving uses the current field values and the current avatar, even if that avatar was generated from earlier descriptions. While generation is running, Save is disabled.

### Actions and destinations

- “Generate avatar” / regenerate → validate input → block the form and show circular progress in the button → ready avatar or failure; unblock the form when the attempt finishes. This action does not create or update a saved character.
- Save a new character opened from Home/list → new character details.
- Save a character added during story creation → the previous story form with the new character selected and input preserved.
- Save edits → character details with updated data.
- “Back” → entry screen without saving changes, including when generation is running. Stop observing the abandoned attempt and ignore late responses: leaving must not create a character, update an existing character, or trigger a delayed save when generation finishes. Any existing saved character remains unchanged. Unlike story generation, this flow does not save a result in the background.

The mobile implementation restores raw form input and the last successful avatar
reference through StatePersistence. Active jobs, loading, and errors are not
restored; generation must be explicitly retried. An absent API configuration
shows an unavailable error and cannot produce an avatar. Existing previews can
still be saved locally. The proposed HTTP contract, retry rules, asset lifetime,
and database migration are documented in [CHARACTER_DATA.md](CHARACTER_DATA.md).

## 8. Character details

Mockup: [character.html](design/screens/character.html).

### Variants

- **Available character:** avatar, name, type, personality, and appearance.
- **Deletion confirmation:** a dialog explaining the consequences, with delete and keep actions.
- **Deleting / deletion failure:** prevent repeated requests; on failure, retain the character and allow retry.
- **Loading / object unavailable:** shared rules apply.

### Actions and destinations

- “Create a story with…” → story form with this character selected.
- “Edit character” or the edit icon → edit form.
- “Delete character” → confirmation dialog.
- Confirm → delete; on success, open the character list, empty only if no other characters remain.
- Cancel / dismiss dialog → the same details.
- “Back” → the screen from which details were opened.

Current assumption: a deleted character is unavailable for new stories, but saved stories remain readable. Reusing a cast containing a deleted character requires a decision.

## 9. Places — browsing and selecting places

Mockup: [places.html](design/screens/places.html).

### Content

One list: custom places first, predefined ones afterwards, without separating headings. Small square thumbnails, names, short descriptions, and optional inhabitant information. Only custom places have “Edit this place”. “Add new” and a random place option are also available.

### Variants

- **With custom places:** custom places first, followed by predefined ones.
- **No custom places:** predefined places remain visible, with a short invitation to create a custom place. This is not an empty screen for the entire collection.
- **No inhabitant description:** omit that part of the card without an empty label.
- **Loading / collection failure:** shared rules apply; already available predefined or saved places remain visible.
- **No places available at all:** a fallback if even predefined suggestions cannot be obtained; offer retry, custom creation, or “Surprise me”. No separate mockup.

### Actions and destinations

| Action | Destination |
| --- | --- |
| Select a place during story creation | Return to that form with the setting selected and other input preserved. |
| Select a place after entering from Home | New story form with the setting selected. |
| Surprise me with a place | Story form with a random setting; does not force automatic mode for the entire plot. |
| Add new / Create a place | Empty place form, preserving entry context. |
| Edit this place | Edit the selected custom place. |
| Back | Story form or Home, depending on the entry context. Does not change the previous selection. |

Predefined places cannot be edited directly. Copying a predefined place into a custom version has not been designed yet.

### Mobile implementation contract

The mobile collection uses bundled, offline predefined suggestions plus durable
local custom places and a cached remote snapshot. Within each source, names and
IDs determine order. Selection changes only the persisted story draft setting;
inhabitants never select main characters. The add/edit form saves locally first and queues unacknowledged uploads. The
proposed GET/PUT contract, identifiers, ordering, migration, retry, and
synchronization limits are documented in [PLACE_DATA.md](PLACE_DATA.md).

## 10. Creating and editing a place

Mockup: [place-form.html](design/screens/place-form.html).

### Fields

| Field | Meaning |
| --- | --- |
| Name | For example, “Grandma's garden”. |
| Short description | Appearance, atmosphere, and distinctive features of the place. |
| Who might you meet? | Optional animals, inhabitants, creatures, or other inspiration. |

A place image is not required. Name and short description are required. The current mockup limits the name to 80 characters and each description to 500; these are provisional limits pending confirmation.

### Variants

- **New, empty:** placeholders and an explanation of optional inhabitants.
- **Populated:** input awaiting save; the inhabitant description can remain empty.
- **Editing:** existing custom place data. Loading blocks fields and Save; Back remains available.
- **Unavailable / forbidden:** a missing record and a predefined or non-editable record show distinct messages and cannot be saved. Load failures offer retry.
- **Validation failure:** messages beside fields without losing other input.
- **Saving / save failure:** progress and prevention of repeated requests; retry after failure.

### Actions and destinations

- “Save & use this place” while preparing a story → save and return to the story form with this place selected.
- The same action after entering from Home/list → save and open a new story form with the place selected, according to the current proposal.
- Editing saves changes to the place; earlier stories remain unchanged. The current action also leads to the story form with that place.
- “Back” → place list without saving changes, preserving the context of the earlier story form.

Place deletion has neither a design nor an agreed action yet.

## 11. Creating a story / new adventure

Mockup: [create-story.html](design/screens/create-story.html).

### Input

- At least one saved character, with multiple selection available.
- Own-idea mode or “Surprise me”.
- In own-idea mode: a selected place or random setting, an optional adventure description, and an optional ending.
- Length: short, about 3–5 minutes, or longer, about 8–12 minutes. These are estimated reading times.

A place describes the setting. The adventure description defines events. These concepts are not mixed into one theme list. Own-idea mode does not require text input if the user wants to rely only on characters and a place.

### Variants

| Variant | Behavior |
| --- | --- |
| Standard form | Select cast, setting, length, and optional wishes. |
| Surprise me | The generator invents the place, events, and ending. The user selects only characters and length. Manual descriptions/setting are hidden and are not submitted in this mode. |
| New adventure from the reader | The previous story's cast is selected; a new plot without previous events, a part number, or a requirement to keep the place. |
| Entry from character details | That character is selected; others can be added. |
| Entry from a place | The setting is selected; the user chooses heroes and length. |
| No characters | Generation is disabled; a message and an action to create the first hero. |
| Characters exist, none selected | A cast error after attempting generation; the form remains. |
| Return after adding a character/place | Previous input preserved, new object selected. |
| Submitting generation | An indicator and prevention of another submission. |
| Submission failure | Form with input preserved and retry available. |

Switching modes may retain manual fields as draft input so the user can return to them. Hidden values must not affect an automatically generated story.

### Actions and destinations

- Select characters or change mode, descriptions, or length → remain in the form.
- Add a hero → create character → save returns with the new character selected.
- Choose a place → Places → selection returns to this form.
- “Generate my story” → validation → job submission → generation screen once the job is accepted.
- “Back” → entry screen, such as Home, library, reader, character details, or Places.

## 12. Story generation

Mockup: [generating.html](design/screens/generating.html).

### Content and variants

| State | Behavior |
| --- | --- |
| Job accepted / queued | Confirm that generation has started; do not imply a nonexistent completion percentage. |
| Creating text | An indicator and explanation of the current stage. |
| Creating the cover | Explain that one cover image is being prepared. |
| Ready | The story is saved and can be opened. Home shows readiness and the library contains the story. |
| Job failure | A message and retry with the configuration preserved. |
| Current status unavailable, for example offline | Explain that progress cannot be retrieved. Lack of connectivity is not evidence that the job itself failed. |

### Actions and destinations

- “Back to Home” → Home with job status.
- System back → leave progress without canceling generation. Do not return to an active submission in the form that could accidentally trigger the same operation again.
- Ready story / open action → its reader. How completion is presented while remaining on the screen needs a decision; automatic navigation is not assumed without one.
- Retry → a new execution with preserved parameters, without retyping the idea.
- Close the app → the job continues. On return, restore its actual status.

On completion, the user receives a notification if they have granted system permission. Tapping it opens the specific ready story, even if the app is not running. Denied permission does not block generation; the result remains available on Home and in the library.

## 13. Story reader

Mockup: [reader.html](design/screens/reader.html).

### Content

Title, heroes, estimated reading time, one opening cover, and scrollable text. No illustrations between paragraphs. At the end, offer a new adventure with the same characters and a return to the library.

### Variants and overlays

| Variant | Behavior / exit |
| --- | --- |
| Standard reading | Light background, text, and cover. |
| Evening mode | A darker, calm palette; the same content and actions. |
| Aa settings | An overlay with small, medium, and large text and an evening-mode switch. Closing returns to the same part of the story. |
| Enlarged cover | Full-screen preview; close/back returns to the reader. |
| End of story | “New adventure with these heroes” and return to the library; this is not a plot continuation. |
| Loading / story unavailable | Shared rules apply; on failure, retry or return to the library. |

### Actions and destinations

- Tap cover → enlarged preview.
- Aa → settings; changing size or theme updates the reader.
- “New adventure with these heroes” → story form with that cast and no previous plot.
- “Back to your stories” → library.
- “Back” → entry screen, such as Home or library. After entry from a notification without an existing navigation stack → Home.

Remembering reading position and reader preferences is a proposed convenience; persistence details have not been settled yet.

## 14. Key flows

### First story

Empty Home → create character → description → generate avatar → save → character details → create story → choose a place or “Surprise me” → generate → read.

### Story without typing an idea

Home / library → create story → select characters → “Surprise me” → length → generate → ready story. A random setting does not require creating and saving a separate user-owned place.

### Adding a hero while preparing a story

Story form → add character → generate avatar and save → the same form with the new hero. Other choices and entered text remain.

### Custom place during story creation

Story form → Places → create place → name, description, and optional inhabitants → save → story form with that place. The place is subsequently available at the start of the shared collection.

### Returning after background generation

Submit → generation → Home or close app → completion → notification / Home card / library → reader for the specific story.

### New adventure for favorite heroes

Reader → “New adventure with these heroes” → new form with the cast → new idea or automatic mode → generation → independent saved story. The previous story remains unchanged.

## 15. Prototype boundaries and open decisions

### What is not part of the app

- `index.html` is a design catalog, not a product screen.
- Sidebar variant selectors and simulation controls are review tools outside the app mockup.
- The HTML files do not perform real generation, notifications, or durable collection persistence.
- Example artwork does not imply a requirement to generate illustrations for every place.
- Simplified links, such as always returning to Home or showing an empty list after deletion, do not replace the specified return-to-origin behavior and actual list contents.

### Decisions required before implementing the relevant features

1. Final limits for character descriptions, place descriptions, and story wishes. HTML limits are provisional except for the agreed character name limit of 50 characters.
2. Whether and how stories and custom places can be deleted; copying predefined places into custom versions.
3. Reusing a cast when some heroes have been deleted. Reading the existing story must remain possible.
4. Persistence of unsaved forms, reading position, and reader preferences; possible confirmation before discarding changes.
5. The limit on simultaneous generation jobs, cancellation, and presentation of multiple jobs on Home. Mockups show one job without a cancel action.
6. Whether the generation screen shows an open action after success or automatically navigates to the reader; when to request notification permission.
7. Ordering within custom places, predefined places, and saved stories, and the source of the final small Places thumbnails.

Audio, story text editing, plot-based series, payments, and a separate notification center are not part of the currently described screens.

# Story flow proposals

Open `index.html` in a browser. These are standalone HTML proposals, not Android screens. No build or server is required. Fonts use Google Fonts when available, with local fallbacks; images, icons, CSS and JavaScript are local.

## Screens

- `reader.html`: story text interspersed with illustration space, full-screen artwork, text sizes, evening mode, continuation. `?part=2` previews the next chapter.
- `continue.html`: required next-chapter idea, optional idea suggestions, inherited cast and context. `?after=2` previews creating part 3.
- `generating.html`: background generation with meaningful stages and an explicit route back to Home.
- `home.html`: use the review-only selector or `?status=generating`, `?status=ready`, `?status=error` to inspect each job state.
- `adventure.html`: related saved parts in narrative order.

## Product assumptions

A parent generates and reads aloud. Stories are immutable and saved automatically. Illustrations belong between passages; tapping one displays it to the child. A continuation inherits characters and previous events and creates a separate saved part. A story is ready once its text and illustrations are complete. Generation continues after leaving the screen/app; completion notifications open the new part when notification permission is available. Home exposes the result regardless of notification permission.

## Prototype limits

The state selector is for review only. Progress, notifications, reading persistence and backend retry are not implemented. The reader has abbreviated example copy. Existing artwork represents illustration placement, not proposed final scenes: production images must depict the characters and events of each passage. Font controls, evening mode, artwork enlargement, idea suggestions and navigation work. Prompt persistence uses optional session storage; no entered idea is sent anywhere. Some links intentionally lead to the existing static create-screen references.

These designs keep the existing English UI copy and cream/coral/teal palette. Static copy in the Android implementation must use string resources. No new generation, pricing, audio or account requirements are introduced by the mockups.

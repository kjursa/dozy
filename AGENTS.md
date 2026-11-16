# Repository guidance

## Project overview

This repository contains `backend/` and `mobile/` for Dozy, an app that generates bedtime stories for children using user-defined characters.

## Shared engineering principles

- Prefer small methods and classes with one clear responsibility (Single Responsibility Principle).
- Use explicit imports for Kotlin types and functions instead of fully qualified references in code. Resolve conflicting names with descriptive import aliases (`import ... as ...`); avoid wildcard imports.
- Provide dependencies through dependency injection, preferably constructor injection. Avoid creating repositories, services, or other collaborators inside the classes that use them.
- PRs that change application logic should include focused unit tests for the changed behavior, especially for validation, mapping, use cases, and repository logic.

## Identifiers

- Use UUIDv7 for new resource identifiers and UUIDv4 for idempotency keys. Generate character and custom-place IDs in the client to support offline creation; generate job, story, and asset IDs in the backend. Preserve an existing resource ID across updates and synchronization, and reuse the same idempotency key when retrying an uncertain request.

## Language

- Write code comments, documentation, and other repository-maintained text in English. Use English for commit messages, issue titles and descriptions, and PR titles and descriptions as well. Localized UI resources and user- or backend-provided content may use their intended language.

## API

For any task that changes files under `api/`, read and follow `api/AGENTS.md` in addition to these repository-level instructions.

## Backend

For any task that changes files under `backend/`, read and follow `backend/AGENTS.md` in addition to these repository-level instructions.

## Mobile

For any task that changes files under `mobile/`, read and follow `mobile/AGENTS.md` in addition to these repository-level instructions.

## E2E

For tasks under `e2e/` or changes to `test-e2e`, read and follow `e2e/AGENTS.md`. E2E execution is manual-only and is not part of routine development verification or `./test-all`.

## Commit messages

Use Conventional Commit prefixes in commit messages, in the format `<type>(optional-scope): <short summary>`. Use a lowercase type such as `feat`, `fix`, `docs`, `chore`, `refactor`, `test`, `build`, `ci`, `perf`, or `revert`. Keep the summary concise and imperative, for example `docs(backend): clarify DI module placement`.

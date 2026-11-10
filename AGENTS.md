# Repository guidance

## Project overview

This repository contains `backend/` and `mobile/` for Dozy, an app that generates bedtime stories for children using user-defined characters.

## Shared engineering principles

- Prefer small methods and classes with one clear responsibility (Single Responsibility Principle).
- Provide dependencies through dependency injection, preferably constructor injection. Avoid creating repositories, services, or other collaborators inside the classes that use them.
- PRs that change application logic should include focused unit tests for the changed behavior, especially for validation, mapping, use cases, and repository logic.

## Language

- Write code comments, documentation, and other repository-maintained text in English. Use English for commit messages, issue titles and descriptions, and PR titles and descriptions as well. Localized UI resources and user- or backend-provided content may use their intended language.

## Backend

For any task that changes files under `backend/`, read and follow `backend/AGENTS.md` in addition to these repository-level instructions.

## Mobile

For any task that changes files under `mobile/`, read and follow `mobile/AGENTS.md` in addition to these repository-level instructions.

## Commit messages

Use Conventional Commit prefixes in commit messages, in the format `<type>(optional-scope): <short summary>`. Use a lowercase type such as `feat`, `fix`, `docs`, `chore`, `refactor`, `test`, `build`, `ci`, `perf`, or `revert`. Keep the summary concise and imperative, for example `docs(backend): clarify DI module placement`.

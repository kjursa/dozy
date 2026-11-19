# Manual API E2E tests

This standalone Kotlin/JVM Gradle build tests the public SDK against a separate,
real backend process over HTTP. It uses the shared `api/client` and `api/contracts`
through `includeBuild("../api")`, with no backend implementation dependency.

E2E runs are manual-only. They are not part of `./test-all`, normal development
checks, or automatic CI. Agents must not execute them unless the user explicitly
requests a run; see [AGENTS.md](AGENTS.md).

## Run PlacesTest with a disposable backend

Requirements: macOS/Linux, Bash, Python 3.10+, and JDK 21 available through
`JAVA_HOME`/`PATH`. Gradle uses the existing backend wrapper.

From the repository root, run:

```bash
./test-e2e
```

The runner:

1. Builds the backend distribution with `installDist`, without running tests.
2. Starts that distribution in a separate process on an available loopback port,
   with a temporary SQLite database/assets directory and fake generators.
3. Waits up to 60 seconds for the owned backend's startup log and `/hello` response.
   If the selected port was taken before startup, it fails instead of using another server.
4. Runs only `com.dozy.e2e.PlacesTest`. Tests execute on every manual invocation,
   independently of Gradle's up-to-date or build-cache state.
5. Stops the backend and removes temporary data on completion, failure or interruption.
   Startup/test failures include the backend log; the process exit code indicates success/failure.

Individual test results and the final PASSED/FAILED summary appear in the console.
The HTML report remains in `e2e/build/reports/tests/test/index.html`.

## Places coverage

- A new guest sees shared, non-editable system places.
- Create, normalize, replay and replace a custom place; clear inhabitants on replacement.
- Reject edits to system places and preserve their original data.
- Return shared validation errors for invalid fields without creating records.
- Isolate guest accounts and reject attempts to replace another guest's place.

Each scenario creates its own guest session and uses resource IDs returned by the backend.

## Use an already running backend

To run manually against an explicitly chosen backend:

```bash
DOZY_E2E_BASE_URL=http://127.0.0.1:8080 \
  ./backend/gradlew -p e2e --no-daemon test --tests com.dozy.e2e.PlacesTest
```

Without the runner, the caller owns backend startup/shutdown and database cleanup.
The default URL is `http://127.0.0.1:8080`; these scenarios persist data under fresh
guest accounts. The runner always overrides the URL with its isolated backend.

## Compile without running E2E

Development verification may compile sources and check scripts without starting a server:

```bash
./backend/gradlew -p e2e --no-daemon testClasses
bash -n test-e2e
python3 -c 'from pathlib import Path; compile(Path("e2e/run_places.py").read_text(), "e2e/run_places.py", "exec")'
```

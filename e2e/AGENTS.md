# E2E guidance

- E2E tests are manual-only. Do not execute `./test-e2e`, this runner, or Gradle E2E test tasks during development or routine agent verification. Run them only when the user explicitly requests an E2E run. Compiling with `testClasses` and checking script syntax is allowed.
- Keep E2E out of `./test-all`, normal development checks, and automatic CI jobs unless the user explicitly changes this policy.
- Test public behavior through `api/client` and shared contracts against a separately running backend. Do not import backend services, repositories, DAOs, or use Ktor `testApplication` here.
- Give each scenario a fresh installation identity and use UUIDv7 resource IDs. Keep scenarios independent and assertions focused on observable behavior.
- Reuse the runner's disposable database and fake generators. Always stop owned processes on success, failure, interruption, or startup timeout; never kill an unrelated server.

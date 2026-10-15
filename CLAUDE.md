# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Follow [AGENTS.md](AGENTS.md) for shared repository rules. This file keeps only Claude-specific routing, a command quick reference, the module map, and current-state cautions.

## Find task-relevant guidance

- Read only the matching record under docs/planning/ when an issue has prior measurements or a recorded decision. Treat captures and counts as dated evidence; check current source, build, device, and inputs before relying on them.
- For frame-time, stutter, scroll, or transition investigations, use [.claude/skills/keios-frame-performance/SKILL.md](.claude/skills/keios-frame-performance/SKILL.md) and load only the measurement reference needed for the task.
- For dependency maintenance, follow AGENTS.md and inspect current Gradle declarations and sources. KeiOS tracks the latest Miuix SNAPSHOT; do not assume a snapshot is current or stale from an old note. The tracked root gradle.properties sets `miuix.version`, which overrides `miuix` in gradle/libs.versions.toml (Gradle properties, then local.properties, then the catalog), so bump both together and confirm the effective pin with `scripts/deps/miuix_snapshot_check.sh`.

## Commands

[readme/BUILD.md](readme/BUILD.md) is the source of truth; these are the everyday entry points.

```bash
./gradlew :app:compileDebugKotlin                 # fast compile check
./gradlew :app:assembleDebug                      # debug APK (os.kei.debug); builds only, avd scripts install
./gradlew :<module>:testDebugUnitTest             # unit tests for one module
./gradlew :build-logic:check testDebugUnitTest -Proborazzi.test.verify=true --continue   # all modules, screenshots verified (release-gate form)
./gradlew :app:lintVitalRelease                   # lint; app sets abortOnError = true
./gradlew :feature-github:testDebugUnitTest --tests "os.kei.feature.github.data.apk.RemoteZipEntryReaderTest"   # single test class
./gradlew :app:verifyRoborazziDebug --tests "<ScreenshotTest>"   # screenshot check; record with recordRoborazziDebug
./gradlew -p build-logic check                    # convention plugins, no device needed
scripts/qa/test_report.sh                         # summarize unit test results
```

- Dependency resolution needs GitHub Packages credentials for Miuix: `gpr.user` / `gpr.key` in `~/.gradle/gradle.properties`, or `GITHUB_ACTOR` / `GITHUB_TOKEN`.
- `scripts/deps/miuix_snapshot_check.sh` reports the effective Miuix pin versus the newest snapshot; check it before working around Miuix-shaped bugs.
- Validation AVDs: `scripts/dev/avd_phone.sh` and `scripts/dev/avd_tablet.sh`. Release-gate and Baseline Profile commands are in BUILD.md.

## Architecture

- Gradle modules are listed in settings.gradle.kts.
- Layering: `app` → `feature-*` → `core-*`. `ui-liquid-glass` and `ui-pip` are standalone UI libraries consumed by `app`. `feature-github-engine` holds the GitHub release/download engine beneath `feature-github`; `feature-mcp` is the shared MCP contract that the BA, OS, Home, and GitHub modules build on. `feature-keepalive` (accessibility-service guard) and `feature-webdav` (WebDAV sync client, Jianguoyun preset) are consumed only by `app`.
- Most Compose UI lives in `app` (`os.kei.ui.page.main`, `os.kei.feature.*`, `os.kei.ui.navigation`). Feature modules mostly hold domain, data, and MCP tool code; `feature-github` is the largest. Check the owning module before adding code to `app`.
- `build-logic/` is an included build of convention plugins (`keios.android.application/library/compose/test`, `keios.miuix`) that owns SDK/JDK defaults, Compose compiler settings, and Miuix substitution. Change shared build behavior there, not in individual modules. See build-logic/README.md.
- Build types and their packages: debug (`os.kei.debug`), release, benchmark, and benchmarkRelease (all `os.kei`), releaseDiagnostic (`os.kei.diag`, R8-identical to release so it can sit beside it for A/B measurement), and nonMinifiedRelease (`os.kei.profilecapture`, profileable and used by connected Baseline Profile tasks, which install and uninstall it). Release, benchmark, benchmarkRelease, and releaseDiagnostic all read Baseline Profiles from `app/src/release/generated/baselineProfiles/`, which the `baselineprofile` module generates from nonMinifiedRelease. Release uses it by default; app/build.gradle.kts wires in the others with `addStaticSourceDirectory`, and without that releaseDiagnostic ships unprofiled and stops being a valid A/B twin of release.
- Signing: `release` builds unsigned (not installable) unless all four `keios.release.storeFile/storePassword/keyAlias/keyPassword` are set in Gradle or local.properties. The other release-like types fall back to `app/signing/keios-ci-debug.keystore`, so use releaseDiagnostic or benchmark for an installable release-like APK.
- MCP tools: add the name to `feature-mcp` `McpToolCatalog`, with en/zh/ja text in `McpToolCatalogDescriptions`, the argument schema in `McpToolCatalogArguments`, and write/network flags in `McpToolCatalogDefinitions`. Implement it in the owning module's `os.kei.feature.<x>.mcp.Mcp*ToolPlugins` (runtime/dev tools live in `feature-mcp` `McpRuntimeTools`/`McpDevTools`; WebDAV and app-side delegates in `app` `os.kei.mcp.bridge`), then aggregate it in `app` `os.kei.mcp.server.KeiOsMcpToolPlugins`. `:app` `McpToolRegistrationTest` and `:feature-mcp` `McpToolCatalogLocalizationTest` fail when registrations, catalog, and translations drift.

## Shared tools and tests

- Before device work, inspect connected targets and select the intended device explicitly. Do not assume emulator-5554 or reuse a serial from an old run. Preserve active ADB, device, and Gradle work.
- Add or update a focused test when it can catch an observable regression not already covered. Put it in the owning module and avoid tests that assert implementation text or call order without a behavior contract. Consult readme/BUILD.md for commands.
- Commit, push, or publish only when the user asks. When commits are requested, group them by review boundary and verify each requested state.

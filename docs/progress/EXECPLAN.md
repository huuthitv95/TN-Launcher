# TN Launcher — living implementation and reconstruction plan

## Scope and non-goals
Original MVP scope: native Kotlin/Views API 29 launcher with Home, apps/search, editable Home/Dock, settings, themes, wallpaper, persistence and safe vendor app shortcuts. The subsequent reconstruction phase below adds folders, a separate Android widget host, app management and public system entries. Media transport, live vehicle data, system overlays, firmware and physical-device operations remain outside this increment. The user has now explicitly requested source commit and push to main; the reviewed publication scope excludes vendor archives, private data and APK/build outputs.

## As-is inventory and evidence
Baseline: main / bfbf053, clean worktree; only AGENTS.md and README.md. Local SDK/API 29 Google APIs x86_64 image and WHPX available; no AVD or connected device. Java 8 default; Java 21 at C:/Program Files/Java/jdk-21. Figma Home 144:62674 / Dock 558:50382 inspected; Apple UI Kit assets excluded per its license. Original launcher8 Manifest declares android.uid.system; this is not a permission available to TN Launcher.

## Intended change
One app module, ViewModel/StateFlow, constructor injection, Preferences DataStore; application ID vn.tnlauncher.app, version 0.1.0 (1). AGP 8.10.1 / Gradle 8.11.1 / Kotlin 2.1.21 / process-scoped JDK 21; min 29, compile/target 35.

## Implementation steps
1. Bootstrap project/wrapper and isolated API 29 AVD.
2. Implement repositories, persistence, vendor contracts and capability detection.
3. Implement adaptive Home, Dock, app drawer/search, settings, themes and wallpaper.
4. Complete static vendor evidence and focused unit/instrumentation/E2E tests.
5. Run both landscape profiles, recovery/lifecycle and performance checks; repair defects.
6. Review hygiene and deliver one debug APK/checksum with truthful status.

## Parity and validation
Unit, lint, assemble, instrumentation, emulator E2E with installed test apps; two representative profiles (1280x720/160dpi, 1920x1080/320dpi). Foreground non-interference verified without claiming OEM camera equivalence. Hardware-only tests remain DEVICE_TEST_REQUIRED. Final results are recorded in docs/testing/compatibility-report.md.

## Progress
- Project, wrapper and isolated API29 AVD are running. Build, 19 unit tests and lint (0 errors, 45 warnings) pass.
- Core repositories, recovery, vendor contracts and functional Home/Dock/Drawer/Search/Settings UI are implemented. Twelve vendor APKs were inspected statically; original APKs remain outside the app/repo.
- Both profiles pass19unit, lint0errors/45warnings, eleven instrumentation scenarios and eight E2E journeys. Supplied fonts, Vietnamese, both app themes, large text, persistence, package changes and bounded icon-cache/render pilot are verified.
- The hold/release Dock defect is fixed. Test wiring now uses HOME intents, a guarded test-only self-termination probe and measured framebuffer dimensions; SDK stderr warnings do not override Gradle exit codes.
- Final visual/source/artifact review passes. SYSTEM follows the observed light OS; global night toggling is not applicable on the locked image. Slow software-rendered frame timing is documented. This is a functional MVP, not a completed Figma fidelity pass.

## Decisions
Actual installed apps alphabetically on first start; six user-pinned Dock favorites plus fixed Apps/Settings. User-authorized static fonts with system sans fallback; original native surfaces; no Apple UI Kit assets. Original vendor APKs are analyzed only, never installed or bundled.

User updated AGENTS.md in commits03a0a6c/4491b15 during implementation: Section22 authorizes the supplied fonts (USER_CONFIRMED), without a repeat license gate. Technical inspection selected static SF Pro Text Regular, SF Pro Display Semibold and MRCH NewYork (fixed Latin brand only), with system sans fallback. Typography passed API29 regression across both profiles and app themes. The subsequent explicit request to commit and push to main includes the three reviewed binaries tn_new_york.ttf, tn_sf_body.otf and tn_sf_heading.otf, whose sizes and hashes match the preserved original inputs; no repeat license review is required.

## Risks and rollback
System UID, signatures, native services and firmware can prevent parity. No background foreground forcing. Never modify original apps or real device. Preserve unrelated work; rollback only task-owned changes. Emulator default Home changes are isolated to the task AVD.

## Outcomes
EMULATOR_GATE_PASSED for functional MVP0.1.0(1). Single debug APK: app/build/outputs/apk/debug/app-debug.apk; exact SHA256 and observations in compatibility-report. The tested implementation was a worktree based on main/HEAD4491b15 before the requested source commit. Source commit and push to main are now explicitly authorized; consult Git for the resulting commit SHA and push outcome. No DEVICE_VERIFIED or AUTOMOTIVE_VERIFIED claim; Wincavn rollback remains unverified.

## Current phase — original Launcher reconstruction
The user's subsequent prompt changes the next priority: functional reconstruction precedes broad Figma changes. The previous MVP gate covers only the documented MVP, not complete original-Launcher parity. All existing local implementation is the starting worktree and must be preserved; AGENTS.md and the external Understand Anything installation are not edited.

Scope: verify retained baseline evidence; build a Vietnamese source knowledge graph with `understand --language vi --no-auto-update`; expand original APK/component/behavior evidence and classify every discovered gap; implement safely reproducible standard-Android functions and verified vendor activity entry points; investigate lint/performance warnings; rerun affected checks and both API29 profiles before claiming the new increment verified. No vendor service/broadcast/Binder experiments, physical-device actions, broad redesign, plugin update or automatic graph hooks. Source commit and push to main are separately authorized by the user's subsequent explicit request; completion is determined from Git rather than assumed here.

Milestones:
1. Verify APK hash, retained test reports, current source/configuration, tools and emulator availability.
2. Validate the installed Understand Anything checkout without changing its Git state; generate/review graph coverage, content freshness and loopback dashboard access.
3. Extend the existing research/matrix with original behavior, exact evidence, gap category1–6, missing behavior, risk and next action.
4. Implement highest-priority evidenced gaps with targeted tests, preserving Home/app/persistence behavior.
5. Run build/unit/lint and two-profile emulator regression; inspect artifact, diff and minimal shared-context file set.

Completion criteria: no known evidenced category1–3 gap in the current bounded increment remains unfinished; each unresolved original feature is explicitly incomplete, unknown or blocked with its actual dependency. Full original-Launcher reconstruction cannot be declared complete while privileged/hardware interfaces and safety behavior remain unverified. Knowledge graph coverage/freshness must be measured, not inferred from HEAD while source is uncommitted. Dashboard tokens and intermediate analysis remain local. Readiness for the separate Figma phase is decided from the expanded matrix rather than the MVP gate.

Progress: baseline SHA-256 matches the delivered46f72880…74f97 APK; the pre-commit worktree started from main/HEAD4491b15 and retained the pre-existing local MVP. Twenty-two APK metadata/Manifest inventories and relevant decompiled traces are recorded. The Android increment has passed25unit, lint0errors/51warnings,33instrumentation and9E2E in each representative profile; a final targeted check covers the amended pagination harness. Full original parity remains incomplete. Final artifact, observations and gate are recorded in compatibility-report, not inferred from this progress paragraph.

Files changed in this increment, reviewed for the requested source commit/push:

- Composition/Manifest: `app/src/main/AndroidManifest.xml`, `app/src/main/java/vn/tnlauncher/app/LauncherApplication.kt`, `app/build.gradle.kts`.
- Data: `app/src/main/java/vn/tnlauncher/app/data/LauncherFolder.kt`, `LauncherWidgetRepository.kt`, `LauncherModels.kt`, `LauncherSettingsRepository.kt` in the same directory.
- Integration: `app/src/main/java/vn/tnlauncher/app/integration/AppManagement.kt`, `SystemSettingsIntegration.kt`, `WincavnIntegration.kt`.
- UI: `app/src/main/java/vn/tnlauncher/app/ui/HomeActivity.kt`, `LauncherViewModel.kt`, `WidgetsActivity.kt`, `LauncherDialogs.kt`; `app/src/main/res/drawable/ic_folder.xml`, `app/src/main/res/layout/activity_home.xml`.
- Tests: `app/src/test/java/vn/tnlauncher/app/LauncherFolderRulesTest.kt`; `app/src/androidTest/AndroidManifest.xml`; `ActivityAccessInstrumentedTest.kt`, `ReconstructionInstrumentedTest.kt`, `WidgetsInstrumentedTest.kt`, `WidgetStorageInstrumentedTest.kt`, `FixtureActivity.kt` and four Java widget/component-state fixtures under `app/src/androidTest/java/vn/tnlauncher/app/`; three widget fixture XML files under `app/src/androidTest/res/`.
- Drivers and handoff: `tools/verify-emulator.ps1`, `tools/emulator-e2e.py`, narrow `.gitignore`, `.ua/knowledge-graph.json`, `.ua/config.json`, `.ua/.understandignore`; existing README, architecture, research, testing, release and progress documents. No AGENTS, original vendor binary, font input, system environment or vehicle change.

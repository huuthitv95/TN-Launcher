# TN Launcher — living implementation and reconstruction plan

## Active increment — 16/10/2026 static evidence W1–W8

Scope: follow the latest user prompt from clean `main/6a02097a0d0186a2e2ace70e4ba8c1287176af87`. Close static evidence gaps only; no production/test/Gradle/Manifest changes, vendor execution, ADB, IPC, vehicle actions, Figma, commit or push. This supersedes earlier publication authorization for this new increment. Keep temporary analysis outside the repository and edit only the existing canonical files permitted by the prompt.

Publication follow-up: the user's subsequent explicit request authorizes committing and pushing the reviewed six canonical documents and shared graph to the existing main branch. This supersedes the no-commit/push restriction above for this handoff only. Preserve source and all verification states; review staged files, validate graph hashes/schema, and verify the remote commit. Keep the historical graph analysis base; use content hashes for freshness.

Workstreams: W1 launcher identity/default-HOME consequences; W2 both distinct CAN APKs and the actual MS producer; W3 JNI/platform and narrowly scoped non-sensitive configuration; W4 toolkit register/get/cmd side-effect census; W5 camera inbound receivers/action mismatch; W6 media service and navigation failure contracts; W7 metadata/signatures for the remaining12 APKs; W8 full-SHA/FQCN/method-signature evidence index and implementation-readiness proposals without code.

Validation/acceptance: each workstream ends with a confidence-based static resolution or a precise unresolved/device requirement. Preserve all implementation/verification states unless justified explicitly. Verify every used APK against the inventory, then rehash at completion; validate relative links, diff scope, secret/private-path boundaries, official graph schema and100% LF-normalized hashes. Gradle/lint/instrumentation/E2E are NOT_RUN because no source changes are allowed. Baseline graph321nodes/449edges/68files has all68 hashes current. Original22 APK hashes and Java21.0.8/JADXdev CLI/Apktool3.0.3/AAPT35/apksigner0.9 have been verified. Outcomes: W1–W8 static workstreams completed with independent confidence/limits in the vendor contract. Both CAN client versions, actual MS producer, camera identity/receiver gaps, toolkit side effects, media/nav failures and12 additional metadata samples are recorded. Final root rehash PASS: 34/34 APKs match inventory. Static validation PASS: diff check/scope, 24 relative links/anchors, UTF-8, added-text secret/private-path review, 88 provenance rows and 13 parity rows with protected cells unchanged. Official graph schema and 68/68 LF-normalized hashes PASS under the bounded documentation-only refresh. No implementation/verification state was promoted and no application runtime validation ran.

Risks/rollback: decompiler control-flow recovery, variant/config/JNI gaps and stateful subscriptions can limit conclusions. Stop reading any configuration suspected to contain sensitive data; never inspect logs/backups/crash/private files. Retain original APKs and all previous evidence; change only task-owned documentation/graph. Full reconstruction remains INCOMPLETE and Figma deferred.

Validation continuity: graph topology/source descriptors remain321nodes/449edges/68files/8layers/9tour; only the six changed document hashes and a nested latestDocOnlyRefresh marker are refreshed after document freeze; source descriptors and graph topology remain unchanged. Stock incremental actually exited1 on relevant dirty documents; preserve its BLOCKED_DIRTY_WORKTREE result and all private metadata. Required static checks passed and exact outcomes are summarized in STATUS; no new report or test artifact is created in the repository. Remaining helper/driver analysis is still possible statically; native semantics require matching artifacts, while active firmware/access and physical effects require separately authorized runtime validation.

## Previous increment — vendor communication contracts, delivered in6a02097

Publication follow-up: the user's subsequent explicit request authorizes committing and pushing the reviewed source to the existing main branch. This supersedes the no-commit/push restriction retained below as research history. Preserve the tested source, exclude vendor archives/private data/build artifacts, verify staged content and the remote result. The graph retains its analysis-base commit; normalized file hashes, not the commit containing the graph, establish snapshot freshness.

User scope: continue original functional reconstruction only; defer Figma/style changes. Starting baseline: `main`, HEAD `15dc34fbc7621bd826497d352bf288e7eb491261`, clean worktree and cached `origin/main` aligned. Existing Android tests are historical evidence, not rerun results. Shared graph baseline: 316 nodes, 431 edges, 67 file hashes all match LF-normalized source; its older analysis-base SHA does not invalidate that snapshot.

Steps: (1) verify tools/artifact provenance; (2) trace RemoteTools/IRemoteModule/Callback/Registrar into the matching ms Binder implementations; (3) independently trace camera/reverse/overlay and media/radio/DSP/navigation producer-consumer paths; (4) classify each contract by confidence, side effects, permissions and feasibility; (5) implement only a demonstrated safe gap with focused tests, if found; (6) update canonical evidence/parity and incrementally refresh/validate the graph; (7) review diffs and retained artifacts.

Expected files: existing research/inventory/parity, architecture, STATUS/this plan and compatibility report when new validation exists; `.ua/knowledge-graph.json`. A dedicated `docs/architecture/wincavn-integration.md` is warranted only if the recovered cross-APK contracts need a canonical specification. Application/tests change only after a concrete safe implementation decision. No AGENTS edits, vendor source/binary import, OEM execution/IPC, device changes, plugin update, commit or push in this increment.

Acceptance: reproducible new caller/receiver evidence for the previously unresolved toolkit and automotive domains; no unknown command executed; independent implementation/verification states retained; graph schema/inventory/hashes checked against final source. Run affected API29 tests if implementation changes; static-only work does not require repeating application regression. Preserve camera/vehicle uncertainty, SwiftShader jank and the prior animation0.0 SurfaceFlinger limitation. Risks: obfuscation, decompiler gaps and absent firmware/native dependencies; missing edges remain explicit rather than guessed.

Evidence and implementation decision: RemoteTools wire transactions/callbacks and MS dispatch are now cross-referenced; GPIO/MCU/camera-owned window paths, media snapshot versus BT patch, guarded MediaSessions and default-map backend are documented in the canonical contract specification. Review found NAV MainActivity actually waits for callback28 before reaching LaunchNavi; prior direct LaunchNavi can silently lose a cold-start command. Change navigation to exact public MainActivity/ACTION_MAIN (still explicit-only, user initiated, no toolkit in TN). Extend the existing fixture regression to assert preserved MAIN action and correct explicit screen. Run both API29 regression profiles and E2E before claiming the new artifact gate. This supersedes the earlier no-code-change expectation, not the prohibition on live vendor IPC/vehicle/Figma/commit/push.


Validation/outcomes for this active increment:25unit PASS, lint0errors/51warnings,33instrumentation/0skip and9E2E PASS on each API29 profile1280/160 and1920/320 at animation1.0. The first1920 run failed one component-state test; a test-only fresh ResultReceiver ACK plus onNewIntent correction passed4/4 focused repeats and final full regressions on both profiles. Production APK stayed unchanged by that test correction: debug0.1.0(1), SHA256 `22c3c663b46a68d71b7c896c1d0649bd2896758db23ed04fbb98c3afc8ff763c`. Gate is scoped to Android, not vendor runtime or automotive parity. Details, failed-run evidence and measured render limits remain in the compatibility report.

Graph handoff: saved shared graph321nodes/449edges/68files/8layers/9tour preserves prior316 IDs/431 edges and47 prior source symbol identities;4 new callable nodes use reviewed Java/lexical scopes. The total of449edges includes the genuine STATUS-to-contract Markdown link. Official schema and inventory/LFhash pass; shared graph was saved after final-doc freeze and fresh pre/post-source validation. Stock incremental is BLOCKED_DIRTY_WORKTREE, explicitly distinct from this manual refresh; do not fake committed meta/fingerprints. No dashboard/plugin update, commit/push, OEM execution or physical-device work occurred. Full original reconstruction remains INCOMPLETE and Figma remains deferred.

## Scope and non-goals
The following sections retain the history of the already delivered MVP/Android reconstruction. Their earlier commit/push authorization was fulfilled by baseline15dc34f; the active increment above prohibits new commit/push and takes precedence. Historical test counts describe their original artifacts, not automatic verification of the current navigation change.

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

# StockPilot AI — Build Progress

| Milestone | Status | Evidence |
|-----------|--------|----------|
| 1 — Foundation (Gradle, theme, navigation) | Done | `./gradlew assembleDebug` SUCCESS |
| 2 — Room inventory + dashboard UI | Done | 30-product seed, KPI/search/filter/edit |
| 3 — Assistant demo mode + tests | Done | `testDebugUnitTest` (parser/executor/reorder) |
| 4 — Audio + TTS | Done | Push-to-talk, honest Whisper-unavailable message |
| 5 — Model Manager import | Done | SAF import, atomic copy, ImportedUnverified only |
| 6 — Polish + handoff | Done | This file, `README.md`, `HANDOFF.md` |

## Commands run (2025-11-20)

```text
./gradlew testDebugUnitTest   → BUILD SUCCESSFUL (10 tests)
./gradlew assembleDebug       → BUILD SUCCESSFUL
```

**Debug APK:** `app/build/outputs/apk/debug/app-debug.apk` (~17 MB)

## Device testing

- `adb devices` — no emulator or physical device attached; on-device acceptance testing **pending**.

## Notable implementation choices

- Application ID `com.stockpilot.ai` (renamed from starter `com.peaic.saminference`).
- AndroidX versions pinned for **AGP 8.11.2 / compileSdk 36** (downgraded from template 1.19.x / lifecycle 2.11).
- `InventoryOperations` interface extracted for JVM unit tests without Robolectric.
- Real inference statuses never set to `Ready` in Phase 1; import → `ImportedUnverified` only.

## Open polish (non-blocking)

- `@OptIn(ExperimentalCoroutinesApi)` for `flatMapLatest` in `InventoryViewModel` (warning only).
- Material volume icons deprecation warnings in `AssistantScreen`.
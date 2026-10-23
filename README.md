# StockPilot AI

Offline-first Android retail inventory with a conversational assistant. **Phase 1** ships inventory UI, Room data, a **DEMO MODE** command interpreter (no Gemma/Whisper weights), voice recording, Android TTS, and model import plumbing for Phase 2.

| | |
|---|---|
| **Application ID** | `com.stockpilot.ai` |
| **Stack** | Kotlin, Jetpack Compose (Material 3), Room, Navigation, DataStore, coroutines |

## Features

- Dashboard KPIs, search, stock filters, product editing
- 30-product sample catalog (5+ low stock, 2 out of stock)
- Typed assistant: stock lookup, low-stock list, reorder math (`max(0, target − (stock − sales))`)
- Push-to-talk + honest “speech model not installed” UX
- Model Manager (SAF import, private storage, unverified status)

**DEMO MODE** is shown anywhere assistant behavior is simulated. Imported model files do **not** enable AI until Phase 2 runtimes are integrated.

## Build & run

```bash
cd AndroidStudioProjects/SamInference
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Install: `app/build/outputs/apk/debug/app-debug.apk`

Open in Android Studio: sync Gradle, run on API 26+ device/emulator. Microphone permission is requested from the Assistant screen.

## Architecture (high level)

```text
UI (Compose) → ViewModels → InventoryRepository / AssistantOrchestrator
                                    ↓
                              Room (ProductEntity)
DemoInferenceEngine → DemoIntentParser → InventoryCommandExecutor
UninstalledSpeechEngine + AudioRecorder + AndroidTtsManager
ModelImportManager → ModelStorage (filesDir/models + DataStore metadata)
```

See `HANDOFF.md` for Phase 2 and model import steps.

## Tests

| Suite | Location |
|-------|----------|
| JVM unit | `ReorderCalculatorTest`, `DemoIntentParserTest`, `InventoryCommandExecutorTest`, `ModelManifestTest` |
| Instrumented (optional) | `InventoryDaoInstrumentedTest` — requires device/emulator |

## License / models

Do not commit model weights. `.gitignore` excludes common weight extensions and `files/models/`.
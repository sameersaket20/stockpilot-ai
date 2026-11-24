# StockPilot AI

Offline-first Android retail inventory with a conversational **on-device agent** (Gemma 4 E2B via LiteRT-LM, Whisper for voice). Read-only access to Room inventory; no cloud LLM; provenance on every assistant reply.

| | |
|---|---|
| **Application ID** | `com.stockpilot.ai` |
| **Stack** | Kotlin, Jetpack Compose (Material 3), Room, Navigation, DataStore, Hilt, coroutines, LiteRT-LM |

## Highlights (for reviewers)

- **~30s → ~10–12s** assistant latency on OnePlus 10 Pro (**GPU** first, CPU fallback); e.g. lookups ~**10s**, open-ended *“what’s the next best thing to do?”* ~**12s** (in-chat timer).
- **Plan → query → answer:** Gemma plans generic `tool_calls`; Kotlin runs Room tools; narrator uses **field glossary**—works for **lookups and arbitrary advisory questions** (no fixed intent menu).
- **Honest offline UX:** Model Manager, provenance labels, **“Responded in Xs”** on each assistant reply.

Full before/after write-up: **[IMPROVEMENTS.md](./IMPROVEMENTS.md)**

## Features

- Dashboard KPIs, search, stock filters, product editing (30-SKU seed catalog)
- **Inventory Assistant** — planner + read-only tools + narrator + guardrails; tappable product rows; persisted chat
- Voice: push-to-talk + Whisper when imported; TTS (default off)
- Model Manager: SAF import for Gemma + Whisper

## Build & run

```bash
cd AndroidStudioProjects/SamInference
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Install: `app/build/outputs/apk/debug/app-debug.apk` or `./gradlew installDebug`

Open in Android Studio: sync Gradle, run on API 26+. Microphone permission from the Assistant screen.

## Architecture

```text
AssistantScreen → AssistantViewModel → AssistantOrchestrator
                                         → InventoryAgentOrchestrator
                                               → GemmaInferenceEngine (GPU→CPU, plan + narrate)
                                               → InventoryToolRunner (summary / query / full read)
                                               → ResponseGuardrails
Inventory UI → InventoryRepository → Room (ProductEntity)
WhisperSpeechEngine ← ModelStorage ← ModelImportManager
```

## Models (not in git)

Import on device via **Settings → Model Manager**. See `GEMMA_MODEL_PREP.md` and `WHISPER_MODEL_PREP.md`.

## Tests

| Suite | Examples |
|-------|----------|
| Unit | `InventoryProductQueryTest`, `AgentPlanParserTest`, `ChatResponseTimingTest`, `ReorderCalculatorTest` |
| Instrumented | `InventoryDaoInstrumentedTest` (device) |

## Docs index

| File | Purpose |
|------|---------|
| [IMPROVEMENTS.md](./IMPROVEMENTS.md) | Before/after, performance, agent design notes |
| [HANDOFF.md](./HANDOFF.md) | Operational handoff, import, limitations |
| [GEMMA_MODEL_PREP.md](./GEMMA_MODEL_PREP.md) | LiteRT artifact selection |
| [BUILD_PROGRESS.md](./BUILD_PROGRESS.md) | Milestone log |
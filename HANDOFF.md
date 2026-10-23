# StockPilot AI — Phase 1 Handoff

## What works today

- **Inventory dashboard** — 30 seeded retail SKUs (includes **Blue T-shirt**), live KPIs, search, low/out filters, edit dialog with validation.
- **Room persistence** — stock edits survive process death (DataStore “seeded” flag prevents re-seed).
- **Inventory Assistant (DEMO MODE)** — typed questions parsed by `DemoIntentParser`; answers from `InventoryCommandExecutor` + live Room data; labeled **“Demo interpreter • Live Room data”**.
- **Conversation context** — follow-up reorder questions can reference the last product (e.g. after asking about Blue T-shirt).
- **Voice** — `RECORD_AUDIO` permission, push-to-talk recording (16 kHz mono target format); **no fake transcription** — shows Whisper-not-installed message.
- **TTS** — Android `TextToSpeech` with mute toggle; warns if offline voice uncertain.
- **Model Manager** — Gemma + Whisper cards; SAF import into app-private storage; streaming copy; replace/remove; status **Missing** or **Imported (unverified)** only (never **Ready** without runtime).
- **Settings** — about blurb, Model Manager link, confirmed inventory reset.

## What is NOT implemented (Phase 2)

- Real **Gemma 4 E2B** inference (LiteRT-LM).
- Real **Whisper Tiny.en** transcription (whisper.cpp JNI).
- Disabling demo interpreter when real models load.
- Airplane-mode voice demo on hardware.

## How to import models later (on device)

1. Transfer files to the phone (Downloads, USB, etc.) — **the app does not download weights**.
2. Open **Settings → Model Manager**.
3. Tap **Import** on the correct card and pick the file via the system document picker (no broad storage permission).
4. Status becomes **Imported (unverified)** — this only stores files; Phase 2 must validate and initialize runtimes.
5. **Gemma:** LiteRT distributions may include **multiple assets**; current manifest supports a file list and can be extended for multi-file packages.
6. **Whisper:** expect a GGML `.bin` (e.g. `ggml-tiny.en.bin`).

Private storage paths: `filesDir/models/gemma/` and `filesDir/models/whisper/`.

## Phase 2 checklist (do not start until model files are on device)

1. Verify LiteRT-LM + Gemma 4 E2B format for your OnePlus 10 Pro API level; read official APIs — no invented integration.
2. Implement `GemmaInferenceEngine` after successful native runtime init; only then set `ModelStatus.Ready`.
3. Structured command JSON from Gemma → validate in Kotlin → execute; never trust model arithmetic.
4. Integrate whisper.cpp Android example; implement `WhisperSpeechEngine`; resample recorded audio as needed.
5. Profile memory; likely load one model at a time.
6. Confirm offline TTS voice packs.
7. Turn off demo parser for production demo path (optional debug flag).
8. Airplane-mode acceptance on real hardware.

References (manual):

- [LiteRT-LM overview](https://developers.google.com/edge/litert-lm/overview)
- [Gemma 4 E2B LiteRT](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm)
- [whisper.cpp Android example](https://github.com/ggml-org/whisper.cpp/tree/master/examples/whisper.android)

## Known limitations

- Suggested chip **“How many should I order?”** needs a product in context or a fuller sentence.
- Import validation is filename/size heuristics only — not compatibility proof.
- Instrumentation test `InventoryDaoInstrumentedTest` added but **not run** (no device in CI session).
- Starter project name in Android Studio may still show legacy paths in `.idea` until re-imported.

## Quick demo script

1. Install `app-debug.apk` on device.
2. Search **Blue T-shirt** → set quantity **24** → save.
3. **Ask Inventory AI** → `How many blue T-shirts do we have?`
4. `If we sell 10 tomorrow, how many should I order to have 30 left?` → expect **16**.
5. Change stock to **15**, ask again → reorder suggestion updates.
6. Record voice → see Whisper not installed message.
7. Model Manager → import any small dummy file → **Imported (unverified)**.
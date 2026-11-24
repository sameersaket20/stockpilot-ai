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

## Architecture

On-device boundary: inventory UI, assistant chat, Gemma, Whisper, Room, and imported model files all run on the Android device.

```mermaid
flowchart TB
  subgraph DEVICE["Android device · offline execution boundary"]
    direction TB

    subgraph PRESENTATION["Presentation"]
      UI["Jetpack Compose<br/>Inventory · Assistant · Settings"]
      VM["ViewModels<br/>InventoryViewModel · AssistantViewModel"]
    end

    subgraph ORCHESTRATION["Orchestration"]
      AO["AssistantOrchestrator<br/>facade → respond"]
      IAO["InventoryAgentOrchestrator<br/>plan · tools · narrate · validate"]
    end

    subgraph AI["On-device AI"]
      GEMMA["GemmaInferenceEngine<br/>one gemma-4-E2B-it.litertlm<br/>LiteRT-LM GPU then CPU<br/>planner + narrator complete"]
      WHISPER["WhisperSpeechEngine<br/>STT only · separate from Gemma"]
    end

    subgraph TRUSTED["Trusted business execution"]
      TOOLS["InventoryToolRunner · AgentPlanParser<br/>query · summary · reorder · guardrails"]
      REPO["InventoryRepository"]
    end

    subgraph PERSIST["Persistence and models"]
      ROOM["Room · products · assistant_messages"]
      CHAT["AssistantChatRepository"]
      MODELS["ModelStorage · ModelImportManager"]
    end
  end

  UI --> VM
  VM --> REPO
  VM -->|"user text"| AO
  VM --> CHAT
  CHAT --> ROOM
  VM -.->|"voice"| WHISPER
  WHISPER -.->|"transcript"| VM
  AO --> IAO
  IAO --> GEMMA
  IAO --> TOOLS
  TOOLS --> REPO
  REPO --> ROOM
  MODELS --> GEMMA
  MODELS --> WHISPER

  classDef presentation fill:#dbeafe,stroke:#2563eb,color:#1e3a8a
  classDef ai fill:#ede9fe,stroke:#7c3aed,color:#4c1d95
  classDef trusted fill:#ccfbf1,stroke:#0d9488,color:#134e4a
  classDef persist fill:#fef3c7,stroke:#d97706,color:#92400e
  classDef infra fill:#f3f4f6,stroke:#6b7280,color:#374151

  class UI,VM presentation
  class GEMMA,WHISPER ai
  class TOOLS,REPO trusted
  class ROOM,CHAT,MODELS persist
  class AO,IAO infra
```

**Orchestration:** `AssistantOrchestrator` is a thin façade; `InventoryAgentOrchestrator` owns planning, tool execution, narration, and validation.

**Cross-cutting (not separate pipeline stages):** Hilt DI, coroutines / `Flow`, and a mutex around Gemma inference lifecycle.

Editable source: [docs/architecture/system-architecture.mmd](./docs/architecture/system-architecture.mmd)

### AI request lifecycle

Example: *“Which products are out of stock, and what should I prioritize?”*

```mermaid
sequenceDiagram
  autonumber
  actor User
  participant VM as AssistantViewModel
  participant STT as RoutingSpeechEngine
  participant Chat as AssistantChatRepository
  participant AO as AssistantOrchestrator
  participant IAO as InventoryAgentOrchestrator
  participant Gemma as GemmaInferenceEngine
  participant Tools as InventoryToolRunner
  participant Room as InventoryRepository and Room

  opt Voice input
    User->>VM: push-to-talk WAV
    VM->>STT: transcribe
    STT-->>VM: text or Unavailable
  end

  User->>VM: send user text
  VM->>Chat: insert user message
  VM->>AO: respond
  Note right of AO: delegates only
  AO->>IAO: respond

  alt Gemma not Ready
    IAO-->>VM: agent unavailable
  else Happy path
    Note over Gemma: AI planner
    IAO->>Gemma: complete planner
    Gemma-->>IAO: tool_calls JSON
    Note over Tools,Room: deterministic reads
    loop Up to 6 tool_calls
      IAO->>Tools: invoke
      Tools->>Room: read-only query
      Room-->>Tools: ProductEntity rows
      Tools-->>IAO: facts JSON
    end
    Note over Gemma: AI narrator
    IAO->>Gemma: complete narrator
    Gemma-->>IAO: draft answer
    IAO->>IAO: ResponseGuardrails and Safety fast path
    IAO-->>VM: AssistantReply plus provenance
  end

  VM->>Chat: persist assistant plus timing
  VM-->>User: Responded in Xs and product links

  Note over IAO: Invalid plan bootstraps summary and query · narration or guard fail uses facts-only fallback
```

Editable source: [docs/architecture/ai-request-lifecycle.mmd](./docs/architecture/ai-request-lifecycle.mmd)

### Architectural decisions (concise)

| Topic | Rationale |
|-------|-----------|
| On-device inference | Privacy and offline retail use; trades cloud scale for local latency and RAM. |
| Gemma 4 E2B + LiteRT-LM | Mobile-sized instruction model with a single importable artifact and GPU/CPU backends. |
| Whisper separate from Gemma | STT and language reasoning differ in model lifecycle; typed chat works without voice models. |
| Planner vs narrator | Better tool selection and grounded answers; two `complete()` calls per turn (GPU mitigates latency). |
| Structured tools vs raw SQL | `InventoryToolRunner` exposes fixed read-only operations; filters live in Kotlin. |
| Deterministic reorder math | `ReorderCalculator` and repository code—not LLM arithmetic. |
| Response guardrails | Block mutation/SQL language in drafts; `AgentSafetyReview` fast path avoids a third Gemma call on success. |

## Performance optimization

Developer-measured on **OnePlus 10 Pro** (NE2211, Snapdragon 8 Gen 1), **GPU (Adreno)** path, same `gemma-4-E2B-it.litertlm`. Not independent benchmarks—see **[IMPROVEMENTS.md](./IMPROVEMENTS.md)**.

| Scenario | Wall time (approx.) | Notes |
|----------|---------------------|--------|
| CPU-only baseline (before GPU + agent tuning) | **~30s** | Single backend, heavier prompts |
| Focused inventory lookup | **~10s** | e.g. highest / zero on-hand |
| Open-ended advisory | **~12s** | e.g. prioritization across low/out stock |

| Improvement | Mechanism |
|-------------|-----------|
| **~60–67% lower latency** vs CPU baseline | GPU-first LiteRT-LM with CPU fallback |
| **~2.5–3× faster** (same device) | Combined with selective `query_products`, field glossary, no extra Gemma on safety success path |

Local chart source (not rendered on GitHub README): [docs/architecture/inference-performance.mmd](./docs/architecture/inference-performance.mmd)

## Build & run

From the repository root:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Install: `app/build/outputs/apk/debug/app-debug.apk` or `./gradlew installDebug`

Open in Android Studio: sync Gradle, run on API 26+. Microphone permission from the Assistant screen when using voice.

## Models (not in git)

Import on device via **Settings → Model Manager**. See [GEMMA_MODEL_PREP.md](./GEMMA_MODEL_PREP.md) and [WHISPER_MODEL_PREP.md](./WHISPER_MODEL_PREP.md).

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
| [docs/architecture/](./docs/architecture/) | Mermaid diagram sources |
| [GEMMA_MODEL_PREP.md](./GEMMA_MODEL_PREP.md) | LiteRT artifact selection |
| [BUILD_PROGRESS.md](./BUILD_PROGRESS.md) | Milestone log |
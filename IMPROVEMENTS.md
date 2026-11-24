# StockPilot AI — evolution & improvements

Evolution summary: **where the project started**, **what we changed**, and **measured outcomes** on a real device (OnePlus 10 Pro, Snapdragon 8 Gen 1).

## Problem statement

Build an **offline-first** retail inventory app with an on-device **agent** that answers natural-language questions over a live SQLite/Room catalog—without cloud APIs, without hidden regex “fake AI,” and with honest provenance when the model is unavailable.

---

## Before (baseline pain points)

| Area | Earlier behavior | Impact |
|------|------------------|--------|
| **Inference backend** | LiteRT-LM **`Backend.CPU()` only** (XNNPACK) | ~**30s** end-to-end assistant replies on device (user-measured) |
| **Data → model** | Full `full_inventory_read` every turn + vague column names (`stock`, `target`, `status`) | Large prompts; Gemma confused **on-hand qty** vs **target shelf** vs **availability** |
| **Prompt shaping** | Keyword filter dropped non–low-stock rows when message contained “stock” | Wrong SKUs in context (e.g. LOW item called “out of stock”) |
| **Agent shape** | Single narrator pass over fat JSON, or removed planner | Either slow/hallucination-prone or no structured fetch |
| **Chat UI** | Full message list from DB; timeline rebuilt on every keystroke | Janky scroll; heavy recomposition |
| **Legacy assistant** | Demo intent parser + fixed command executor (removed) | Not a real agent; misleading for demos |

---

## After (what we implemented)

### 1. Inference — GPU with safe fallback

- **Try `Backend.GPU()` first**, fall back to **`Backend.CPU()`** on the **same** imported `gemma-4-E2B-it.litertlm`.
- **Model Manager** shows active backend: `GPU (Adreno)` or `CPU (XNNPACK)`.
- **Outcome:** same question class dropped from **~30s → ~10s** wall time (user-measured after deploy).

See: `GemmaInferenceEngine.kt`, `GEMMA_MODEL_PREP.md`.

### 2. Agent — teach Gemma *where* data lives, then query, then answer

**No new fixed Q&A routes.** Generic read-only tools:

| Tool | Role |
|------|------|
| `inventory_summary` | Aggregates, `highest_quantity_*`, `zero_on_hand_*`, glossary |
| `query_products` | Filter / sort / limit on Room products (`on_hand_qty_eq`, `availability`, etc.) |
| `full_inventory_read` | Full catalog only when the plan requires it |
| `compute_reorder` | Trusted arithmetic only |

**Flow per user message:**

1. **Planner Gemma** → JSON `tool_calls` only (`AgentPlanParser`).
2. **Kotlin executes** tools against Room (no SQL from the model).
3. **Narrator Gemma** → reply from `FACTS_JSON` + **`field_glossary`** (plain English: quantity on hand vs target stock vs OUT/LOW).

See: `InventorySchemaForModel.kt`, `InventoryProductQuery.kt`, `InventoryAgentOrchestrator.kt`, `InventoryFieldGlossary.kt`.

### Open-ended questions (not fixed intents)

The assistant is **not** limited to lookup templates. Natural, advisory prompts work on the same pipeline:

| Example user prompt | What happens (no hard-coded route) |
|---------------------|-------------------------------------|
| *“What’s the highest quantity on hand?”* | Planner → `inventory_summary` and/or `query_products` (sort `on_hand_qty` desc) → narrator cites facts |
| *“What’s out of stock with zero quantity?”* | Planner → `query_products` (`on_hand_qty_eq: 0` or `availability: OUT`) → narrator lists zero-qty SKUs only |
| *“What’s the next best thing to do?”* | Planner → summary + rows needing attention (low/out, suggested reorder) → narrator prioritizes actions from live data |

These are **arbitrary-language** questions: Gemma chooses tools from the schema, Kotlin fetches, Gemma explains. Reply shows **on-device Gemma** provenance and **“Responded in Xs”** in chat.

**Measured on device (GPU, OnePlus 10 Pro):**

| Prompt style | Typical wall time |
|--------------|-------------------|
| Focused lookup (highest / zero qty) | **~10s** |
| Open-ended ops advice (*“next best thing to do”*) | **~12s** |
| Same workload on **CPU-only** (before GPU fallback) | **~30s** |

### 3. Data semantics (fix “in stock” confusion)

- `product_line_header`: `on_hand_qty`, `target_shelf_qty`, `availability` (OK/LOW/OUT).
- Precomputed slices: **`zero_on_hand_product_lines`**, **`highest_quantity_on_hand`** — data hints, not intent handlers.
- **Removed** keyword-based row filtering before narration.

### 4. Chat UX & performance

- **Pagination:** `observeLastN(limit)` (+20 when scrolling to top).
- **Isolated** `ChatMessageList`; timeline `stateIn` decoupled from typing.
- **IME-safe** scroll; absolute bottom on focus.
- **Response timer:** live elapsed while working + **“Responded in Xs”** persisted on assistant bubbles (Room v4).

### 5. Product & trust UX

- WhatsApp-style chat persistence, date headers, product link rows (name + SKU).
- **Provenance** labels (on-device Gemma / facts-only / unavailable).
- Custom launcher icon; edge-to-edge Compose UI.

### 6. Removed legacy paths

- `DemoIntentParser`, `InventoryCommandExecutor`, routing quick-prompt chips, upfront Gemma skip for stock questions.
- Safety: deterministic `ResponseGuardrails`; **no third Gemma pass** on the hot path (`AgentSafetyReview` short-circuit).

---

## Architecture today (overview)

```text
User message
  → Gemma planner (tool_calls JSON)
  → InventoryToolRunner (Room read-only)
  → FACTS_JSON + field_glossary
  → Gemma narrator
  → Guardrails → chat + provenance + response_duration_ms
```

**Stack:** Kotlin, Compose, Room, Hilt, LiteRT-LM (Gemma 4 E2B IT), Whisper Tiny.en, on-device only.

---

## How to verify (demo script)

1. Model Manager: Gemma **Ready (GPU (Adreno))**; import `gemma-4-E2B-it.litertlm`.
2. **Structured:** *“highest quantity on hand”* / *“zero quantity out of stock”* — answers grounded in `on_hand_qty`, not LOW SKUs.
3. **Open-ended:** *“What’s the next best thing to do?”* — actionable reply from summary + query results (~**12s**, **Responded in …** on bubble).
4. Compare GPU vs CPU in Model Manager if needed; long chat pagination and typing smoothness.

---

## Tests

`./gradlew testDebugUnitTest` — agent parser, guardrails, query filter/sort, reorder math, chat timing format, tool runner glossary fields.

---

## Future (optional, not required for demo)

- Single-call agent (plan+answer) for even lower latency on GPU.
- Planner logging in debug builds.
- Stricter import validation per LiteRT variant.

---

## Related docs

| File | Contents |
|------|----------|
| `README.md` | Build, high-level architecture |
| `HANDOFF.md` | Feature checklist, import steps |
| `GEMMA_MODEL_PREP.md` | HF artifact choice, SHA, device notes |
| `WHISPER_MODEL_PREP.md` | Voice model import |
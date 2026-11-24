# StockPilot AI — Handoff

> **Evolution & benchmarks:** see [IMPROVEMENTS.md](./IMPROVEMENTS.md) for before/after and measured latency (GPU ~10s vs CPU ~30s).

## What works today

- **Inventory dashboard** — 30 seeded retail SKUs, KPIs, search, low/out filters, edit dialog.
- **Room persistence** — stock edits survive process death.
- **Inventory Assistant** — `InventoryAgentOrchestrator`: Gemma **planner** (`tool_calls`) → `InventoryToolRunner` → Gemma **narrator** → `ResponseGuardrails`. Provenance + **Responded in Xs** on replies.
- **No demo command path** — legacy intent parser / command executor removed.
- **Voice** — Whisper when **Ready**; honest errors when not loaded.
- **Model Manager** — Gemma + Whisper import; **GPU (Adreno) then CPU** for Gemma; backend label when **Ready**.
- **Chat** — persisted messages, pagination, product link cards, IME-safe scroll.

## Assistant pipeline (Gemma Ready)

1. **Planner:** JSON `tool_calls` (`inventory_summary`, `query_products`, `full_inventory_read`, `compute_reorder`).
2. **Execute:** read-only Room tools; `field_glossary` + `product_lines` in facts.
3. **Narrate:** natural language from `FACTS_JSON` only.
4. **Guardrails:** deterministic mutation/SQL checks (no extra Gemma pass on success path).

If Gemma is missing or fails: explicit fallback messaging (optional facts-only low/out list on failure only).

## How to import models (on device)

1. Transfer weights to the phone (Downloads / USB) — **app does not download models**.
2. **Settings → Model Manager** → Import → pick file.
3. Wait for **Ready** — Gemma should show **`GPU (Adreno)`** or **`CPU (XNNPACK)`**.
4. **Gemma:** `gemma-4-E2B-it.litertlm` (see `GEMMA_MODEL_PREP.md`).
5. **Whisper:** `ggml-tiny.en.bin` (see `WHISPER_MODEL_PREP.md`).

Storage: `filesDir/models/gemma/`, `filesDir/models/whisper/`.

## Intentional boundaries

| Piece | Why |
|-------|-----|
| Read-only tool surface | No DB writes from the agent |
| Structured `query_products` | Safe filters—not raw SQL from the model |
| Two Gemma calls / message | Plan accuracy vs latency tradeoff (GPU mitigates) |

## Known limitations

- Import validation is filename/size heuristics.
- Planner JSON may be invalid — bootstrap `inventory_summary` + `query_products` if empty.
- Thermal throttling can increase latency on long GPU runs.

## Quick demo script

1. Model Manager → Gemma **Ready (GPU (Adreno))**.
2. **Lookup:** *“What’s the highest quantity on hand?”* → ~10s, provenance + **Responded in …**.
3. **Open-ended:** *“What’s the next best thing to do?”* → prioritization from live low/out/reorder data, ~**12s** on GPU (no scripted command).
4. **Precision:** zero on-hand / out of stock — only `on_hand_qty = 0` SKUs, not LOW-with-stock.
5. Optional: edit a SKU in inventory, voice with Whisper **Ready**, airplane mode.
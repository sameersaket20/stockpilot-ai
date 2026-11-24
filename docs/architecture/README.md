# Architecture diagrams (Mermaid)

The **[main README](../../README.md)** embeds these diagrams natively. Edit the `.mmd` files here, then copy any changes into the matching ` ```mermaid ` blocks in `README.md` (keep them in sync).

| Source | README section |
|--------|----------------|
| [system-architecture.mmd](./system-architecture.mmd) | Architecture |
| [ai-request-lifecycle.mmd](./ai-request-lifecycle.mmd) | AI request lifecycle |
| [inference-performance.mmd](./inference-performance.mmd) | Optional local chart preview; README uses a Markdown table for GitHub compatibility |

## Code alignment

- **One** `GemmaInferenceEngine` / `gemma-4-E2B-it.litertlm` — planner and narrator each call `complete()`.
- **Whisper** (`RoutingSpeechEngine` → `WhisperSpeechEngine`) is on the voice path in `AssistantViewModel`, not inside `InventoryAgentOrchestrator`.
- **`AssistantOrchestrator`** only forwards to **`InventoryAgentOrchestrator`**.
- **`AgentSafetyReview.reviewReadOnly`** does not call Gemma on the success path.
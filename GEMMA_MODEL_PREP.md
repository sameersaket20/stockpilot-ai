# Gemma 4 E2B — model preparation (OnePlus 10 Pro)

**Date:** 2025-11-20  
**Device:** OnePlus NE2211 (`taro` / Snapdragon 8 Gen 1), API 36, ADB `f0cc7817`

## 1. LiteRT-LM in StockPilot AI (current)

| Item | Status |
|------|--------|
| LiteRT-LM Kotlin/Android dependency | **Integrated** (`litertlm-android`) |
| `GemmaInferenceEngine` | **Implemented** — `complete()` for agent planner/narrator/review |
| Assistant path | **`InventoryAgentOrchestrator`** — no demo command fallback |
| Model Manager | Import + init → `ModelStatus.Ready` when load succeeds |

See [LiteRT-LM Kotlin guide](https://ai.google.dev/edge/litert-lm/overview) for upstream API changes.

## 2. Hugging Face repo variants (inspected)

Repo: [litert-community/gemma-4-E2B-it-litert-lm](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm)

| File | ~Size | Target |
|------|------:|--------|
| **`gemma-4-E2B-it.litertlm`** | **2,588,147,712 B** | **General Android CPU (XNNPACK)** — **selected** |
| `gemma-4-E2B-it-gpu.litertlm` | 2.34 GB | Web / WebGPU-optimized (not primary for native Android CPU) |
| `gemma-4-E2B-it_Google_Tensor_G5/G6.litertlm` | 3.2–3.5 GB | Google Tensor only — **not used** |
| `gemma-4-E2B-it_Qualcomm_QCS8275.litertlm` | 2.72 GB | QCS8275 / Ventuno Q NPU hackathon — **not SD 8 Gen 1 phone** |
| `gemma-4-E2B-it_NPU_Uncompiled.litertlm` | 2.61 GB | NPU uncompiled — no documented match for OnePlus 10 Pro |
| Intel LNL/PTL | ~2.9 GB | Desktop — **not used** |

Official README benchmarks Android **CPU/GPU** at **~2583 MB on disk** — matches `gemma-4-E2B-it.litertlm`.

## 3. Selected artifact

- **Filename:** `gemma-4-E2B-it.litertlm`
- **Instruction-tuned:** yes (`-it-` in name; base card [google/gemma-4-E2B-it](https://huggingface.co/google/gemma-4-E2B-it))
- **Backend:** StockPilot tries **GPU (Adreno) first**, then **CPU (XNNPACK)** on the same `gemma-4-E2B-it.litertlm`. On OnePlus NE2211, GPU cut end-to-end assistant time from **~30s → ~10s** (user-measured). Model Manager shows which backend initialized.

## 4. Local download (gitignored)

- **Path:** `SamInference/models/gemma-4-E2B-it.litertlm`
- **Size:** 2,588,147,712 bytes (verified)
- **SHA-256:** `181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c` (matches Hugging Face LFS metadata `oid`)
- **xetHash (HF metadata):** `ee3c29acd58e68bea04006a144cd2e40b3b34dcf5c08200a013744c518b15115`
- **Auth:** Download succeeded **without** HF token for this community repo file; accept upstream Gemma license on Hugging Face if you clone or use gated assets later.

## 5. Phone copy (ADB — does not register in app)

**2025-11-20 status:** OnePlus was connected during planning (`f0cc7817`) but **disconnected before ADB push finished** (`adb: device not found`). Reconnect USB debugging and run:

```bash
adb push models/gemma-4-E2B-it.litertlm /sdcard/Download/gemma-4-E2B-it.litertlm
```

Target on device: **`/sdcard/Download/gemma-4-E2B-it.litertlm`**

**Register in StockPilot:**

1. Install/run `app-debug.apk`.
2. **Settings → Model Manager → Gemma 4 E2B → Import**.
3. Pick **Downloads → `gemma-4-E2B-it.litertlm`** (SAF).
4. Status should become **Imported (unverified)** — not **Ready** until LiteRT-LM runtime loads the file.

ADB push **does not** update app storage or manifest; import step is required.

## 6. Optional: `chat_template.jinja`

Bundled in the HF repo for prompt templating; **not** copied yet. LiteRT-LM integration may require it — add when implementing `GemmaInferenceEngine`.

## 7. On-device validation (your hardware)

1. Import via Model Manager and confirm **Ready** on NE2211.
2. Exercise assistant planner + narrator latency on CPU; tune temperature / retries if needed.
3. Confirm safety review pass does not regress acceptable inventory answers.
4. RAM ~12 GB; expect ~1.7 GB+ peak CPU memory per upstream README on comparable phones.

## 8. Claim status

| Claim | Allowed? |
|-------|----------|
| Model file downloaded & checksum OK | **Yes** |
| Model on phone in Downloads | **After push completes** |
| Model Manager **Imported** | **After manual SAF import** |
| Gemma **inference works** in StockPilot | **No** — runtime not integrated |
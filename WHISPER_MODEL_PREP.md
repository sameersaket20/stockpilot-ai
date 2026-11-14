# Whisper Tiny.en model prep

## File

- **Name:** `ggml-tiny.en.bin`
- **Source:** [Hugging Face — ggerganov/whisper.cpp](https://huggingface.co/ggerganov/whisper.cpp/tree/main) (tiny.en GGML)
- **Size:** ~75 MB (varies by quant)
- **On device:** import via **Settings → Model Manager → Whisper Tiny.en → Import**

The app uses **whisper.cpp** (`dev.ffmpegkit-maintained:whisper-android`, **arm64-v8a**). Recordings are **16 kHz mono WAV** for transcription.

## Not bundled

Model weights are **not** in the APK or git. Keep copies under gitignored `models/` on your dev machine if you like.

## After import

Status should move **Loading → Ready**. Then use the **mic** on the Inventory Assistant (push to talk).
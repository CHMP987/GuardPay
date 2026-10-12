# Spike B harness

A standalone Android app that runs Gemma 4 E2B through LiteRT-LM 0.17.1 on a phone and logs the numbers behind `evidence/ai/benchmark.md`. It is outside the MVP build: it uses Kotlin 2.4.0, which `litertlm-android` 0.17.1 needs through kotlin-reflect, while `app/` stays on 2.2.20. Nothing in `app/shared` depends on it.

Build it from the repo root. Copy the root `local.properties` here first, since it is gitignored:

```bash
./gradlew -p spikes/ai-bench assembleDebug
```

To run it:

1. Install the APK with `adb install -r -t`.
2. Push `gemma-4-E2B-it.litertlm` (from HF `litert-community`, about 2.6 GB, not in the repo) to `/sdcard/Android/data/com.guardpay.aibench/files/`.
3. Start a run:

   ```bash
   adb shell am start -n com.guardpay.aibench/.BenchActivity --es backend gpu
   ```

   Use `backend cpu` for the CPU run.
4. When logcat tag `AIBENCH` shows `bg_window`, press HOME. The last three runs then happen in the background.
5. The run is done at `finished`. Pull `bench-<backend>-<ms>.jsonl` from the same directory.

On Git Bash, set `MSYS_NO_PATHCONV=1` first so `/sdcard` paths are not rewritten. The ten messages in `Messages.kt` are SIMULATED. `Parser.kt` copies the acceptance rule of `AnalysisParser`.

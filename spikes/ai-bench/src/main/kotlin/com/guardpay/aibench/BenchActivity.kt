package com.guardpay.aibench

import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.os.Debug
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.widget.TextView
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File
import kotlin.concurrent.thread

/**
 * Spike B harness. Started by adb:
 *   am start -n com.guardpay.aibench/.BenchActivity --es backend cpu|gpu
 * Model: <external files>/gemma-4-E2B-it.litertlm. Writes one JSON line per event to
 * <external files>/bench-<backend>-<time>.jsonl and logcat tag AIBENCH.
 * Steps: cold load (empty cache dir), close, warm load, 10 runs in the foreground,
 * an 8 s bg_window (the host presses HOME), then 3 runs while in the background.
 */
@OptIn(ExperimentalApi::class)
class BenchActivity : Activity() {
    @Volatile private var foreground = false
    private lateinit var view: TextView
    private lateinit var out: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = TextView(this).apply { textSize = 13f; setPadding(32, 64, 32, 32) }
        setContentView(view)
        val backend = intent.getStringExtra("backend") ?: "cpu"
        val dir = getExternalFilesDir(null)!!
        out = File(dir, "bench-$backend-${System.currentTimeMillis()}.jsonl")
        thread(name = "bench") {
            try {
                run(backend, File(dir, "gemma-4-E2B-it.litertlm"))
            } catch (t: Throwable) {
                emit("error", "type" to t.javaClass.name, "message" to (t.message ?: ""))
            }
            emit("finished")
        }
    }

    override fun onResume() { super.onResume(); foreground = true }
    override fun onPause() { foreground = false; super.onPause() }

    private fun run(backendName: String, model: File) {
        emit(
            "device",
            "backend" to backendName,
            "modelBytes" to model.length(),
            "memAvailableKb" to meminfo("MemAvailable"),
            "memTotalKb" to meminfo("MemTotal"),
        )
        emit("before", *conditions())

        ExperimentalFlags.enableBenchmark = true
        val cache = File(cacheDir, "litertlm").apply { deleteRecursively(); mkdirs() }
        fun backend(): Backend = if (backendName == "gpu") Backend.GPU() else Backend.CPU()
        fun config() = EngineConfig(modelPath = model.path, backend = backend(), cacheDir = cache.path)

        // Cold: first load in this process, empty cache dir. The OS page cache is not
        // dropped (no root), so a file read earlier may still be in RAM.
        var t = SystemClock.elapsedRealtime()
        Engine(config()).also { it.initialize() }.close()
        emit("load", "kind" to "cold", "ms" to SystemClock.elapsedRealtime() - t, *memory())

        t = SystemClock.elapsedRealtime()
        val engine = Engine(config())
        engine.initialize()
        emit("load", "kind" to "warm", "ms" to SystemClock.elapsedRealtime() - t, *memory())

        engine.use {
            MESSAGES.forEachIndexed { i, msg -> analyze(it, i + 1, msg, "fg") }
            emit("after10", *conditions())
            emit("bg_window")
            Thread.sleep(8_000)
            for (i in 1..3) analyze(it, i, MESSAGES[i - 1], "bg")
        }
        emit("end", *conditions(), *memory())
    }

    private fun analyze(engine: Engine, n: Int, message: String, phase: String) {
        val conversation = engine.createConversation(
            ConversationConfig(
                systemInstruction = Contents.of(SYSTEM_PROMPT),
                samplerConfig = SamplerConfig(topK = 1, topP = 1.0, temperature = 1.0, seed = 0),
                maxOutputToken = 256,
                thinkingConfig = ThinkingConfig(enableThinking = false),
            )
        )
        conversation.use { c ->
            val text = StringBuilder()
            val start = SystemClock.elapsedRealtime()
            var first = -1L
            runBlocking {
                c.sendMessageAsync(message).collect { m ->
                    if (first < 0) first = SystemClock.elapsedRealtime() - start
                    m.contents.contents.forEach { part -> if (part is Content.Text) text.append(part.text) }
                }
            }
            val total = SystemClock.elapsedRealtime() - start
            val b = c.getBenchmarkInfo()
            val raw = text.toString()
            val suggestion = Parser.parse(raw)
            emit(
                "run",
                "phase" to phase, "n" to n, "foregroundAtEnd" to foreground,
                "firstChunkMs" to first, "totalMs" to total,
                "ttftS" to b.timeToFirstTokenInSecond,
                "prefillTokens" to b.lastPrefillTokenCount, "prefillTokS" to b.lastPrefillTokensPerSecond,
                "decodeTokens" to b.lastDecodeTokenCount, "decodeTokS" to b.lastDecodeTokensPerSecond,
                "parseable" to (suggestion != null), "suggestion" to (suggestion ?: ""),
                "raw" to raw,
                *memory(),
            )
        }
    }

    private fun conditions(): Array<Pair<String, Any>> {
        val bm = getSystemService(BatteryManager::class.java)
        val sticky = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return arrayOf(
            "batteryPct" to bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY),
            "chargeCounterUah" to bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER),
            "plugged" to (sticky?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1),
            "batteryTempC" to (sticky?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0,
            "thermalStatus" to getSystemService(PowerManager::class.java).currentThermalStatus,
            "uptimeMs" to SystemClock.elapsedRealtime(),
        )
    }

    /** VmHWM is the peak resident set of this process since it started. */
    private fun memory(): Array<Pair<String, Any>> {
        val status = File("/proc/self/status").readLines()
        fun kb(key: String) = status.firstOrNull { it.startsWith("$key:") }
            ?.substringAfter(':')?.trim()?.substringBefore(' ')?.toLongOrNull() ?: -1
        return arrayOf(
            "vmHwmKb" to kb("VmHWM"), "vmRssKb" to kb("VmRSS"),
            "pssKb" to Debug.getPss(), "memAvailableKb" to meminfo("MemAvailable"),
        )
    }

    private fun meminfo(key: String): Long = File("/proc/meminfo").readLines()
        .first { it.startsWith("$key:") }.substringAfter(':').trim().substringBefore(' ').toLong()

    @Synchronized
    private fun emit(event: String, vararg fields: Pair<String, Any>) {
        val obj = JsonObject(
            mapOf("event" to JsonPrimitive(event), "t" to JsonPrimitive(System.currentTimeMillis())) +
                fields.associate { (k, v) ->
                    k to when (v) {
                        is Number -> JsonPrimitive(v)
                        is Boolean -> JsonPrimitive(v)
                        else -> JsonPrimitive(v.toString())
                    }
                }
        ).toString()
        out.appendText(obj + "\n")
        Log.i("AIBENCH", obj)
        runOnUiThread { view.append("$event\n") }
    }
}

package com.guardpay.shared.ai

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * The Gemma 4 E2B file on this phone. It is never in the APK: the owner downloads it
 * from the app. The download only fetches the model; nothing about the owner is sent.
 *
 * The URL is pinned to one Hugging Face commit and the result must match [SIZE] and
 * [SHA256] before it is used. A partial download resumes from `<name>.part`.
 * A file already in place with the right size (e.g. pushed by adb) counts as present.
 */
class GemmaModelFile(context: Context) {
    private val dir: File = context.getExternalFilesDir(null) ?: context.filesDir
    val file = File(dir, NAME)
    private val part = File(dir, "$NAME.part")

    private val _status = MutableStateFlow(initial())
    val status: StateFlow<ModelStatus> = _status.asStateFlow()

    private fun initial(): ModelStatus =
        if (file.length() == SIZE) ModelStatus.OnDevice else ModelStatus.NotDownloaded(SIZE)

    /** Blocking. Downloads, checks and moves the model into place; never throws. */
    fun download(): Boolean {
        if (file.length() == SIZE) return true.also { _status.value = ModelStatus.OnDevice }
        return try {
            if (part.length() != SIZE) fetch()
            if (part.length() != SIZE || sha256(part) != SHA256) {
                part.delete()
                false
            } else {
                file.delete()
                part.renameTo(file)
            }
        } catch (e: Exception) {
            false
        }.also { ok -> _status.value = if (ok) ModelStatus.OnDevice else ModelStatus.DownloadFailed }
    }

    private fun fetch() {
        var have = part.length()
        if (have > SIZE) have = 0L.also { part.delete() }
        if (dir.usableSpace < SIZE - have) error("no space")
        _status.value = ModelStatus.Downloading(have, SIZE)
        val conn = open(URL(URL_PINNED), have)
        try {
            if (conn.responseCode == HttpURLConnection.HTTP_OK) have = 0L // server ignored Range
            else if (conn.responseCode != HttpURLConnection.HTTP_PARTIAL) error("HTTP ${conn.responseCode}")
            conn.inputStream.use { input ->
                FileOutputStream(part, have > 0).use { out ->
                    val buf = ByteArray(1 shl 16)
                    var lastShown = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        have += n
                        if (have - lastShown >= SIZE / 200) {
                            lastShown = have
                            _status.value = ModelStatus.Downloading(have, SIZE)
                        }
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
    }

    /** Follows Hugging Face's redirect to its CDN by hand, so the Range header survives. */
    private fun open(start: URL, from: Long): HttpURLConnection {
        var url = start
        repeat(5) {
            val c = url.openConnection() as HttpURLConnection
            c.instanceFollowRedirects = false
            c.connectTimeout = 20_000
            c.readTimeout = 60_000
            if (from > 0) c.setRequestProperty("Range", "bytes=$from-")
            if (c.responseCode !in 300..399) return c
            val next = c.getHeaderField("Location") ?: error("redirect without Location")
            c.disconnect()
            url = URL(url, next)
            if (url.protocol != "https") error("non-https redirect")
        }
        error("too many redirects")
    }

    private fun sha256(f: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { input ->
            val buf = ByteArray(1 shl 20)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val NAME = "gemma-4-E2B-it.litertlm"
        const val SIZE = 2_588_147_712L
        const val SHA256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c"

        /** litert-community/gemma-4-E2B-it-litert-lm at commit b3ca0d2f. Public, not gated. */
        const val URL_PINNED = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/" +
            "resolve/b3ca0d2f076785a8f4b2219ddbd2bdb99954eae1/gemma-4-E2B-it.litertlm"
    }
}

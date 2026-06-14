package ai.sarj.agentsim.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.HandlerThread
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

enum class Sfx { TAP, MESSAGE_SEND, MESSAGE_RECEIVE, STAR_POP, TOKEN_EARN, COMBO_UP, LEVEL_UP, STRIKE_BUZZ, SARJ_WHOOSH, ERROR }

/**
 * Procedural SFX — every sound is synthesized to a PCM buffer in code, so the app ships with ZERO
 * audio asset files. Synthesis + playback run on a dedicated worker thread (never the main thread,
 * never fighting the on-device LLM). Real .ogg can drop in behind play() later.
 */
class SoundManager {

    @Volatile var muted = false

    private val sampleRate = 22050
    private val thread = HandlerThread("sfx").apply { start() }
    private val handler = Handler(thread.looper)
    private val cache = HashMap<String, ShortArray>()

    fun play(sfx: Sfx, variant: Int = 0) {
        if (muted) return
        handler.post {
            val buf = cache.getOrPut("${sfx.name}_$variant") { render(sfx, variant) }
            writeBuffer(buf)
        }
    }

    private fun render(sfx: Sfx, variant: Int): ShortArray = when (sfx) {
        Sfx.TAP -> tone(900.0, 25, 900.0, releaseMs = 25)
        Sfx.MESSAGE_SEND -> tone(660.0, 70, 880.0)
        Sfx.MESSAGE_RECEIVE -> tone(700.0, 90, 520.0, releaseMs = 55)
        Sfx.STAR_POP -> tone(660.0 * 2.0.pow(variant / 12.0), 120, 660.0 * 2.0.pow(variant / 12.0), partials = listOf(1.0, 2.0, 3.0))
        Sfx.TOKEN_EARN -> tone(1175.0, 110, 1175.0, partials = listOf(1.0, 2.0))
        Sfx.COMBO_UP -> seq(listOf(784.0 to 60, 1046.0 to 60))
        Sfx.LEVEL_UP -> seq(listOf(523.0 to 90, 659.0 to 90, 784.0 to 130))
        Sfx.STRIKE_BUZZ -> square(140.0, 180)
        Sfx.SARJ_WHOOSH -> tone(300.0, 450, 1600.0, wave = 1, releaseMs = 120)
        Sfx.ERROR -> seq(listOf(440.0 to 150, 330.0 to 160))
    }

    private fun tone(
        freq: Double, durMs: Int, sweepTo: Double,
        attackMs: Int = 6, releaseMs: Int = 40, partials: List<Double> = listOf(1.0), wave: Int = 0
    ): ShortArray {
        val n = durMs * sampleRate / 1000
        val out = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / sampleRate
            val frac = i.toDouble() / n
            val f = freq + (sweepTo - freq) * frac
            var s = 0.0
            for (p in partials) {
                val ph = 2 * PI * f * p * t
                s += (if (wave == 1) triangle(ph) else sin(ph)) / partials.size
            }
            out[i] = (s * envelope(i, n, attackMs, releaseMs) * 0.5 * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun square(freq: Double, durMs: Int): ShortArray {
        val n = durMs * sampleRate / 1000
        val out = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / sampleRate
            val s = if (sin(2 * PI * freq * t) >= 0) 1.0 else -1.0
            out[i] = (s * envelope(i, n, 4, 12) * 0.35 * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun seq(notes: List<Pair<Double, Int>>): ShortArray {
        val parts = notes.map { tone(it.first, it.second, it.first) }
        val out = ShortArray(parts.sumOf { it.size })
        var off = 0
        for (p in parts) { p.copyInto(out, off); off += p.size }
        return out
    }

    private fun triangle(phase: Double): Double {
        val x = (phase / (2 * PI)) % 1.0
        return 4.0 * abs(x - 0.5) - 1.0
    }

    private fun envelope(i: Int, n: Int, attackMs: Int, releaseMs: Int): Double {
        val a = (attackMs * sampleRate / 1000).coerceAtLeast(1)
        val r = (releaseMs * sampleRate / 1000).coerceAtLeast(1)
        return when {
            i < a -> i.toDouble() / a
            i > n - r -> exp(-3.0 * (i - (n - r)) / r)
            else -> 1.0
        }
    }

    private fun writeBuffer(buf: ShortArray) {
        val track = runCatching {
            AudioTrack(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
                buf.size * 2,
                AudioTrack.MODE_STATIC,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )
        }.getOrNull() ?: return
        runCatching {
            track.write(buf, 0, buf.size)
            track.play()
            handler.postDelayed({ runCatching { track.stop(); track.release() } }, buf.size * 1000L / sampleRate + 120)
        }
    }

    fun release() {
        runCatching { thread.quitSafely() }
    }
}

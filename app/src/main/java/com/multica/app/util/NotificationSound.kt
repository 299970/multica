package com.multica.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * v0.3.20 老板 2026-06-08 需求：
 *  6. runtimes 上线/离线 → 叮
 *  5. agent 任务开始 → 叮，结束 → 嘟
 *
 * 实现：用 AudioTrack 程序合成短促音（不依赖本地音频文件）
 *  - 叮：1000Hz 正弦波，200ms
 *  - 嘟：500Hz 正弦波，300ms（低沉结束音）
 */
object NotificationSound {
    private const val SAMPLE_RATE = 22050

    fun ding(ctx: Context) = play(ctx, 1000, 200, 0.6)
    fun dong(ctx: Context) = play(ctx, 500, 300, 0.5)

    private fun play(ctx: Context, freqHz: Int, durationMs: Int, volume: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val samples = ShortArray(SAMPLE_RATE * durationMs / 1000)
                for (i in samples.indices) {
                    val t = i.toDouble() / SAMPLE_RATE
                    // 带淡出（避免咔嗒声）
                    val env = (1.0 - t / (durationMs / 1000.0)).coerceIn(0.0, 1.0)
                    samples[i] = (volume * env * Short.MAX_VALUE * Math.sin(2.0 * Math.PI * freqHz * t)).toInt().toShort()
                }
                val track = AudioTrack(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                    samples.size * 2,
                    AudioTrack.MODE_STATIC,
                    AudioManager.AUDIO_SESSION_ID_GENERATE,
                )
                track.write(samples, 0, samples.size)
                track.play()
                // 等播放完再释放
                Thread.sleep(durationMs.toLong() + 50)
                track.stop()
                track.release()
            } catch (e: Throwable) {
                // 播放失败不崩
            }
        }
    }
}

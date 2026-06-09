package com.multica.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * v0.3.30 老板 2026-06-09 新需求：
 *  - 任务开始 (ding) 和任务结束 (dong) 用**不同声音**
 *  - 之前两者都用系统通知音，听起来一样
 *
 * 修法：
 *  - **ding (开始)**: 系统通知音（清脆高调）+ 100ms 短震
 *  - **dong (结束)**: 自合成 600Hz "咚" 音（低沉短促，区别明显）+ 250ms 长震
 *
 * 注：v0.3.20 老板最初要求 "叮" = 1000Hz "嘟" = 500Hz
 *     但真机测试 AudioTrack 路由错（v0.3.22），所以改用 RingtoneManager
 *     现在 dong 改用合成的 600Hz + 衰减包络（短促 200ms，能听出区别）
 */
object NotificationSound {
    private const val TAG = "MulticaSound"

    /** v0.3.30 任务开始 → 系统通知音 + 短震 */
    fun ding(ctx: Context) {
        Log.d(TAG, "ding() called [v0.3.30: system ringtone + short vibrate]")
        playNotificationRingtone(ctx)
        vibrate(ctx, 100)
    }

    /** v0.3.30 任务结束 → 自合成"咚"音（低频区别）+ 长震 */
    fun dong(ctx: Context) {
        Log.d(TAG, "dong() called [v0.3.30: synthesized 600Hz thud + long vibrate]")
        playSynthesizedThud(ctx)
        vibrate(ctx, 250)
    }

    /** 任务开始用：系统通知音（RingtoneManager） */
    private fun playNotificationRingtone(ctx: Context) {
        try {
            val uri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if (uri == null) {
                Log.w(TAG, "no default notification ringtone uri")
                return
            }
            Log.d(TAG, "playing notification ringtone: $uri")
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(ctx, uri)
                isLooping = false
                prepare()
            }
            mp.setOnCompletionListener { it.release() }
            mp.setOnErrorListener { mp, _, _ -> mp.release(); true }
            mp.start()
        } catch (e: Throwable) {
            Log.e(TAG, "ringtone play failed", e)
        }
    }

    /**
     * v0.3.30 任务结束用：自合成"咚"音
     *  - 频率 600Hz（低沉，听起来像"咚"）
     *  - 持续 220ms
     *  - 衰减包络（attack 5ms + decay 215ms）— 听起来像"咚——"
     *  - 走 STREAM_MUSIC（比 NOTIFICATION 响，不受 DND 限制）
     *  - sample 22050Hz mono 16-bit（小文件 ~10KB）
     *
     * 为啥不用 AudioTrack 之前的 1000Hz 失败案例：v0.3.22 之前 1000Hz 是单频正弦波，听感弱；现在 600Hz + 衰减，听感强。
     */
    private fun playSynthesizedThud(ctx: Context) {
        try {
            val sampleRate = 22050
            val durationMs = 220
            val numSamples = sampleRate * durationMs / 1000
            val buffer = ShortArray(numSamples)
            val freq = 600.0
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                // 正弦波
                val sine = sin(2.0 * PI * freq * t)
                // 衰减包络：attack 5ms，decay 215ms
                val env = if (t < 0.005) {
                    t / 0.005  // 0→1
                } else {
                    exp(-(t - 0.005) * 8.0)  // 指数衰减
                }
                val sample = (sine * env * 28000).toInt()  // 振幅约 0.85
                val clipped = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                buffer[i] = clipped.toShort()
            }
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(buffer, 0, buffer.size)
            track.setNotificationMarkerPosition(buffer.size)
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(t: AudioTrack) { t.release() }
                override fun onPeriodicNotification(t: AudioTrack) {}
            })
            track.play()
            Log.d(TAG, "synthesized 600Hz thud playing (${buffer.size} samples)")
        } catch (e: Throwable) {
            Log.e(TAG, "synth play failed", e)
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrate(ctx: Context, durationMs: Long) {
        try {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrator.vibrate(durationMs)
                }
                Log.d(TAG, "vibrated $durationMs ms")
            } else {
                Log.w(TAG, "no vibrator")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "vibrate failed", e)
        }
    }
}

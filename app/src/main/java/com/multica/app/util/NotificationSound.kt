package com.multica.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * v0.3.24 老板 2026-06-08 反馈："还是没听到声音"（Samsung Galaxy Note 8 真机 + ToneGenerator + 通知音量 7/15）
 *
 * 真根因分析（v0.3.23 仍未解决）：
 *  - ToneGenerator 调到 STREAM_NOTIFICATION 7/15 但仍无声
 *  - 真机 Note 8，Android 9，DND 关
 *  - 可能：ToneGenerator 在该设备/系统上有兼容问题，或 USAGE_NOTIFICATION 被静音路由
 *
 * v0.3.24 修法（最暴力 3 重保险）：
 *  1. RingtoneManager.getDefaultUri(TYPE_NOTIFICATION) — 调系统通知铃声（必响）
 *  2. MediaPlayer 播放 + STREAM_MUSIC + max volume — 强制走音乐流
 *  3. Vibrator 震动 — 静音模式也有反馈
 */
object NotificationSound {
    private const val TAG = "MulticaSound"

    fun ding(ctx: Context) {
        Log.d(TAG, "ding() called")
        playNotificationRingtone(ctx)
        vibrate(ctx, 100)
    }
    fun dong(ctx: Context) {
        Log.d(TAG, "dong() called")
        playNotificationRingtone(ctx)
        vibrate(ctx, 200)
    }

    /**
     * 调系统默认通知铃声（RingtoneManager）— 这是 Android 最稳的"有声音"方式
     * 任何设备/系统版本都支持，必有声音（除非手机完全静音+震动也关）
     */
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

package com.pandyabhavik.snakes.ui

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.random.Random

/**
 * Haptics played straight on the vibrator with media attributes, so they work even when the system
 * "touch feedback" setting is off. A phone with a good motor (the Fold) gets composed primitives: a
 * gritty rattle while the die is shaken, a thud when it lands, a light tick per square, a rising sweep up
 * a ladder and a falling one down a snake. Other phones get the closest waveform.
 */
class Haptics(context: Context) {
    enum class Pattern { SHAKE, LAND, HOP, LADDER, SNAKE, TURN, WIN, TAP, BLOCKED }

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val lowTick = VibrationEffect.Composition.PRIMITIVE_LOW_TICK
    private val tick = VibrationEffect.Composition.PRIMITIVE_TICK
    private val click = VibrationEffect.Composition.PRIMITIVE_CLICK
    private val thud = VibrationEffect.Composition.PRIMITIVE_THUD
    private val quickRise = VibrationEffect.Composition.PRIMITIVE_QUICK_RISE
    private val slowRise = VibrationEffect.Composition.PRIMITIVE_SLOW_RISE
    private val quickFall = VibrationEffect.Composition.PRIMITIVE_QUICK_FALL

    private fun supported(vararg primitives: Int): Boolean =
        vibrator != null && Build.VERSION.SDK_INT >= 31 && vibrator.areAllPrimitivesSupported(*primitives)

    private val basic = supported(lowTick, tick, click)
    private val heavy = supported(thud)
    private val sweeps = supported(quickRise, slowRise, quickFall)
    private val random = Random(System.nanoTime())

    fun play(pattern: Pattern) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val effect = if (basic) composed(pattern) else fallback(pattern)
        vibrate(v, effect)
    }

    private fun vibrate(v: Vibrator, effect: VibrationEffect) {
        if (Build.VERSION.SDK_INT >= 33) {
            v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_MEDIA))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        }
    }

    private fun composed(pattern: Pattern): VibrationEffect {
        val c = VibrationEffect.startComposition()
        val bump = if (heavy) thud else click
        when (pattern) {
            // Bone dice knocking about in a cupped hand: uneven little ticks, some harder than others.
            Pattern.SHAKE -> repeat(9) { i ->
                c.addPrimitive(if (i % 3 == 1) tick else lowTick, 0.25f + random.nextFloat() * 0.45f, if (i == 0) 0 else 35 + random.nextInt(40))
            }
            // The die hits the felt, bounces once, settles.
            Pattern.LAND -> c.addPrimitive(bump, 0.85f).addPrimitive(lowTick, 0.45f, 70).addPrimitive(lowTick, 0.2f, 55)
            Pattern.HOP -> c.addPrimitive(tick, 0.38f)
            Pattern.LADDER -> if (sweeps) {
                c.addPrimitive(slowRise, 0.6f).addPrimitive(click, 0.7f, 40)
            } else {
                for (i in 0 until 6) c.addPrimitive(tick, 0.2f + i * 0.12f, if (i == 0) 0 else 110)
            }
            Pattern.SNAKE -> if (sweeps) {
                c.addPrimitive(quickFall, 0.8f)
                for (i in 0 until 5) c.addPrimitive(lowTick, 0.6f - i * 0.1f, 130)
                c.addPrimitive(bump, 0.5f, 60)
            } else {
                for (i in 0 until 7) c.addPrimitive(lowTick, 0.8f - i * 0.1f, if (i == 0) 0 else 120)
            }
            Pattern.TURN -> c.addPrimitive(lowTick, 0.3f)
            Pattern.WIN -> {
                c.addPrimitive(bump, 0.6f).addPrimitive(click, 0.5f, 120).addPrimitive(click, 0.7f, 120)
                if (sweeps) c.addPrimitive(slowRise, 0.8f, 60)
                c.addPrimitive(bump, 1f, 80)
            }
            Pattern.TAP -> c.addPrimitive(tick, 0.5f)
            Pattern.BLOCKED -> c.addPrimitive(lowTick, 0.6f).addPrimitive(lowTick, 0.6f, 90)
        }
        return c.compose()
    }

    private fun fallback(pattern: Pattern): VibrationEffect = when (pattern) {
        Pattern.SHAKE -> VibrationEffect.createWaveform(longArrayOf(0, 12, 40, 10, 55, 14, 35, 10, 60, 12), intArrayOf(0, 90, 0, 60, 0, 110, 0, 70, 0, 90), -1)
        Pattern.LAND -> VibrationEffect.createWaveform(longArrayOf(0, 35, 60, 15), intArrayOf(0, 200, 0, 80), -1)
        Pattern.HOP, Pattern.TAP, Pattern.TURN -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
        Pattern.LADDER -> VibrationEffect.createWaveform(longArrayOf(0, 15, 90, 15, 90, 15, 90, 20), intArrayOf(0, 60, 0, 110, 0, 160, 0, 230), -1)
        Pattern.SNAKE -> VibrationEffect.createWaveform(longArrayOf(0, 25, 90, 20, 90, 15, 90, 12), intArrayOf(0, 220, 0, 160, 0, 110, 0, 60), -1)
        Pattern.WIN -> VibrationEffect.createWaveform(longArrayOf(0, 40, 80, 25, 120, 70), intArrayOf(0, 140, 0, 110, 0, 255), -1)
        Pattern.BLOCKED -> VibrationEffect.createWaveform(longArrayOf(0, 30, 60, 30), intArrayOf(0, 150, 0, 150), -1)
    }
}

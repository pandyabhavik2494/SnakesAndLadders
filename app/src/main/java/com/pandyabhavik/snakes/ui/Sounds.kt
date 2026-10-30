package com.pandyabhavik.snakes.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.pandyabhavik.snakes.R
import kotlin.random.Random

enum class Sound(val resId: Int) {
    SHAKE(R.raw.shake),
    LAND(R.raw.land),
    HOP(R.raw.hop),
    LADDER(R.raw.ladder),
    SNAKE(R.raw.snake),
    TURN(R.raw.turn),
    WIN(R.raw.win),
    TAP(R.raw.tap),
    BLOCKED(R.raw.blocked),
}

/** Short game sounds through a SoundPool. Clips load at start; a clip asked for before it is decoded is skipped. */
class Sounds(private val context: Context) {
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val ids = HashMap<Sound, Int>()
    private val ready = HashSet<Int>()
    private val random = Random(System.nanoTime())

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status -> if (status == 0) synchronized(ready) { ready.add(sampleId) } }
        Sound.entries.forEach { load(it) }
    }

    private fun load(sound: Sound): Int = ids.getOrPut(sound) { pool.load(context, sound.resId, 1) }

    /**
     * [rate] shifts the pitch (hops climb a little with each square). Taps and landings get a hair of
     * random variation so a run of them never sounds like one clip on repeat.
     */
    fun play(sound: Sound, rate: Float = 1f, volume: Float = 1f) {
        val id = load(sound)
        if (synchronized(ready) { id !in ready }) return
        val jitter = if (sound == Sound.HOP || sound == Sound.LAND || sound == Sound.TAP) 0.97f + random.nextFloat() * 0.06f else 1f
        pool.play(id, volume, volume, 1, 0, (rate * jitter).coerceIn(0.5f, 2f))
    }

    fun release() = pool.release()
}

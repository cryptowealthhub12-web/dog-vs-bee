package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SoundEngine(private val context: Context) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private var musicJob: Job? = null
  private var sfxMixerJob: Job? = null

  @Volatile var soundEnabled: Boolean = true
  @Volatile var musicEnabled: Boolean = true
  @Volatile var vibrationEnabled: Boolean = true
  @Volatile private var isAppForeground: Boolean = true
  @Volatile private var isBuzzingActive: Boolean = false

  private var lastDrawSoundMs = 0L
  private var lastBounceSoundMs = 0L

  private data class ActiveClip(val samples: ShortArray, var cursor: Int = 0)

  private val sfxLock = Any()
  private val activeClips = mutableListOf<ActiveClip>()
  private val sfxWakeSignal = Channel<Unit>(Channel.CONFLATED)

  private var musicTrack: AudioTrack? = null
  private var sfxTrack: AudioTrack? = null

  private val vibrator: Vibrator? by lazy {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
      vm?.defaultVibrator
    } else {
      @Suppress("DEPRECATION")
      context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
  }

  // Pre-synthesized PCM clips (allocated once, zero per-frame allocations or AudioTrack churn)
  private val musicLoopSamples: ShortArray by lazy { synthesizeCozyPuzzleLoop() }
  private val buzzLoopSamples: ShortArray by lazy { generateBuzzLoop(320, 0.065f) }
  private val clickClip: ShortArray by lazy { generateSweep(520.0, 820.0, 42, 0.16f) }
  private val drawTickClip: ShortArray by lazy { generatePencilScratch(36, 0.085f) }
  private val bounceClip: ShortArray by lazy { generateBoing(95, 0.15f) }
  private val beeSpawnClip: ShortArray by lazy { generateBuzzLoop(190, 0.10f) }
  private val clockNormalClip: ShortArray by lazy { generateTone(880.0, 45, 0.16f, WaveType.TRIANGLE) }
  private val clockUrgentClip: ShortArray by lazy { generateTone(1180.0, 45, 0.18f, WaveType.TRIANGLE) }
  private val coinClip: ShortArray by lazy {
    concatClips(
      generateTone(987.77, 65, 0.17f, WaveType.SINE),
      generateTone(1318.51, 145, 0.19f, WaveType.SINE),
    )
  }
  private val winClip: ShortArray by lazy {
    concatClips(
      generateDogBark(500.0, 0.50f),
      ShortArray(22050 * 25 / 1000),
      generateDogBark(600.0, 0.54f),
      generateTone(523.25, 95, 0.56f, WaveType.TRIANGLE),
      generateTone(659.25, 95, 0.56f, WaveType.TRIANGLE),
      generateTone(783.99, 95, 0.56f, WaveType.TRIANGLE),
      generateTone(1046.50, 130, 0.56f, WaveType.TRIANGLE),
      generateTone(1318.51, 320, 0.58f, WaveType.TRIANGLE),
    )
  }
  private val loseClip: ShortArray by lazy {
    concatClips(
      generateSweep(780.0, 320.0, 120, 0.22f),
      generateSweep(360.0, 290.0, 140, 0.20f),
      generateSweep(280.0, 145.0, 310, 0.22f),
    )
  }

  fun updateSettings(sound: Boolean, music: Boolean, vibration: Boolean) {
    soundEnabled = sound
    vibrationEnabled = vibration
    val wasMusic = musicEnabled
    musicEnabled = music
    if (!sound) {
      stopBeeBuzz()
      synchronized(sfxLock) { activeClips.clear() }
    }
    if (music && !wasMusic && isAppForeground) {
      startMusicLoop()
    } else if (!music) {
      stopMusicLoop()
    }
  }

  fun onAppResume() {
    isAppForeground = true
    if (musicEnabled) startMusicLoop()
  }

  fun onAppPause() {
    isAppForeground = false
    stopMusicLoop()
    stopBeeBuzz()
  }

  fun startMusicLoop() {
    if (!musicEnabled || !isAppForeground) return
    if (musicJob?.isActive == true) return
    musicJob =
      scope.launch {
        val sampleRate = SAMPLE_RATE
        val loop = musicLoopSamples
        val track = getOrCreateMusicTrack(sampleRate) ?: return@launch
        try {
          track.play()
          var offset = 0
          val chunk = 2048
          while (isActive && musicEnabled && isAppForeground) {
            val count = minOf(chunk, loop.size - offset)
            val written = track.write(loop, offset, count)
            if (written <= 0) break
            offset += written
            if (offset >= loop.size) offset = 0
          }
        } catch (_: Exception) {
        } finally {
          try {
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
              track.pause()
              track.flush()
            }
          } catch (_: Exception) {}
        }
      }
  }

  fun stopMusicLoop() {
    musicJob?.cancel()
    musicJob = null
  }

  fun startBeeBuzz() {
    if (!soundEnabled || !isAppForeground) return
    isBuzzingActive = true
    ensureSfxMixerRunning()
    sfxWakeSignal.trySend(Unit)
  }

  fun stopBeeBuzz() {
    isBuzzingActive = false
  }

  fun playClick() {
    if (!soundEnabled) return
    enqueueClip(clickClip)
  }

  fun playDrawTick() {
    if (!soundEnabled) return
    val now = System.currentTimeMillis()
    if (now - lastDrawSoundMs < 55L) return
    lastDrawSoundMs = now
    enqueueClip(drawTickClip)
  }

  fun playBounce() {
    if (!soundEnabled) return
    val now = System.currentTimeMillis()
    if (now - lastBounceSoundMs < 85L) return
    lastBounceSoundMs = now
    enqueueClip(bounceClip)
  }

  fun playBeeBuzz() {
    if (!soundEnabled) return
    enqueueClip(beeSpawnClip)
  }

  fun playClockTick(urgent: Boolean) {
    if (!soundEnabled) return
    enqueueClip(if (urgent) clockUrgentClip else clockNormalClip)
  }

  fun playCoin() {
    if (!soundEnabled) return
    enqueueClip(coinClip)
  }

  fun resumeMusicIfEnabled() {
    if (musicEnabled && isAppForeground) {
      startMusicLoop()
    }
  }

  fun playWin() {
    stopBeeBuzz()
    stopMusicLoop()
    vibrate(55L)
    if (!soundEnabled) return
    synchronized(sfxLock) { activeClips.clear() }
    enqueueClip(winClip)
  }

  fun playLose() {
    stopBeeBuzz()
    vibrate(95L)
    if (!soundEnabled) return
    enqueueClip(loseClip)
  }

  fun vibrate(ms: Long = 25L) {
    if (!vibrationEnabled) return
    try {
      val v = vibrator ?: return
      if (!v.hasVibrator()) return
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
      } else {
        @Suppress("DEPRECATION")
        v.vibrate(ms)
      }
    } catch (_: Exception) {}
  }

  private fun enqueueClip(samples: ShortArray) {
    if (!isAppForeground || !soundEnabled) return
    synchronized(sfxLock) {
      if (activeClips.size >= 6) {
        activeClips.removeAt(0)
      }
      activeClips.add(ActiveClip(samples))
    }
    ensureSfxMixerRunning()
    sfxWakeSignal.trySend(Unit)
  }

  private fun ensureSfxMixerRunning() {
    if (sfxMixerJob?.isActive == true) return
    sfxMixerJob =
      scope.launch {
        val track = getOrCreateSfxTrack(SAMPLE_RATE) ?: return@launch
        val chunkSize = 512
        val mixBuf = ShortArray(chunkSize)
        val accum = IntArray(chunkSize)
        var buzzCursor = 0
        var playing = false

        try {
          while (isActive) {
            val hasWork =
              synchronized(sfxLock) { activeClips.isNotEmpty() } ||
                (isBuzzingActive && soundEnabled && isAppForeground)

            if (!hasWork) {
              if (playing) {
                try {
                  track.pause()
                  track.flush()
                } catch (_: Exception) {}
                playing = false
              }
              buzzCursor = 0
              sfxWakeSignal.receive()
              continue
            }

            if (!playing) {
              track.play()
              playing = true
            }

            accum.fill(0)
            synchronized(sfxLock) {
              val it = activeClips.iterator()
              while (it.hasNext()) {
                val clip = it.next()
                val remaining = clip.samples.size - clip.cursor
                val take = minOf(chunkSize, remaining)
                for (i in 0 until take) {
                  accum[i] += clip.samples[clip.cursor + i].toInt()
                }
                clip.cursor += take
                if (clip.cursor >= clip.samples.size) {
                  it.remove()
                }
              }
            }

            if (isBuzzingActive && soundEnabled && isAppForeground) {
              val buzz = buzzLoopSamples
              for (i in 0 until chunkSize) {
                accum[i] += buzz[buzzCursor].toInt()
                buzzCursor++
                if (buzzCursor >= buzz.size) buzzCursor = 0
              }
            }

            for (i in 0 until chunkSize) {
              mixBuf[i] = accum[i].coerceIn(-32767, 32767).toShort()
            }
            val written = track.write(mixBuf, 0, chunkSize)
            if (written < 0) break
          }
        } catch (_: Exception) {
        } finally {
          try {
            if (playing) {
              track.pause()
              track.flush()
            }
          } catch (_: Exception) {}
        }
      }
  }

  @Synchronized
  private fun getOrCreateMusicTrack(sampleRate: Int): AudioTrack? {
    musicTrack?.let {
      if (it.state == AudioTrack.STATE_INITIALIZED) return it
    }
    return try {
      val minBuf =
        AudioTrack.getMinBufferSize(
          sampleRate,
          AudioFormat.CHANNEL_OUT_MONO,
          AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(4096)
      AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        )
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        )
        .setBufferSizeInBytes(minBuf * 2)
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build()
        .also { musicTrack = it }
    } catch (_: Exception) {
      null
    }
  }

  @Synchronized
  private fun getOrCreateSfxTrack(sampleRate: Int): AudioTrack? {
    sfxTrack?.let {
      if (it.state == AudioTrack.STATE_INITIALIZED) return it
    }
    return try {
      val minBuf =
        AudioTrack.getMinBufferSize(
          sampleRate,
          AudioFormat.CHANNEL_OUT_MONO,
          AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(2048)
      AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        )
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        )
        .setBufferSizeInBytes(minBuf)
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build()
        .also { sfxTrack = it }
    } catch (_: Exception) {
      null
    }
  }

  private enum class WaveType {
    SINE,
    TRIANGLE,
  }

  private fun synthesizeCozyPuzzleLoop(): ShortArray {
    val sampleRate = SAMPLE_RATE
    val stepDurationSec = 0.24
    val steps = 32
    val totalSamples = (sampleRate * stepDurationSec * steps).toInt()
    val floatBuf = DoubleArray(totalSamples)

    val chordRoots = doubleArrayOf(130.81, 196.00, 220.00, 174.61, 130.81, 196.00, 174.61, 196.00)
    val chordThirds = doubleArrayOf(329.63, 246.94, 261.63, 220.00, 329.63, 246.94, 220.00, 246.94)
    val chordFifths = doubleArrayOf(392.00, 293.66, 329.63, 261.63, 392.00, 293.66, 261.63, 293.66)

    val melody =
      doubleArrayOf(
        523.25, 659.25, 783.99, 659.25,
        587.33, 659.25, 587.33, 493.88,
        440.00, 523.25, 659.25, 523.25,
        698.46, 659.25, 587.33, 523.25,
        523.25, 783.99, 880.00, 783.99,
        659.25, 587.33, 659.25, 783.99,
        698.46, 659.25, 587.33, 523.25,
        587.33, 659.25, 587.33, 392.00,
      )

    val stepSamples = (sampleRate * stepDurationSec).toInt()

    for (bar in 0 until 8) {
      val barStart = bar * 4 * stepSamples
      val barLen = 4 * stepSamples
      val r = chordRoots[bar]
      val t3 = chordThirds[bar]
      val f5 = chordFifths[bar]
      for (j in 0 until barLen) {
        val idx = barStart + j
        if (idx >= totalSamples) break
        val sec = idx.toDouble() / sampleRate
        val frac = j.toDouble() / barLen
        val attack = (j / (sampleRate * 0.035)).coerceAtMost(1.0)
        val release = ((barLen - j) / (sampleRate * 0.06)).coerceAtMost(1.0)
        val padEnv = attack * release
        val pad = (sin(2.0 * PI * t3 * sec) + sin(2.0 * PI * f5 * sec)) * 0.022 * padEnv
        val bassDecay = exp(-4.8 * frac) * (j / (sampleRate * 0.012)).coerceAtMost(1.0)
        val bass = sin(2.0 * PI * r * sec) * 0.075 * bassDecay
        floatBuf[idx] += pad + bass
      }
    }

    for (s in 0 until steps) {
      val start = s * stepSamples
      val freq = melody[s]
      val noteLen = (stepSamples * 1.45).toInt()
      for (j in 0 until noteLen) {
        val idx = (start + j) % totalSamples
        val sec = j.toDouble() / sampleRate
        val attack = (j / (sampleRate * 0.012)).coerceAtMost(1.0)
        val decay = exp(-5.2 * sec)
        val tone =
          (sin(2.0 * PI * freq * sec) * 0.78 + sin(4.0 * PI * freq * sec) * 0.22) *
            attack *
            decay *
            0.095
        floatBuf[idx] += tone
      }
      if (s % 2 == 1) {
        val shakerLen = (sampleRate * 0.032).toInt()
        for (j in 0 until shakerLen) {
          val idx = (start + j) % totalSamples
          val env = exp(-8.0 * j / shakerLen)
          val n = (Random.nextDouble() * 2.0 - 1.0) * 0.012 * env
          floatBuf[idx] += n
        }
      }
    }

    val out = ShortArray(totalSamples)
    for (i in 0 until totalSamples) {
      val v = floatBuf[i].coerceIn(-0.9, 0.9)
      out[i] = (v * Short.MAX_VALUE).toInt().toShort()
    }
    return out
  }

  private fun generateBuzzLoop(durationMs: Int, volume: Float): ShortArray {
    val count = (SAMPLE_RATE * durationMs / 1000).coerceAtLeast(1)
    val samples = ShortArray(count)
    var phase = 0.0
    for (i in 0 until count) {
      val t = i.toDouble() / SAMPLE_RATE
      val lfo = sin(2.0 * PI * 18.0 * t)
      val freq = 175.0 + lfo * 18.0
      phase += 2.0 * PI * freq / SAMPLE_RATE
      val fade = sin(PI * i / count).coerceAtLeast(0.2)
      val saw = ((phase / PI) % 2.0) - 1.0
      val buzz = saw * 0.55 + sin(phase * 2.0) * 0.3 + sin(phase * 3.0) * 0.15
      samples[i] = (buzz * fade * volume * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
    }
    return samples
  }

  private fun generatePencilScratch(durationMs: Int, volume: Float): ShortArray {
    val count = (SAMPLE_RATE * durationMs / 1000).coerceAtLeast(1)
    val samples = ShortArray(count)
    var prev = 0.0
    for (i in 0 until count) {
      val frac = i.toDouble() / count
      val env = sin(PI * frac)
      val noise = (Random.nextDouble() * 2.0 - 1.0)
      val crisp = (noise - prev * 0.65) * 0.6 + sin(2.0 * PI * 420.0 * i / SAMPLE_RATE) * 0.35
      prev = noise
      samples[i] = (crisp * env * volume * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
    }
    return samples
  }

  private fun generateBoing(durationMs: Int, volume: Float): ShortArray {
    val count = (SAMPLE_RATE * durationMs / 1000).coerceAtLeast(1)
    val samples = ShortArray(count)
    var phase = 0.0
    for (i in 0 until count) {
      val frac = i.toDouble() / count
      val wobble = sin(2.0 * PI * 3.0 * frac) * 90.0
      val freq = 240.0 + 180.0 * frac + wobble
      phase += 2.0 * PI * freq / SAMPLE_RATE
      val env = exp(-3.5 * frac)
      samples[i] = (sin(phase) * env * volume * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
    }
    return samples
  }

  private fun generateDogBark(pitchBase: Double, volume: Float): ShortArray {
    val durationMs = 95
    val count = (SAMPLE_RATE * durationMs / 1000).coerceAtLeast(1)
    val samples = ShortArray(count)
    var phase = 0.0
    for (i in 0 until count) {
      val frac = i.toDouble() / count
      val freq = pitchBase + sin(PI * frac) * 240.0
      phase += 2.0 * PI * freq / SAMPLE_RATE
      val env = sin(PI * frac) * exp(-2.5 * frac)
      val wave = sin(phase) * 0.7 + sin(phase * 2.0) * 0.3
      samples[i] = (wave * env * volume * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
    }
    return samples
  }

  private fun generateTone(freq: Double, durationMs: Int, volume: Float, wave: WaveType): ShortArray {
    val count = (SAMPLE_RATE * durationMs / 1000).coerceAtLeast(1)
    val samples = ShortArray(count)
    for (i in 0 until count) {
      val t = i.toDouble() / SAMPLE_RATE
      val env = exp(-4.5 * i / count)
      val raw =
        when (wave) {
          WaveType.SINE -> sin(2.0 * PI * freq * t)
          WaveType.TRIANGLE -> {
            val phase = (t * freq) % 1.0
            4.0 * abs(phase - 0.5) - 1.0
          }
        }
      samples[i] = (raw * env * volume * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
    }
    return samples
  }

  private fun generateSweep(startFreq: Double, endFreq: Double, durationMs: Int, volume: Float): ShortArray {
    val count = (SAMPLE_RATE * durationMs / 1000).coerceAtLeast(1)
    val samples = ShortArray(count)
    var phase = 0.0
    for (i in 0 until count) {
      val frac = i.toDouble() / count
      val freq = startFreq + (endFreq - startFreq) * frac
      phase += 2.0 * PI * freq / SAMPLE_RATE
      val env = (1.0 - frac * 0.7) * sin(PI * frac).coerceAtLeast(0.1)
      samples[i] =
        (sin(phase) * env * volume * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
    }
    return samples
  }

  private fun concatClips(vararg parts: ShortArray): ShortArray {
    val total = parts.sumOf { it.size }
    val out = ShortArray(total)
    var offset = 0
    for (part in parts) {
      part.copyInto(out, offset)
      offset += part.size
    }
    return out
  }

  fun release() {
    stopMusicLoop()
    stopBeeBuzz()
    sfxMixerJob?.cancel()
    sfxMixerJob = null
    scope.cancel()
    synchronized(this) {
      try {
        musicTrack?.stop()
        musicTrack?.release()
      } catch (_: Exception) {}
      musicTrack = null
      try {
        sfxTrack?.stop()
        sfxTrack?.release()
      } catch (_: Exception) {}
      sfxTrack = null
    }
  }

  private companion object {
    const val SAMPLE_RATE = 22050
  }
}

package org.hfathi.bugloos.playback.system

import android.animation.ValueAnimator
import android.content.Context
import android.media.AudioManager
import androidx.core.animation.addListener
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import com.google.android.exoplayer2.SimpleExoPlayer
import org.hfathi.bugloos.logD
import org.hfathi.bugloos.playback.state.PlaybackStateManager
import org.hfathi.bugloos.settings.SettingsManager
import org.hfathi.bugloos.ui.getSystemServiceSafe

/**
 * Object that manages the AudioFocus state.
 * Adapted from NewPipe (https://github.com/TeamNewPipe/NewPipe)
 * @author hamid fathi
 */
class AudioReactor(
    context: Context,
    private val player: SimpleExoPlayer
) : AudioManager.OnAudioFocusChangeListener {
    private val audioManager = context.getSystemServiceSafe(AudioManager::class)

    private val settingsManager = SettingsManager.getInstance()
    private val playbackManager = PlaybackStateManager.getInstance()

    private val request = AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
        .setWillPauseWhenDucked(true)
        .setOnAudioFocusChangeListener(this)
        .build()

    private var pauseWasTransient = false

    /**
     * Request the android system for audio focus
     */
    fun requestFocus() {
        AudioManagerCompat.requestAudioFocus(audioManager, request)
    }

    /**
     * Abandon the current focus request, functionally "Destroying it".
     */
    fun release() {
        AudioManagerCompat.abandonAudioFocusRequest(audioManager, request)
    }

    override fun onAudioFocusChange(focusChange: Int) {
        if (!settingsManager.doAudioFocus) {
            // Dont do audio focus if its not enabled
            return
        }

        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> onGain()
            AudioManager.AUDIOFOCUS_LOSS -> onLossPermanent()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> onLossTransient()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> onDuck()
        }
    }

    private fun onGain() {
        if (player.volume == VOLUME_DUCK) {
            unduck()
        } else if (pauseWasTransient) {
            logD("Gained focus after transient loss")

            // Play again if the pause was only temporary [AudioManager.AUDIOFOCUS_LOSS_TRANSIENT]
            playbackManager.setPlaying(true)
            pauseWasTransient = false
        }
    }

    private fun onLossTransient() {
        // Since this loss is only temporary, mark it as such if we had to pause playback.
        if (playbackManager.isPlaying) {
            logD("Pausing for transient loss")

            playbackManager.setPlaying(false)
            pauseWasTransient = true
        }
    }

    private fun onLossPermanent() {
        logD("Pausing for permanent loss")

        playbackManager.setPlaying(false)
    }

    private fun onDuck() {
        logD("Ducking, lowering volume")

        player.volume = VOLUME_DUCK
    }

    private fun unduck() {
        logD("Unducking, raising volume")

        player.volume = VOLUME_DUCK

        ValueAnimator().apply {
            setFloatValues(VOLUME_DUCK, VOLUME_FULL)
            duration = DUCK_DURATION
            addListener(
                onStart = { player.volume = VOLUME_DUCK },
                onCancel = { player.volume = VOLUME_FULL },
                onEnd = { player.volume = VOLUME_FULL }
            )
            addUpdateListener {
                player.volume = animatedValue as Float
            }
            start()
        }
    }

    companion object {
        private const val VOLUME_DUCK = 0.2f
        private const val DUCK_DURATION = 1500L
        private const val VOLUME_FULL = 1.0f
    }
}

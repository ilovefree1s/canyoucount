package com.canyoucount.timeit

import android.media.MediaPlayer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.canyoucount.timeit.ui.TimeItNavGraph
import com.canyoucount.timeit.ui.theme.TimeItTheme

class MainActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private val isMuted = mutableStateOf(false)

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TimeItTheme {
                Surface(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                    TimeItNavGraph(
                        isMuted = isMuted.value,
                        onToggleMute = {
                            isMuted.value = !isMuted.value
                            if (isMuted.value) mediaPlayer?.setVolume(0f, 0f)
                            else mediaPlayer?.setVolume(0.5f, 0.5f)
                        }
                    )
                }
            }
        }
        startMusic()
    }

    private fun startMusic() {
        mediaPlayer = MediaPlayer.create(this, R.raw.clockworkpulse).apply {
            isLooping = true
            setVolume(0.5f, 0.5f)
            start()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isMuted.value) mediaPlayer?.start()
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

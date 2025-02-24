package expo.modules.video

import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.google.ads.interactivemedia.v3.api.AdPodInfo
import com.google.ads.interactivemedia.v3.api.player.AdMediaInfo
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer.VideoAdPlayerCallback
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate
import java.util.Timer
import java.util.TimerTask
import kotlin.math.log


/** Example implementation of IMA's VideoAdPlayer interface.  */
class VideoAdPlayerAdapter(private val videoPlayer: ExoPlayer, audioManager: AudioManager) : VideoAdPlayer {
  private val audioManager: AudioManager
  private val videoAdPlayerCallbacks: MutableList<VideoAdPlayerCallback> = ArrayList()
  private var timer: Timer? = null
  private var adDuration = 0
  private var isInitialPrepare = true // TODO: set to false on new source

  // The saved ad position, used to resumed ad playback following an ad click-through.
  private var savedAdPosition = 0
  private var loadedAdMediaInfo: AdMediaInfo? = null

  init {
    Log.d(LOGTAG, "Initialized VideoAdPlayerAdapter")
    videoPlayer.addListener(object : Player.Listener {
      override fun onPlaybackStateChanged(state: Int) {
        Log.d(LOGTAG,"onPlaybackStateChanged")
        if (state == Player.STATE_ENDED)
          notifyImaOnContentCompleted()
      }
    })

    this.audioManager = audioManager
  }

  private fun startAdTracking() {
    Log.i(LOGTAG, "startAdTracking")
    if (timer != null) {
      return
    }
    timer = Timer()
    val handler = Handler(Looper.getMainLooper())
    val updateTimerTask: TimerTask =
      object : TimerTask() {
        override fun run() {
          handler.post {
            val progressUpdate = adProgress
            notifyImaSdkAboutAdProgress(progressUpdate)
          }
        }
      }
    timer?.schedule(updateTimerTask, POLLING_TIME_MS, INITIAL_DELAY_MS)
  }


  private fun notifyImaSdkAboutAdEnded() {
    Log.i(LOGTAG, "notifyImaSdkAboutAdEnded")
    savedAdPosition = 0
    for (callback in videoAdPlayerCallbacks) {
      callback.onEnded(loadedAdMediaInfo!!)
    }
  }

  private fun notifyImaSdkAboutAdProgress(adProgress: VideoProgressUpdate) {
    for (callback in videoAdPlayerCallbacks) {
      callback.onAdProgress(loadedAdMediaInfo!!, adProgress)
    }
  }

  /**
   * @param errorType Media player's error type as defined at
   * https://cs.android.com/android/platform/superproject/+/master:frameworks/base/media/java/android/media/MediaPlayer.java;l=4335
   * @return True to stop the current ad playback.
   */
  private fun notifyImaSdkAboutAdError(errorType: Int): Boolean {
    Log.i(LOGTAG, "notifyImaSdkAboutAdError")

    when (errorType) {
      MediaPlayer.MEDIA_ERROR_UNSUPPORTED -> Log.e(LOGTAG, "notifyImaSdkAboutAdError: MEDIA_ERROR_UNSUPPORTED")
      MediaPlayer.MEDIA_ERROR_TIMED_OUT -> Log.e(LOGTAG, "notifyImaSdkAboutAdError: MEDIA_ERROR_TIMED_OUT")
      else -> {}
    }
    for (callback in videoAdPlayerCallbacks) {
      callback.onError(loadedAdMediaInfo!!)
    }
    return true
  }

  fun notifyImaOnContentCompleted() {
    Log.i(LOGTAG, "notifyImaOnContentCompleted")
    for (callback in videoAdPlayerCallbacks) {
      callback.onContentComplete()
    }
  }

  private fun stopAdTracking() {
    Log.i(LOGTAG, "stopAdTracking")
    if (timer != null) {
      timer!!.cancel()
      timer = null
    }
  }

  override fun getAdProgress(): VideoProgressUpdate {
    val adPosition = videoPlayer.currentPosition
    return VideoProgressUpdate(adPosition, adDuration.toLong())
  }

  override fun addCallback(videoAdPlayerCallback: VideoAdPlayerCallback) {
    videoAdPlayerCallbacks.add(videoAdPlayerCallback)
  }

  override fun loadAd(adMediaInfo: AdMediaInfo, adPodInfo: AdPodInfo) {
    Log.d(LOGTAG, "loadAd")
    // This simple ad loading logic works because preloading is disabled. To support
    // preloading ads your app must maintain state for the currently playing ad
    // while handling upcoming ad downloading and buffering at the same time.
    // See the IMA Android preloading guide for more info:
    // https://developers.google.com/interactive-media-ads/docs/sdks/android/client-side/preload
    loadedAdMediaInfo = adMediaInfo
  }

  override fun pauseAd(adMediaInfo: AdMediaInfo) {
    Log.i(LOGTAG, "pauseAd")
    savedAdPosition = videoPlayer.currentPosition.toInt()
    stopAdTracking()
  }

  override fun playAd(adMediaInfo: AdMediaInfo){
    Log.d("IMA", "Play ad: ${adMediaInfo.url}")
    val mediaItem = MediaItem.fromUri(adMediaInfo.url) // Create a media item
    videoPlayer.setMediaItem(mediaItem)

    videoPlayer.prepare() // Prepare the player

    videoPlayer.addListener(object : Player.Listener {
      override fun onPlaybackStateChanged(playbackState: Int) {
        Log.d(LOGTAG, "onPlaybackStateChanged")

        if (playbackState == Player.STATE_READY && isInitialPrepare) {
          Log.d(LOGTAG, "onPlaybackStateChanged state ready")
          isInitialPrepare = false
          adDuration = videoPlayer.duration.toInt()
          if (savedAdPosition > 0) {
            videoPlayer.seekTo(savedAdPosition.toLong())
          }
          videoPlayer.play()
          startAdTracking()
        }
        else if (playbackState == Player.STATE_ENDED) {
          Log.d(LOGTAG, "onPlaybackStateChanged ended")
          savedAdPosition = 0
          notifyImaSdkAboutAdEnded()
        }
      }

      override fun onPlayerError(error: PlaybackException) {
        Log.e(LOGTAG, "onPlayerError")
        notifyImaSdkAboutAdError(error.errorCode)
      }
    })
  }

  override fun release() {
    Log.d(LOGTAG, "Release")
    // any clean up that needs to be done.
  }

  override fun removeCallback(videoAdPlayerCallback: VideoAdPlayerCallback) {
    videoAdPlayerCallbacks.remove(videoAdPlayerCallback)
  }

  override fun stopAd(adMediaInfo: AdMediaInfo) {
    Log.i(LOGTAG, "stopAd")
    stopAdTracking()
  }

  /** Returns current volume as a percent of max volume.  */
  override fun getVolume(): Int {
    return (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
      / audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC))
  }

  companion object {
    private const val LOGTAG = "IMA"
    private const val POLLING_TIME_MS: Long = 250
    private const val INITIAL_DELAY_MS: Long = 250
  }
}
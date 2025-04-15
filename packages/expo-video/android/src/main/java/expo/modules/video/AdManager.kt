package expo.modules.video

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.ima.ImaAdsLoader
import com.google.ads.interactivemedia.v3.api.AdEvent
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer
import com.google.ads.interactivemedia.v3.api.AdErrorEvent
import com.google.ads.interactivemedia.v3.api.player.*
import expo.modules.kotlin.AppContext
import kotlinx.coroutines.launch

private const val LOG_TAG = "AdManager"

class AdManager(val context: Context, val appContext: AppContext?) {
  private var isAdManagerInitialized = false
  val adsLoader = buildAdsLoader()
  val logHandler = LogHandler(enabled = false)

  fun initializeAds(player: ExoPlayer) {
    if(isAdManagerInitialized){ return }

    isAdManagerInitialized = true
    adsLoader.setPlayer(player)
    logHandler.d(LOG_TAG, "Player is configured to display Ads")
  }

  fun dispose(){
    isAdManagerInitialized = false

    appContext?.mainQueue?.launch {
      adsLoader.setPlayer(null)
      adsLoader.release()
    }
  }

  // TODO: COMPANION FOR ALL BUILDERS?
  @OptIn(UnstableApi::class)
  fun buildAdsLoader(): ImaAdsLoader {
    return ImaAdsLoader.Builder(context)
      .setAdEventListener(buildAdEventListener())
      .setAdErrorListener(buildAdErrorListener())
      .setVideoAdPlayerCallback(buildAdPlayerCallback())
      .build()
  }

  private fun buildAdEventListener(): AdEvent.AdEventListener {
    return AdEvent.AdEventListener { event ->
      logHandler.d(LOG_TAG, "Received AD Event: ${event.type}")
    }
  }

  private fun buildAdErrorListener(): AdErrorEvent.AdErrorListener {
    return AdErrorEvent.AdErrorListener { errorEvent ->
      logHandler.e(LOG_TAG, "Received AD Error: ${errorEvent.error.message}")
    }
  }

  private fun buildAdPlayerCallback(): VideoAdPlayer.VideoAdPlayerCallback {
    return object : VideoAdPlayer.VideoAdPlayerCallback {
      override fun onPlay(adMediaInfo: AdMediaInfo) {
        logHandler.d(LOG_TAG, "Ad started playing: ${adMediaInfo.url}")
      }

      override fun onPause(adMediaInfo: AdMediaInfo) {
        logHandler.d(LOG_TAG, "Ad paused: ${adMediaInfo.url}")
      }

      override fun onResume(adMediaInfo: AdMediaInfo) {
        logHandler.d(LOG_TAG, "Ad resumed: ${adMediaInfo.url}")
      }

      override fun onVolumeChanged(adMediaInfo: AdMediaInfo, volume: Int) {
        logHandler.d(LOG_TAG, "Ad volume changed: ${adMediaInfo.url}")
      }

      override fun onAdProgress(adMediaInfo: AdMediaInfo, update: VideoProgressUpdate) {
        logHandler.d(LOG_TAG, "Ad progress: ${update.currentTimeMs}")
      }

      override fun onBuffering(adMediaInfo: AdMediaInfo) {
        logHandler.d(LOG_TAG, "Ad buffering: ${adMediaInfo.url}")
      }

      override fun onContentComplete() {
        logHandler.d(LOG_TAG, "Ad completed")
      }

      override fun onEnded(adMediaInfo: AdMediaInfo) {
        logHandler.d(LOG_TAG, "Ad ended: ${adMediaInfo.url}")
      }

      override fun onError(adMediaInfo: AdMediaInfo) {
        logHandler.e(LOG_TAG, "Ad error: ${adMediaInfo.url}")
      }

      override fun onLoaded(adMediaInfo: AdMediaInfo) {
        logHandler.d(LOG_TAG, "Ad loaded: ${adMediaInfo.url}")
      }
    }
  }
}

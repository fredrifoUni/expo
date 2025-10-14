package expo.plugins.interactiveMediaAds

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.ui.PlayerView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.ima.ImaAdsLoader
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.google.ads.interactivemedia.v3.api.AdErrorEvent
import com.google.ads.interactivemedia.v3.api.AdEvent
import com.google.ads.interactivemedia.v3.api.player.AdMediaInfo
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate
import expo.modules.kotlin.AppContext
import expo.modules.video.interfaces.AdManager
import kotlinx.coroutines.launch

private const val LOG_TAG = "AdManager"

class AdManagerIMA(val context: Context, val appContext: AppContext?): AdManager {
  private var isAdManagerInitialized = false
  private val adsLoader = buildAdsLoader()
  override fun initializeAds(player: ExoPlayer) {
    if(isAdManagerInitialized){ return }

    isAdManagerInitialized = true
    adsLoader.setPlayer(player)
    Log.d(LOG_TAG, "Player is configured to display Ads")
  }

  override fun setLocalAdInsertionComponents(mediaSourceBuilder: DefaultMediaSourceFactory?, playerView: PlayerView) {
    mediaSourceBuilder?.setLocalAdInsertionComponents({ _ -> adsLoader }, playerView)
  }

  override fun dispose(){
    isAdManagerInitialized = false

    appContext?.mainQueue?.launch {
      adsLoader.setPlayer(null)
      adsLoader.release()
    }
  }

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
      Log.d(LOG_TAG, "Received AD Event: ${event.type}")
    }
  }

  private fun buildAdErrorListener(): AdErrorEvent.AdErrorListener {
    return AdErrorEvent.AdErrorListener { errorEvent ->
      Log.e(LOG_TAG, "Received AD Error: ${errorEvent.error.message}")
    }
  }

  private fun buildAdPlayerCallback(): VideoAdPlayer.VideoAdPlayerCallback {
    return object : VideoAdPlayer.VideoAdPlayerCallback {
      override fun onPlay(adMediaInfo: AdMediaInfo) {
        Log.d(LOG_TAG, "Ad started playing: ${adMediaInfo.url}")
      }

      override fun onPause(adMediaInfo: AdMediaInfo) {
        Log.d(LOG_TAG, "Ad paused: ${adMediaInfo.url}")
      }

      override fun onResume(adMediaInfo: AdMediaInfo) {
        Log.d(LOG_TAG, "Ad resumed: ${adMediaInfo.url}")
      }

      override fun onVolumeChanged(adMediaInfo: AdMediaInfo, volume: Int) {
        Log.d(LOG_TAG, "Ad volume changed: ${adMediaInfo.url}")
      }

      override fun onAdProgress(adMediaInfo: AdMediaInfo, update: VideoProgressUpdate) {
        Log.d(LOG_TAG, "Ad progress: ${update.currentTimeMs}")
      }

      override fun onBuffering(adMediaInfo: AdMediaInfo) {
        Log.d(LOG_TAG, "Ad buffering: ${adMediaInfo.url}")
      }

      override fun onContentComplete() {
        Log.d(LOG_TAG, "Ad completed")
      }

      override fun onEnded(adMediaInfo: AdMediaInfo) {
        Log.d(LOG_TAG, "Ad ended: ${adMediaInfo.url}")
      }

      override fun onError(adMediaInfo: AdMediaInfo) {
        Log.e(LOG_TAG, "Ad error: ${adMediaInfo.url}")
      }

      override fun onLoaded(adMediaInfo: AdMediaInfo) {
        Log.d(LOG_TAG, "Ad loaded: ${adMediaInfo.url}")
      }
    }
  }
}

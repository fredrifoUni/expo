package expo.modules.video

import android.content.Context
import android.util.Log
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

class AdManager(val context: Context, val appContext: AppContext?) {
  private var isAdManagerInitialized = false
  val adsLoader = buildAdsLoader()

  fun initializeAds(player: ExoPlayer) {
    if(isAdManagerInitialized){ return }

    isAdManagerInitialized = true
    adsLoader.setPlayer(player)
    Log.d("IMA", "Player is configured to display Ads")
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

  fun buildAdEventListener(): AdEvent.AdEventListener {
    return AdEvent.AdEventListener { event ->
      Log.d("IMA", "Received AD Event: ${event.type}")
    }
  }

  fun buildAdErrorListener(): AdErrorEvent.AdErrorListener {
    return AdErrorEvent.AdErrorListener { errorEvent ->
      Log.e("IMA", "Received AD Error: ${errorEvent.error.message}")
    }
  }

  fun buildAdPlayerCallback(): VideoAdPlayer.VideoAdPlayerCallback {
    return object : VideoAdPlayer.VideoAdPlayerCallback {
      override fun onPlay(adMediaInfo: AdMediaInfo) {
        Log.d("IMA", "Ad started playing: ${adMediaInfo.url}")
      }

      override fun onPause(adMediaInfo: AdMediaInfo) {
        Log.d("IMA", "Ad paused: ${adMediaInfo.url}")
      }

      override fun onResume(adMediaInfo: AdMediaInfo) {
        Log.d("IMA", "Ad resumed: ${adMediaInfo.url}")
      }

      override fun onVolumeChanged(adMediaInfo: AdMediaInfo, volume: Int) {
        Log.d("IMA", "Ad volume changed: ${adMediaInfo.url}")
      }

      override fun onAdProgress(adMediaInfo: AdMediaInfo, update: VideoProgressUpdate) {
        Log.d("IMA", "Ad progress: ${update.currentTimeMs}")
      }

      override fun onBuffering(adMediaInfo: AdMediaInfo) {
        Log.d("IMA", "Ad buffering: ${adMediaInfo.url}")
      }

      override fun onContentComplete() {
        Log.d("IMA", "Ad completed")
      }

      override fun onEnded(adMediaInfo: AdMediaInfo) {
        Log.d("IMA", "Ad ended: ${adMediaInfo.url}")
      }

      override fun onError(adMediaInfo: AdMediaInfo) {
        Log.e("IMA", "Ad error: ${adMediaInfo.url}")
      }

      override fun onLoaded(adMediaInfo: AdMediaInfo) {
        Log.d("IMA", "Ad loaded: ${adMediaInfo.url}")
      }
    }
  }
}

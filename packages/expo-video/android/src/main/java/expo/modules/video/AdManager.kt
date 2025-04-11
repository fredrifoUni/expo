package expo.modules.video

import android.util.Log
import com.google.ads.interactivemedia.v3.api.AdEvent
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer
import com.google.ads.interactivemedia.v3.api.AdErrorEvent
import com.google.ads.interactivemedia.v3.api.player.*

class AdManager {
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

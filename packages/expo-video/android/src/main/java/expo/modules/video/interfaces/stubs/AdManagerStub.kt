package expo.modules.video.interfaces.stubs

import android.util.Log
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import expo.modules.video.interfaces.AdManager

class AdManagerStub : AdManager {
  override fun initializeAds(player: ExoPlayer) = notImplemented()
  override fun setLocalAdInsertionComponents(mediaSourceBuilder: DefaultMediaSourceFactory?, playerView: PlayerView) = notImplemented()
  override fun dispose() = notImplemented()

  fun notImplemented(){
    Log.e("AdManager", "Attempted to use plugin that has not been configured")
  }
}

package expo.modules.video.interfaces

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.AdsConfiguration
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import expo.modules.video.records.Advertisement
import androidx.core.net.toUri


interface AdManager {
  fun initializeAds(player: ExoPlayer)
  fun setLocalAdInsertionComponents(mediaSourceBuilder: DefaultMediaSourceFactory?, playerView: PlayerView)
  fun dispose()

  companion object {
    fun injectAdsToMediaItemBuilder(mediaBuilder: MediaItem.Builder, advertisement: Advertisement?) {
      advertisement?.googleIMA?.adTagUrl?.let {
        mediaBuilder.setAdsConfiguration(
          AdsConfiguration.Builder(it.toUri()).build()
        )
      }
    }
  }
}

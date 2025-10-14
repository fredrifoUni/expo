package expo.modules.video.player

import androidx.media3.common.C
import androidx.media3.common.Timeline
import androidx.media3.common.util.Assertions
import androidx.media3.common.util.Log
import androidx.media3.common.util.NullableType
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.analytics.PlayerId
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.exoplayer.upstream.Allocator
import androidx.media3.exoplayer.upstream.DefaultAllocator
import expo.modules.video.records.BufferOptions

@UnstableApi
class VideoPlayerLoadControl : DefaultLoadControl() {
  private var targetBufferMs: Long
    get() = maxBufferUs / 1000
    set(value) {
      minBufferUs = Util.msToUs(value)
      maxBufferUs = Util.msToUs(value)
    }

  private var bufferForPlaybackMs: Long
    get() = bufferForPlaybackUs / 1000
    set(value) {
      bufferForPlaybackUs = Util.msToUs(value)
    }

  private var bufferForPlaybackAfterRebufferMs: Long
    get() = bufferForPlaybackAfterRebufferUs / 1000
    set(value) {
      bufferForPlaybackAfterRebufferUs = Util.msToUs(value)
    }

  fun applyBufferOptions(bufferOptions: BufferOptions) {
    targetBufferMs = bufferOptions.preferredForwardBufferDuration?.let { (it * 1000).toLong() }
      ?: DEFAULT_MAX_BUFFER_MS.toLong()

    targetBufferBytesOverwrite = if (bufferOptions.maxBufferBytes == 0L) {
      C.LENGTH_UNSET
    } else {
      bufferOptions.maxBufferBytes.toInt()
    }

    if (targetBufferBytesOverwrite != C.LENGTH_UNSET) {
      for (state in loadingStates.values) {
        state.targetBufferBytes = targetBufferBytesOverwrite
      }
    }

    prioritizeTimeOverSizeThresholds = bufferOptions.prioritizeTimeOverSizeThreshold

    val safeBufferForPlayback = if (bufferOptions.minBufferForPlayback * 1000 > targetBufferMs) {
      targetBufferMs
    } else {
      (bufferOptions.minBufferForPlayback * 1000).toLong()
    }
    bufferForPlaybackMs = safeBufferForPlayback
    bufferForPlaybackAfterRebufferMs = safeBufferForPlayback

    updateAllocator()
  }
}

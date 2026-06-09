package expo.modules.video

import android.view.Gravity
import android.view.View.VISIBLE
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import expo.modules.video.listeners.VideoPlayerListener
import expo.modules.video.player.VideoPlayer
import expo.modules.video.records.AudioTrack
import expo.modules.video.records.ContentProposalRecord
import expo.modules.video.records.SubtitleTrack
import expo.modules.video.records.TimeUpdate
import expo.modules.video.records.VideoSource
import expo.modules.video.records.VideoTrack

@OptIn(UnstableApi::class)
internal class ContentProposalManager(private val videoView: VideoView) : VideoPlayerListener {
  private val overlayView: UpNextOverlayView = UpNextOverlayView(videoView.context).also { overlay ->
    val margin = (40 * videoView.resources.displayMetrics.density).toInt()
    val lp = FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.WRAP_CONTENT,
      FrameLayout.LayoutParams.WRAP_CONTENT,
      Gravity.TOP or Gravity.END
    ).apply {
      topMargin = margin
      marginEnd = margin
    }
    videoView.playerView.addView(overlay, lp)
  }

  private var currentPlayer: VideoPlayer? = null

  var contentProposal: ContentProposalRecord? = null
    set(value) {
      field = value
      reset()
      if (value != null) {
        currentPlayer?.addListener(this)
      }
    }

  fun onPlayerChanged(newPlayer: VideoPlayer?) {
    reset()
    currentPlayer = newPlayer
    if (contentProposal != null && newPlayer != null) {
      newPlayer.addListener(this)
    }
  }

  override fun onVideoSourceLoaded(
    player: VideoPlayer,
    videoSource: VideoSource?,
    duration: Double?,
    availableVideoTracks: List<VideoTrack>,
    availableSubtitleTracks: List<SubtitleTrack>,
    availableAudioTracks: List<AudioTrack>
  ) {
    hideOverlay()
  }

  override fun onPlayedToEnd(player: VideoPlayer) {
    hideOverlay()
  }

  override fun onTimeUpdate(player: VideoPlayer, timeUpdate: TimeUpdate) {
    val proposal = contentProposal ?: return
    val duration = player.duration.toDouble()
    if (duration <= 0) return
    val remaining = duration - timeUpdate.currentTime

    if (remaining > 0 && remaining <= proposal.showAtRemainingTime) {
      if (overlayView.visibility != VISIBLE) {
        showOverlay(proposal)
      }
    } else if (overlayView.visibility == VISIBLE) {
      hideOverlay()
    }
  }

  private fun reset() {
    currentPlayer?.removeListener(this)
    hideOverlay()
  }

  private fun showOverlay(proposal: ContentProposalRecord) {
    overlayView.show(proposal.title, proposal.imageUrl)
  }

  private fun hideOverlay() {
    overlayView.hide()
  }
}

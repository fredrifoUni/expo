package expo.modules.video

import android.app.Dialog
import android.content.Context
import android.util.Log
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.ui.PlayerView

private const val LOG_TAG = "FullscreenDialog"

class FullscreenPlayerDialog(
  context: Context,
  private val playerView: PlayerView,
  private val onBackPressCallback: () -> Unit
) : Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {
  private val containerView = FrameLayout(context)
  private var layoutParamsMatchParent = FrameLayout.LayoutParams(
    FrameLayout.LayoutParams.MATCH_PARENT,
    FrameLayout.LayoutParams.MATCH_PARENT
  )

  init {
    setContentView(containerView, layoutParamsMatchParent)
    Log.d(LOG_TAG, "Fullscreen Dialog is initializing")
  }

  override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    // Forward backPress callback to parent view
    if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
      onBackPressCallback()
      return true
    }
    return super.dispatchKeyEvent(event)
  }

  override fun onStop() {
    super.onStop()
  }

  override fun onStart() {
    super.onStart()

    // Move the player view to the fullscreen dialog
    (playerView.parent as? ViewGroup)?.removeView(playerView)
    containerView.addView(playerView, layoutParamsMatchParent)

    // Hide system bars (including navigation back button)
    val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
    controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    controller?.hide(WindowInsetsCompat.Type.systemBars())

    Log.d(LOG_TAG, "Fullscreen Dialog finished initialization")
  }
}

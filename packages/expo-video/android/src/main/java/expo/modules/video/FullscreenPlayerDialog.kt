package expo.modules.video

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.annotation.RequiresApi
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
  val logHandler = LogHandler(enabled = true)

  private var layoutParamsMatchParent = FrameLayout.LayoutParams(
    FrameLayout.LayoutParams.MATCH_PARENT,
    FrameLayout.LayoutParams.MATCH_PARENT
  )

  init {
    setContentView(containerView, layoutParamsMatchParent)

    logHandler.d(LOG_TAG, "Fullscreen Dialog is initializing")
  }


  // TODO: Not sure if this is ever triggered for Dialogs. DispatchKeyEvent might be sufficient.
  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  private val onBackInvokedCallback = OnBackInvokedCallback {
    onBackPressCallback()
  }

  override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
      onBackPressCallback()
      return true
    }
    return super.dispatchKeyEvent(event)
  }

  override fun onStop() {
    super.onStop()

    // Unregister listeners
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      onBackInvokedDispatcher.unregisterOnBackInvokedCallback(onBackInvokedCallback)
    }
  }

  override fun onStart() {
    super.onStart()

    // Move the player view to the fullscreen dialog
    (playerView.parent as? ViewGroup)?.removeView(playerView)
    containerView.addView(playerView, layoutParamsMatchParent)

    // Hide system bars (including navigation back button)
    val insetController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
    insetController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    insetController?.hide(WindowInsetsCompat.Type.systemBars())

    // Register listeners
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      onBackInvokedDispatcher.registerOnBackInvokedCallback(
        OnBackInvokedDispatcher.PRIORITY_DEFAULT,
        onBackInvokedCallback
      )
    }

    logHandler.d(LOG_TAG, "Fullscreen Dialog finished initialization")
  }
}

package expo.modules.video

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.annotation.RequiresApi
import androidx.core.view.WindowCompat
import androidx.media3.ui.PlayerView

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

    Log.d("IMA", "Dialog" + (window !== null).toString())

    // Set the dialog to fullscreen
//    window?.let {
//      WindowCompat.setDecorFitsSystemWindows(it, false)
//      it.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
//    };
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

    Log.d("IMA", "Dialog onStart")

    // Move the player view to the fullscreen dialog
    (playerView.parent as? ViewGroup)?.removeView(playerView)
    containerView.addView(playerView, layoutParamsMatchParent)

    // Register listeners
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      onBackInvokedDispatcher.registerOnBackInvokedCallback(
        OnBackInvokedDispatcher.PRIORITY_DEFAULT,
        onBackInvokedCallback
      )
    }
  }
}
package expo.modules.video

import android.util.Log

// TODO: Allow log configurations using a Builder pattern (enableWarnings().enableErrors())
// Makes logging behavior configurable per feature
class LogHandler(enabled: Boolean = false) {
  private var isLoggingEnabled: Boolean = enabled

  fun d(tag: String, msg: String) {
    if (!isLoggingEnabled) { return }
    Log.d(tag, msg)
  }

  fun e(tag: String, msg: String) {
    if (!isLoggingEnabled) { return }
    Log.e(tag, msg)
  }
}

package expo.modules.video

import android.content.Context
import android.util.Log
import expo.modules.kotlin.AppContext
import expo.modules.video.interfaces.AdManager
import expo.modules.video.interfaces.stubs.AdManagerStub

private const val LOG_TAG = "AdManagerFactory"

// reflection-based dynamic loading of IMA plugin
object AdManagerFactory {
  fun create(context: Context, appContext: AppContext?): AdManager {
    if (BuildConfig.INCLUDE_IMA) {
      try {
        val adManagerIMA = Class.forName("expo.plugins.interactiveMediaAds.AdManagerIMA")
          .getConstructor(Context::class.java, AppContext::class.java)
          .newInstance(context, appContext) as AdManager

        Log.d(LOG_TAG, "Successfully loaded IMA Ad Plugin.")
        return adManagerIMA
      } catch (e: Throwable) {
        Log.e(LOG_TAG, "Failed to load IMA Ad Plugin: $e")
      }
    }

    Log.d(LOG_TAG, "IMA Ad plugin is not in use.")
    return AdManagerStub()
  }
}

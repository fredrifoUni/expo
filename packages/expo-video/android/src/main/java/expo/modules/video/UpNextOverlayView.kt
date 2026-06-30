package expo.modules.video

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

internal class UpNextOverlayView(context: Context) : FrameLayout(context) {
  private val imageView: ImageView
  private val titleLabel: TextView
  private var pendingImageCall: Call? = null

  init {
    background = GradientDrawable().apply {
      setColor(Color.parseColor("#CC000000"))
      cornerRadius = 12.dp
    }

    val padding = 14.dp.toInt()
    setPadding(padding, padding, padding, padding)

    imageView = ImageView(context).apply {
      scaleType = ImageView.ScaleType.CENTER_CROP
      val imageHeight = 72.dp.toInt()
      val imageWidth = (imageHeight * 16f / 9f).toInt()
      layoutParams = LinearLayout.LayoutParams(imageWidth, imageHeight)
      background = GradientDrawable().apply { cornerRadius = 8.dp }
      clipToOutline = true
      visibility = GONE
    }

    val upNextLabel = TextView(context).apply {
      text = "UP NEXT"
      setTextColor(Color.WHITE)
      setTypeface(null, Typeface.BOLD)
      textSize = 20f
    }

    titleLabel = TextView(context).apply {
      setTextColor(Color.WHITE)
      textSize = 20f
      maxLines = 2
      ellipsize = android.text.TextUtils.TruncateAt.END
    }

    val textStack = LinearLayout(context).apply {
      orientation = LinearLayout.VERTICAL
      layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
        marginStart = 12.dp.toInt()
        marginEnd = 12.dp.toInt()
      }
      addView(upNextLabel)
      addView(titleLabel)
    }

    val row = LinearLayout(context).apply {
      orientation = LinearLayout.HORIZONTAL
      gravity = Gravity.CENTER_VERTICAL
      addView(imageView)
      addView(textStack)
    }

    addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    visibility = GONE
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val maxWidth = 480.dp.toInt()
    val atMostSpec = MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST)
    super.onMeasure(atMostSpec, heightMeasureSpec)
  }

  fun show(title: String, imageUrl: String?) {
    titleLabel.text = title

    pendingImageCall?.cancel()
    pendingImageCall = null

    if (imageUrl != null) {
      imageView.visibility = VISIBLE
      val request = Request.Builder().url(imageUrl).build()
      val call = httpClient.newCall(request)
      pendingImageCall = call
      call.enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {}
        override fun onResponse(call: Call, response: Response) {
          val bytes = response.body?.bytes() ?: return
          val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return
          Handler(Looper.getMainLooper()).post {
            if (visibility == VISIBLE) {
              imageView.setImageBitmap(bitmap)
            }
          }
        }
      })
    } else {
      imageView.setImageBitmap(null)
      imageView.visibility = GONE
    }

    visibility = VISIBLE
  }

  fun hide() {
    pendingImageCall?.cancel()
    pendingImageCall = null
    visibility = GONE
  }

  private val Float.dp get() = this * resources.displayMetrics.density
  private val Int.dp get() = this.toFloat().dp

  companion object {
    private val httpClient = OkHttpClient()
  }
}

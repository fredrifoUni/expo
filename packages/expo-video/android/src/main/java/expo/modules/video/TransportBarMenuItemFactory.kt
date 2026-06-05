package expo.modules.video

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.ImageButton
import expo.modules.video.records.TransportBarCustomMenuItem

internal object TransportBarMenuItemFactory {
  fun createButton(
    context: Context,
    item: TransportBarCustomMenuItem,
    onPressed: (String) -> Unit
  ): ImageButton {
    val density = context.resources.displayMetrics.density
    val sizePx = (48 * density).toInt()
    val iconSizePx = context.resources.getDimensionPixelSize(androidx.media3.ui.R.dimen.exo_settings_icon_size)
    val paddingPx = (sizePx - iconSizePx) / 2
    return ImageButton(context).apply {
      val typedValue = TypedValue()
      context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, typedValue, true)
      setBackgroundResource(typedValue.resourceId)
      isFocusable = true
      isFocusableInTouchMode = false
      layoutParams = ViewGroup.LayoutParams(sizePx, sizePx)
      setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
      scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
      applyItem(context, item, onPressed)
    }
  }

  fun updateButton(
    context: Context,
    button: ImageButton,
    item: TransportBarCustomMenuItem,
    onPressed: (String) -> Unit
  ) {
    button.applyItem(context, item, onPressed)
  }

  private fun ImageButton.applyItem(
    context: Context,
    item: TransportBarCustomMenuItem,
    onPressed: (String) -> Unit
  ) {
    contentDescription = item.title
    val iconResId = context.resources.getIdentifier(item.icon.replace('.', '_'), "drawable", context.packageName)
    if (iconResId != 0) {
      setImageResource(iconResId)
      colorFilter = PorterDuffColorFilter(android.graphics.Color.WHITE, PorterDuff.Mode.SRC_IN)
    } else {
      setImageDrawable(null)
    }
    setOnClickListener { onPressed(item.id) }
  }
}

package expo.modules.video

import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.media3.ui.PlayerView
import expo.modules.video.records.CustomMenuItemPressedPayload
import expo.modules.video.records.TransportBarCustomMenuItem

private const val CUSTOM_ITEM_TAG_PREFIX = "expo_video_custom_action:"

private fun itemTag(id: String) = "$CUSTOM_ITEM_TAG_PREFIX$id"

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal fun VideoView.updateTransportBarCustomMenuItems(
  playerView: PlayerView,
  items: List<TransportBarCustomMenuItem>?,
  onItemPressed: (String) -> Unit
) {
  val basicControls = playerView.findViewById<LinearLayout>(androidx.media3.ui.R.id.exo_extra_controls)
    ?: return

  val newItems = items ?: emptyList()
  val newItemIds = newItems.map { it.id }.toSet()

  // Remove buttons for items no longer in the list
  (basicControls.childCount - 1 downTo 0).forEach { index ->
    val tag = basicControls.getChildAt(index).tag as? String ?: return@forEach
    if (tag.startsWith(CUSTOM_ITEM_TAG_PREFIX)) {
      val existingId = tag.removePrefix(CUSTOM_ITEM_TAG_PREFIX)
      if (existingId !in newItemIds) {
        basicControls.removeViewAt(index)
      }
    }
  }

  // Update existing buttons or add new ones
  newItems.forEach { item ->
    val tag = itemTag(item.id)
    val existingButton = (0 until basicControls.childCount)
      .map { basicControls.getChildAt(it) }
      .firstOrNull { it.tag == tag } as? ImageButton

    if (existingButton != null) {
      TransportBarMenuItemFactory.updateButton(context, existingButton, item, onItemPressed)
    } else {
      val button = TransportBarMenuItemFactory.createButton(context, item, onItemPressed)
      button.tag = tag
      basicControls.addView(button, 0)
    }
  }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal fun VideoView.applyTransportBarCustomMenuItemsToPlayerView(playerView: PlayerView) {
  val items = transportBarCustomMenuItems ?: return
  updateTransportBarCustomMenuItems(playerView, items) { id ->
    onCustomMenuItemPressed(CustomMenuItemPressedPayload(id))
  }
}

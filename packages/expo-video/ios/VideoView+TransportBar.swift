// Copyright 2024-present 650 Industries. All rights reserved.

import AVKit
import ExpoModulesCore

#if os(tvOS)
@available(tvOS 15.0, *)
extension VideoView {
  /// Updates the transport bar custom menu items on the player view controller.
  /// Called each time the JS prop changes (including icon updates for toggle state).
  func updateTransportBarCustomMenuItems(_ items: [TransportBarCustomMenuItem]?) {
    // Clear the transport menu items if the new items are nil or empty
    guard let items, !items.isEmpty else {
      clearTransportBarCustomMenuItems()
      return
    }

    // Create UIActions for the transport bar menu items, attaching the action handler
    let actions = TransportBarMenuItemFactory.createActions(from: items) { [weak self] id in
      self?.handleCustomMenuItemPressed(id: id)
    }

    // Update the player view controller with the new transport bar menu items
    applyTransportBarActions(actions)
  }

  private func handleCustomMenuItemPressed(id: String) {
    let payload = CustomMenuItemPressedPayload()
    payload.id = id
    onCustomMenuItemPressed(payload)
  }

  private func applyTransportBarActions(_ actions: [UIAction]) {
    // HACK: Force the items to redraw by first removing them.
    // TODO: Find a better way that doesn't shift focus away from the buttons
    playerViewController.transportBarCustomMenuItems = []
    playerViewController.transportBarCustomMenuItems = actions
  }

  private func clearTransportBarCustomMenuItems() {
    playerViewController.transportBarCustomMenuItems = []
  }
}
#endif

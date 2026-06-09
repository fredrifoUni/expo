// Copyright 2024-present 650 Industries. All rights reserved.

import UIKit

/// Creates UIActions from TransportBarCustomMenuItem records for use in
/// AVPlayerViewController.transportBarCustomMenuItems.
/// Designed to be extensible for UIMenu support in the future.
internal class TransportBarMenuItemFactory {
  /// Creates an array of UIActions from an array of menu item records.
  /// - Parameters:
  ///   - items: Array of menu item records from JS.
  ///   - handler: Closure invoked with the pressed item's `id`.
  /// - Returns: Array of configured UIActions.
  static func createActions(
    from items: [TransportBarCustomMenuItem],
    handler: @escaping (String) -> Void
  ) -> [UIAction] {
    return items.map { createAction(from: $0, handler: handler) }
  }

  /// Creates a UIAction for a single menu item configuration.
  /// - Parameters:
  ///   - item: The menu item record from JS.
  ///   - handler: Closure invoked with the item's `id` when the action is triggered.
  /// - Returns: A configured UIAction.
  static func createAction(
    from item: TransportBarCustomMenuItem,
    handler: @escaping (String) -> Void
  ) -> UIAction {
    let image = UIImage(systemName: item.icon)

    return UIAction(
      title: item.title,
      image: image,
      identifier: UIAction.Identifier(item.id)
    ) { _ in
      handler(item.id)
    }
  }
}

// Copyright 2024-present 650 Industries. All rights reserved.

import ExpoModulesCore

// swiftlint:disable redundant_optional_initialization - Initialization with nil is necessary
internal struct TransportBarCustomMenuItem: Record {
  @Field
  var id: String = ""

  @Field
  var icon: String = ""

  @Field
  var title: String = ""
}
// swiftlint:enable redundant_optional_initialization

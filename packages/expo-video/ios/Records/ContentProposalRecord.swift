// Copyright 2023-present 650 Industries. All rights reserved.

import ExpoModulesCore

internal struct ContentProposalRecord: Record {
  @Field var title: String = ""
  @Field var imageUrl: URL? = nil
  @Field var showAtRemainingTime: Double = 0
}

// Copyright 2024-present 650 Industries. All rights reserved.
import AVFoundation

class VideoAdsManagerStub: VideoAdsManager {
  var player: VideoPlayer?
  var isPlayingAd = false
  var isContentFullscreen = false
  var hasMoreAds = false
  weak var delegate: VideoAdsManagerDelegate?

  // Log all stub function calls
  func logNotSupported() {
    print("VideoAdsManager - Attempted to use plugin that has not been configured.")
  }

  // Stub functions
  func prepareAds(adTagUrl: String, player: AVPlayer, videoView: VideoView) { logNotSupported() }
  func cleanup() { logNotSupported() }
  func requestAds(adDisplayContainer: AdDisplayContainer, adTagUrl: String) { logNotSupported() }
  func contentDidFinishPlaying() { logNotSupported() }
}

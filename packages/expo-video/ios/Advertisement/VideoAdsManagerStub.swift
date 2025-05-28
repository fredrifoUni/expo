// Copyright 2024-present 650 Industries. All rights reserved.
import AVFoundation

class VideoAdsManagerStub: VideoAdsManager {
    var player: VideoPlayer?
    var isPlayingAd = false
    var isContentFullscreen = false
    var hasMoreAds = false
    weak var delegate: VideoAdsManagerDelegate?
    
    // Log all stub function calls
    func logNotSupported(functionName: String = #function) {
        print("VideoAdsManager stub function triggered." + functionName)
    }
    
    // Stub functions
    func prepareAds(player: AVPlayer, videoPlayerItem: VideoPlayerItem?, videoView: VideoView?){ logNotSupported() }
    func cleanup() { logNotSupported() }
    func requestAds(adDisplayContainer: AdDisplayContainer, adTagUri: String) { logNotSupported() }
    func contentDidFinishPlaying() { logNotSupported() }
}

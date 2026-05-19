// Copyright 2024-present 650 Industries. All rights reserved.
import AVFoundation

// Create aliases to avoid conditional imports in other files
#if canImport(GoogleInteractiveMediaAds)
import GoogleInteractiveMediaAds
public typealias AdDisplayContainer = IMAAdDisplayContainer
#else
public protocol AdDisplayContainer {}
#endif

protocol VideoAdsManagerDelegate: AnyObject {
    func postrollAdFinished(_ manager: VideoAdsManager)
}

protocol VideoAdsManager: AnyObject {
    var player: VideoPlayer? { get set }
    var isPlayingAd: Bool { get set}
    var isContentFullscreen: Bool { get set }
    var hasMoreAds: Bool { get set }
    var delegate: VideoAdsManagerDelegate? { get set }

    func prepareAds(player: AVPlayer, videoPlayerItem: VideoPlayerItem?, videoView: VideoView?)
    func requestAds(adDisplayContainer: AdDisplayContainer, adTagUri: String)
    func contentDidFinishPlaying()
    func cleanup()
}

class VideoAdsManagerBuilder {
    static func create() -> VideoAdsManager {
        #if canImport(GoogleInteractiveMediaAds)
        return VideoAdsManagerIMA()
        #else
        return VideoAdsManagerStub()
        #endif
    }
}

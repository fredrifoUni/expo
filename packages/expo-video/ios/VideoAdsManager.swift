// Copyright 2024-present 650 Industries. All rights reserved.

import Foundation
import GoogleInteractiveMediaAds

protocol VideoAdsManagerDelegate: AnyObject {
    func postrollAdFinished(_ manager: VideoAdsManager)
}

class VideoAdsManager: NSObject, IMAAdsLoaderDelegate, IMAAdsManagerDelegate {
    private var adsLoader: IMAAdsLoader = IMAAdsLoader(settings: nil)
    var adsManager: IMAAdsManager!
    var contentPlayhead: IMAAVPlayerContentPlayhead?
    weak var delegate: VideoAdsManagerDelegate?
    
    // Tracked values
    var hasMoreAds: Bool = false
    var isContentCompleteCalled = true
    
    var isPlayingAd: Bool = false {
        didSet {
            // Ensures the content and Ad player are syncronized
            if isPlayingAd { player?.ref.pause() }
            else { player?.ref.play() }
        }
    }
    
    weak var player: VideoPlayer? {
        didSet {
            contentPlayhead = IMAAVPlayerContentPlayhead(avPlayer: self.player!.ref)
        }
    }
    
    func reset() {
        hasMoreAds = false
        isContentCompleteCalled = false
        isPlayingAd = false
    }
    
    override init() {
        super.init()
        self.adsLoader.delegate = self
    }
    
    // Create an ad request with our ad tag,
    public func requestAds(adDisplayContainer: IMAAdDisplayContainer, adTagUri: String){
        reset()
        let request = IMAAdsRequest(
            adTagUrl: adTagUri,
            adDisplayContainer: adDisplayContainer,
            contentPlayhead: contentPlayhead,
            userContext: nil
        )

        adsLoader.requestAds(with: request)
    }
        
    // Triggered when normal video has finished
    func contentDidFinishPlaying() {
      // Show post-roll if requested.
      isContentCompleteCalled = true
      adsLoader.contentComplete()
    }
    
    // MARK: - IMAAdsLoaderDelegate
    
    func adsLoader(_ loader: IMAAdsLoader, adsLoadedWith adsLoadedData: IMAAdsLoadedData) {
      adsManager = adsLoadedData.adsManager
      adsManager?.delegate = self
      adsManager.initialize(with: nil)
        print("AdLoader successfully initialized")
    }

    func adsLoader(_ loader: IMAAdsLoader, failedWith adErrorData: IMAAdLoadingErrorData) {
        print("Error loading ads: " + (adErrorData.adError.message ?? "No message"))
        self.isPlayingAd = false
    }
    
    // MARK: - IMAAdsManagerDelegate

    public func adsManager(_ adsManager: IMAAdsManager, didReceive event: IMAAdEvent) {
        print("AdsManager Event: " + event.typeString)
        
        switch event.type {
          case IMAAdEventType.LOADED:
            // Queue ads when they are ready
            hasMoreAds = true
            adsManager.start()
            break
          case IMAAdEventType.ALL_ADS_COMPLETED:
            hasMoreAds = false
            
            // Trigger video complete if video has finished
            if(isContentCompleteCalled) {
              self.delegate?.postrollAdFinished(self)
            }
            break
          default:
            break
        }
    }
    
    public func adsManager(_ adsManager: IMAAdsManager, didReceive error: IMAAdError) {
      // Fall back to playing content
        print("AdsManager error: " + (error.message ?? "NONE"))
        self.isPlayingAd = false
        self.hasMoreAds = false
    }
    
    public func adsManagerDidRequestContentPause(_ adsManager: IMAAdsManager) {
      // Pause the content for the SDK to play ads.
        self.isPlayingAd = true
    }

    public func adsManagerDidRequestContentResume(_ adsManager: IMAAdsManager) {
      // Resume the content since the SDK is done playing ads (at least for now).
        self.isPlayingAd = false
    }
}

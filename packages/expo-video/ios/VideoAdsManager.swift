// Copyright 2024-present 650 Industries. All rights reserved.

import Foundation
import GoogleInteractiveMediaAds

protocol VideoAdsManagerDelegate: AnyObject {
    func postrollAdFinished(_ manager: VideoAdsManager)
}

class VideoAdsManager: NSObject, IMAAdsLoaderDelegate, IMAAdsManagerDelegate {
    private var adsLoader: IMAAdsLoader = IMAAdsLoader(settings: nil)
    var adsManager: IMAAdsManager!
    var adDisplayContainer: IMAAdDisplayContainer?
    var contentPlayhead: IMAAVPlayerContentPlayhead?
    weak var delegate: VideoAdsManagerDelegate?
    var fullscreenController: UIViewController?
    
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
    
    // Keeps track of if the AVPlayer is in fullscreen
    var isContentFullscreen: Bool = false {
      
      // Set the Ad view to fullscreen if content is playing
      didSet {
        if isPlayingAd && isContentFullscreen { 
          enterFullscreen() 
        }
        else { 
          exitFullscreen() 
        }
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
    
    private func getTopViewController() -> UIViewController? {
      guard let keyWindow = UIApplication.shared.connectedScenes
        .compactMap({ ($0 as? UIWindowScene)?.windows.first(where: { $0.isKeyWindow }) })
        .first else { return nil }

      var topController = keyWindow.rootViewController
      while let presentedController = topController?.presentedViewController {
        topController = presentedController
      }
      return topController
    }
    
    // Sets the ad overlay to fullscreen mode (on top of content video fullscreen)
    public func enterFullscreen() {
      // The player is already in fullscreen
      if fullscreenController != nil { return }
        
        fullscreenController = getTopViewController()
        if let fullscreenController = fullscreenController, let adVideoView = self.adDisplayContainer?.adContainerViewController {
        fullscreenController.modalPresentationStyle = .fullScreen
        fullscreenController.present(adVideoView, animated: false, completion: nil)
      }
    }
    
    // Exit fullscreen mode
    public func exitFullscreen() {
      fullscreenController?.dismiss(animated: false) {
        self.fullscreenController = nil
      }
    }
     
    // Create an ad request with our ad tag,
    public func requestAds(adDisplayContainer: IMAAdDisplayContainer, adTagUri: String){
        self.adDisplayContainer = adDisplayContainer
        
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
          case IMAAdEventType.STARTED:
            // Set Ads to fullscreen if the AVPlayer is in fullscreen
            if isContentFullscreen { 
              enterFullscreen() 
            }
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

      // Exit fullscreen when the Ad is not playing.
      exitFullscreen()
    }
}

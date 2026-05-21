// Copyright 2024-present 650 Industries. All rights reserved.

// Only compile file if GoogleInteractiveMediaAds is available
#if canImport(GoogleInteractiveMediaAds)
  import Foundation
  import GoogleInteractiveMediaAds

  class VideoAdsManagerIMA: NSObject, IMAAdsLoaderDelegate, IMAAdsManagerDelegate, VideoAdsManager {
    private var adsLoader = IMAAdsLoader(settings: nil)
    var adsManager: IMAAdsManager?
    var adDisplayContainer: AdDisplayContainer?
    var contentPlayhead: IMAAVPlayerContentPlayhead?
    weak var delegate: VideoAdsManagerDelegate?
    var fullscreenController: UIViewController?

    // Tracked values
    var hasMoreAds = false
    var isContentCompleteCalled = true

    var isPlayingAd: Bool = false {
      didSet {
        // Ensures the content and Ad player are synchronized
        if isPlayingAd { player?.ref.pause() } else { player?.ref.play() }
      }
    }

    // Keeps track of if the AVPlayer is in fullscreen
    var isContentFullscreen: Bool = false {

      // Set the Ad view to fullscreen if content is playing
      didSet {
        if isPlayingAd && isContentFullscreen {
          enterFullscreen()
        } else {
          exitFullscreen()
        }
      }
    }

    weak var player: VideoPlayer? {
      didSet {
        if let avPlayer = player?.ref {
          contentPlayhead = IMAAVPlayerContentPlayhead(avPlayer: avPlayer)
        } else {
          contentPlayhead = nil
        }
      }
    }

    func reset() {
      hasMoreAds = false
      isContentCompleteCalled = false
      isPlayingAd = false
    }

    override init() {
      super.init()
      adsLoader.delegate = self
    }

    // This must be called before deinit due to adsManager exception - EXC_BAD_ACCESS
    func cleanup() {
      // Prevent Ad from playing sound during deinit
      adsManager?.volume = 0
      adsManager?.pause()
      player = nil

      // NOTE: Needs to run on main thread to avoid crash on iOS 17 release builds
      DispatchQueue.main.async {
        self.adsManager?.destroy()
      }
    }

    // Sets the ad overlay to fullscreen mode (on top of content video fullscreen)
    public func enterFullscreen() {
      // The player is already in fullscreen
      if fullscreenController != nil { return }
      guard fullscreenController == nil,
        let adDisplayContainer = self.adDisplayContainer,
        let adViewController = adDisplayContainer.adContainerViewController
      else {
        return
      }

      fullscreenController = UIViewController.topViewController
      fullscreenController?.modalPresentationStyle = .fullScreen
      fullscreenController?.present(adViewController, animated: false)
    }

    // Exit fullscreen mode
    public func exitFullscreen() {
      fullscreenController?.dismiss(animated: false) {
        self.fullscreenController = nil
      }
    }

    // Create an ad request with our ad tag,
    public func requestAds(
      adDisplayContainer: AdDisplayContainer,
      adTagUrl: String
    ) {
      self.adDisplayContainer = adDisplayContainer

      reset()
      let request = IMAAdsRequest(
        adTagUrl: adTagUrl,
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

    // TODO: Display loading indication for when ads are being fetched
    func prepareAds(
      adTagUrl: String,
      player: AVPlayer,
      videoView: VideoView
    ) {
      let adDisplayContainer = AdDisplayContainer(
        adContainer: videoView.playerViewController.view,
        viewController: videoView.playerViewController
      )
      requestAds(adDisplayContainer: adDisplayContainer, adTagUrl: adTagUrl)
    }

    // MARK: - IMAAdsLoaderDelegate

    func adsLoader(
      _ loader: IMAAdsLoader,
      adsLoadedWith adsLoadedData: IMAAdsLoadedData
    ) {
      adsManager = adsLoadedData.adsManager
      adsManager?.delegate = self
      adsManager?.initialize(with: nil)
      print("AdLoader successfully initialized")
    }

    func adsLoader(
      _ loader: IMAAdsLoader,
      failedWith adErrorData: IMAAdLoadingErrorData
    ) {
      print(
        "Error loading ads: " + (adErrorData.adError.message ?? "No message")
      )
      isPlayingAd = false
    }

    // MARK: - IMAAdsManagerDelegate

    public func adsManager(
      _ adsManager: IMAAdsManager,
      didReceive event: IMAAdEvent
    ) {
      print("AdsManager Event: " + event.typeString)

      switch event.type {
      case IMAAdEventType.ALL_ADS_COMPLETED:
        hasMoreAds = false

        // Trigger video complete if video has finished
        if isContentCompleteCalled {
          delegate?.postrollAdFinished(self)
        }
        break
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
      case IMAAdEventType.TAPPED:
        // Since we don't have player Controls for Ads we need to
        // always trigger `resume` when the IMA player is pressed
        adsManager.resume()
        break
      default:
        break
      }
    }

    public func adsManager(
      _ adsManager: IMAAdsManager,
      didReceive error: IMAAdError
    ) {
      // Fall back to playing content
      print("AdsManager error: " + (error.message ?? "No Message"))
      isPlayingAd = false
      hasMoreAds = false
    }

    public func adsManagerDidRequestContentPause(_ adsManager: IMAAdsManager) {
      // Pause the content for the SDK to play ads.
      isPlayingAd = true
    }

    public func adsManagerDidRequestContentResume(_ adsManager: IMAAdsManager) {
      // Resume the content since the SDK is done playing ads (at least for now).
      isPlayingAd = false

      // Exit fullscreen when the Ad is not playing.
      exitFullscreen()
    }
  }

  extension UIApplication {
    var firstKeyWindow: UIWindow? {
      return
        connectedScenes
        .compactMap { $0 as? UIWindowScene }
        .first { $0.activationState == .foregroundActive }?
        .keyWindow
    }
  }

  extension UIViewController {
    static var topViewController: UIViewController? {
      guard let keyWindow = UIApplication.shared.firstKeyWindow else {
        return nil
      }

      var topController = keyWindow.rootViewController
      while let presentedController = topController?.presentedViewController {
        topController = presentedController
      }
      return topController
    }
  }
#endif

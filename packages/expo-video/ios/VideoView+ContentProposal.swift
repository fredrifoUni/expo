// Copyright 2023-present 650 Industries. All rights reserved.

#if os(tvOS)
import AVKit
import UIKit
import ExpoModulesCore

extension VideoView {
  func setupContentProposal() {
    teardownContentProposal()

    guard let record = contentProposalRecord, let avPlayer = player?.ref else { return }

    let overlay = UpNextOverlayView()
    upNextOverlayView = overlay

    guard let contentOverlayView = playerViewController.contentOverlayView else { return }
    contentOverlayView.addSubview(overlay)
    NSLayoutConstraint.activate([
      overlay.topAnchor.constraint(equalTo: contentOverlayView.safeAreaLayoutGuide.topAnchor, constant: 40),
      overlay.trailingAnchor.constraint(equalTo: contentOverlayView.safeAreaLayoutGuide.trailingAnchor, constant: -40)
    ])

    upNextTimeObserverPlayer = avPlayer
    let interval = CMTimeMakeWithSeconds(0.5, preferredTimescale: 600)
    upNextTimeObserver = avPlayer.addPeriodicTimeObserver(forInterval: interval, queue: .main) { [weak self, weak avPlayer] time in
      guard let self, let avPlayer, let record = self.contentProposalRecord else { return }
      let duration = avPlayer.currentItem?.duration ?? .invalid
      guard duration.isNumeric, duration > .zero else {
        overlay.hide()
        return
      }
      let remaining = duration.seconds - time.seconds
      if remaining > 0 && remaining <= record.showAtRemainingTime {
        overlay.show(title: record.title, imageUrl: record.imageUrl)
      } else {
        overlay.hide()
      }
    }
  }

  func teardownContentProposal() {
    if let observer = upNextTimeObserver {
      upNextTimeObserverPlayer?.removeTimeObserver(observer)
      upNextTimeObserver = nil
      upNextTimeObserverPlayer = nil
    }
    upNextOverlayView?.removeFromSuperview()
    upNextOverlayView = nil
  }
}
#endif

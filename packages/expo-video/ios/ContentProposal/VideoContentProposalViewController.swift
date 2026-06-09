// Copyright 2023-present 650 Industries. All rights reserved.

#if os(tvOS)
import UIKit
import ExpoModulesCore

internal class UpNextOverlayView: UIView {
  private let cardView: UIView = {
    let view = UIView()
    view.backgroundColor = UIColor(white: 0, alpha: 0.8)
    view.layer.cornerRadius = 12
    view.clipsToBounds = true
    view.translatesAutoresizingMaskIntoConstraints = false
    return view
  }()

  private let previewImageView: UIImageView = {
    let imageView = UIImageView()
    imageView.contentMode = .scaleAspectFill
    imageView.clipsToBounds = true
    imageView.layer.cornerRadius = 6
    imageView.backgroundColor = UIColor.black
    imageView.translatesAutoresizingMaskIntoConstraints = false
    return imageView
  }()

  private let upNextLabel: UILabel = {
    let label = UILabel()
    label.text = "UP NEXT"
    label.font = UIFont.systemFont(ofSize: 24, weight: .bold)
    label.textColor = .white
    label.translatesAutoresizingMaskIntoConstraints = false
    return label
  }()

  private let titleLabel: UILabel = {
    let label = UILabel()
    label.font = UIFont.systemFont(ofSize: 24, weight: .medium)
    label.textColor = .white
    label.numberOfLines = 2
    label.lineBreakMode = .byTruncatingTail
    label.translatesAutoresizingMaskIntoConstraints = false
    return label
  }()

  private var pendingImageTask: URLSessionDataTask?

  override init(frame: CGRect) {
    super.init(frame: frame)
    translatesAutoresizingMaskIntoConstraints = false
    isHidden = true
    setupCardView()
  }

  required init?(coder: NSCoder) {
    fatalError("init(coder:) has not been implemented")
  }

  func show(title: String, imageUrl: URL?) {
    titleLabel.text = title
    isHidden = false

    pendingImageTask?.cancel()
    pendingImageTask = nil

    if let imageUrl {
      previewImageView.isHidden = false
      let task = URLSession.shared.dataTask(with: imageUrl) { [weak self] data, response, _ in
        guard let data, response is HTTPURLResponse, let image = UIImage(data: data) else { return }
        DispatchQueue.main.async {
          guard let self, !self.isHidden else { return }
          self.previewImageView.image = image
        }
      }
      task.resume()
      pendingImageTask = task
    } else {
      previewImageView.image = nil
      previewImageView.isHidden = true
    }
  }

  func hide() {
    pendingImageTask?.cancel()
    pendingImageTask = nil
    isHidden = true
  }

  private func setupCardView() {
    let textStack = UIStackView(arrangedSubviews: [upNextLabel, titleLabel])
    textStack.axis = .vertical
    textStack.alignment = .leading
    textStack.spacing = 0
    textStack.translatesAutoresizingMaskIntoConstraints = false

    addSubview(cardView)
    cardView.addSubview(previewImageView)
    cardView.addSubview(textStack)

    let padding: CGFloat = 18
    let imageTextGap: CGFloat = 12

    NSLayoutConstraint.activate([
      cardView.topAnchor.constraint(equalTo: topAnchor),
      cardView.leadingAnchor.constraint(equalTo: leadingAnchor),
      cardView.trailingAnchor.constraint(equalTo: trailingAnchor),
      cardView.bottomAnchor.constraint(equalTo: bottomAnchor),
      cardView.widthAnchor.constraint(equalToConstant: 600),
      cardView.heightAnchor.constraint(equalToConstant: 140),

      previewImageView.leadingAnchor.constraint(equalTo: cardView.leadingAnchor, constant: padding),
      previewImageView.topAnchor.constraint(equalTo: cardView.topAnchor, constant: padding),
      previewImageView.bottomAnchor.constraint(equalTo: cardView.bottomAnchor, constant: -padding),
      previewImageView.widthAnchor.constraint(equalTo: previewImageView.heightAnchor, multiplier: 16.0 / 9.0),

      textStack.leadingAnchor.constraint(equalTo: previewImageView.trailingAnchor, constant: imageTextGap),
      textStack.trailingAnchor.constraint(equalTo: cardView.trailingAnchor, constant: -padding),
      textStack.centerYAnchor.constraint(equalTo: cardView.centerYAnchor)
    ])
  }
}
#endif

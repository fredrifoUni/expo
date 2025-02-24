package expo.modules.video.player

import android.content.Context
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.util.Log
import android.view.SurfaceView
import android.view.ViewGroup
import android.webkit.URLUtil
import androidx.core.content.getSystemService
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_BUFFERING
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import androidx.media3.ui.PlayerView.switchTargetView
import androidx.multidex.MultiDex
import com.google.ads.interactivemedia.v3.api.AdErrorEvent
import com.google.ads.interactivemedia.v3.api.AdErrorEvent.AdErrorListener
import com.google.ads.interactivemedia.v3.api.AdEvent
import com.google.ads.interactivemedia.v3.api.AdEvent.AdEventListener
import com.google.ads.interactivemedia.v3.api.AdEvent.AdEventType
import com.google.ads.interactivemedia.v3.api.AdsLoader
import com.google.ads.interactivemedia.v3.api.AdsManager
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory
import com.google.ads.interactivemedia.v3.api.player.AdMediaInfo
import com.google.ads.interactivemedia.v3.api.player.ContentProgressProvider
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer.VideoAdPlayerCallback
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate
import expo.modules.kotlin.AppContext
import expo.modules.kotlin.sharedobjects.SharedObject
import expo.modules.video.IntervalUpdateClock
import expo.modules.video.IntervalUpdateEmitter
import expo.modules.video.VideoAdPlayerAdapter
import expo.modules.video.VideoManager
import expo.modules.video.delegates.IgnoreSameSet
import expo.modules.video.enums.AudioMixingMode
import expo.modules.video.enums.PlayerStatus
import expo.modules.video.enums.PlayerStatus.ERROR
import expo.modules.video.enums.PlayerStatus.IDLE
import expo.modules.video.enums.PlayerStatus.LOADING
import expo.modules.video.enums.PlayerStatus.READY_TO_PLAY
import expo.modules.video.playbackService.ExpoVideoPlaybackService
import expo.modules.video.playbackService.PlaybackServiceConnection
import expo.modules.video.records.BufferOptions
import expo.modules.video.records.PlaybackError
import expo.modules.video.records.TimeUpdate
import expo.modules.video.records.VideoSource
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.lang.ref.WeakReference


// https://developer.android.com/guide/topics/media/media3/getting-started/migration-guide#improvements_in_media3
@UnstableApi
class VideoPlayer(val context: Context, appContext: AppContext, source: VideoSource?) : AutoCloseable, SharedObject(appContext), IntervalUpdateEmitter {
  // This improves the performance of playing DRM-protected content
  private var activePlayerView: PlayerView? = null
  private var isIMAInitialized = false
  private var renderersFactory = DefaultRenderersFactory(context)
    .forceEnableMediaCodecAsynchronousQueueing()
    .setEnableDecoderFallback(true)
  private var listeners: MutableList<WeakReference<VideoPlayerListener>> = mutableListOf()
  val loadControl: VideoPlayerLoadControl = VideoPlayerLoadControl.Builder().build()
  val subtitles: VideoPlayerSubtitles = VideoPlayerSubtitles(this)
  val trackSelector = DefaultTrackSelector(context)

  private var sdkFactory: ImaSdkFactory? = null // Factory class for creating SDK objects.
  private var adsLoader: AdsLoader? = null // The AdsLoader instance exposes the requestAds method.
  private var adsManager: AdsManager? = null // AdsManager exposes methods to control ad playback and listen to ad events.
  private var savedPosition = 0 // The saved content position, used to resumed content following an ad break.
  private var videoAdPlayerAdapter: VideoAdPlayerAdapter? = null

  val player = ExoPlayer
    .Builder(context, renderersFactory)
    .setLooper(context.mainLooper)
    .setLoadControl(loadControl)
    .build()

  val serviceConnection = PlaybackServiceConnection(WeakReference(this))
  val intervalUpdateClock = IntervalUpdateClock(this)

  var playing by IgnoreSameSet(false) { new, old ->
    sendEvent(PlayerEvent.IsPlayingChanged(new, old))
  }

  var uncommittedSource: VideoSource? = source
  private var lastLoadedSource by IgnoreSameSet<VideoSource?>(null) { new, old ->
    sendEvent(PlayerEvent.SourceChanged(new, old))
  }

  // Volume of the player if there was no mute applied.
  var userVolume = 1f
  var status: PlayerStatus = IDLE
  var requiresLinearPlayback = false
  var staysActiveInBackground = false
  var preservesPitch = false
    set(preservesPitch) {
      field = preservesPitch
      playbackParameters = applyPitchCorrection(playbackParameters)
    }
  var showNowPlayingNotification = false
    set(value) {
      field = value
      serviceConnection.playbackServiceBinder?.service?.setShowNotification(value, this.player)
    }
  var duration = 0f
  var isLive = false

  var volume: Float by IgnoreSameSet(1f) { new: Float, old: Float ->
    player.volume = if (muted) 0f else new
    userVolume = volume
    sendEvent(PlayerEvent.VolumeChanged(new, old))
  }

  var muted: Boolean by IgnoreSameSet(false) { new: Boolean, old: Boolean ->
    player.volume = if (new) 0f else userVolume
    sendEvent(PlayerEvent.MutedChanged(new, old))
  }

  var playbackParameters by IgnoreSameSet(
    PlaybackParameters.DEFAULT,
    propertyMapper = { applyPitchCorrection(it) }
  ) { new: PlaybackParameters, old: PlaybackParameters ->
    player.playbackParameters = new

    if (old.speed != new.speed) {
      sendEvent(PlayerEvent.PlaybackRateChanged(new.speed, old.speed))
    }
  }

  val currentOffsetFromLive: Float?
    get() {
      return if (player.currentLiveOffset == C.TIME_UNSET) {
        null
      } else {
        player.currentLiveOffset / 1000f
      }
    }

  val currentLiveTimestamp: Long?
    get() {
      val window = Timeline.Window()
      if (!player.currentTimeline.isEmpty) {
        player.currentTimeline.getWindow(player.currentMediaItemIndex, window)
      }
      if (window.windowStartTimeMs == C.TIME_UNSET) {
        return null
      }
      return window.windowStartTimeMs + player.currentPosition
    }

  var bufferOptions: BufferOptions = BufferOptions()
    set(value) {
      field = value
      loadControl.applyBufferOptions(value)
    }

  val bufferedPosition: Double
    get() {
      if (player.currentMediaItem == null) {
        return -1.0
      }
      if (player.playbackState == STATE_BUFFERING) {
        return 0.0
      }
      return player.bufferedPosition / 1000.0
    }

  var audioMixingMode: AudioMixingMode = AudioMixingMode.AUTO
    set(value) {
      val old = field
      field = value
      sendEvent(PlayerEvent.AudioMixingModeChanged(value, old))
    }

  fun buildAdEventListener(): AdEventListener {
    return AdEventListener { event: AdEvent ->
      val eventType = event.type
      Log.d("IMA", "Received AD Event: $eventType")
    }
  }

  fun buildAdErrorListener(): AdErrorListener {
    return AdErrorListener { event: AdErrorEvent ->
      val eventType = event.error.message
      Log.e("IMA", "Received AD Error: $eventType")
    }
  }

  fun buildAdPlayerCallback(): VideoAdPlayerCallback {
    return object : VideoAdPlayerCallback {
      override fun onPlay(adMediaInfo: AdMediaInfo) {
        Log.d("IMA", "Ad started playing: ${adMediaInfo.url}")
      }

      override fun onPause(adMediaInfo: AdMediaInfo) {
        Log.d("IMA", "Ad paused: ${adMediaInfo.url}")
      }

      override fun onResume(adMediaInfo: AdMediaInfo) {
        Log.d("IMA", "Ad resumed: ${adMediaInfo.url}")
      }

      override fun onVolumeChanged(adMediaInfo: AdMediaInfo, p1: Int) {
        Log.d("IMA", "Ad volume changed: ${adMediaInfo.url}")
      }

      override fun onAdProgress(adMediaInfo: AdMediaInfo, p1: VideoProgressUpdate) {
        Log.d("IMA", "Ad progress: ${adMediaInfo.url}")
      }

      override fun onBuffering(adMediaInfo: AdMediaInfo) {
        Log.d("IMA", "Ad buffer: ${adMediaInfo.url}")
      }

      override fun onContentComplete() {
        Log.d("IMA", "Ad completed")
      }

      override fun onEnded(adMediaInfo: AdMediaInfo) {
        Log.d("IMA", "Ad ended: ${adMediaInfo.url}")
      }

      override fun onError(adMediaInfo: AdMediaInfo) {
        Log.e("IMA", "Received AD Error: ${adMediaInfo.url}")
      }

      override fun onLoaded(adMediaInfo: AdMediaInfo) {
        Log.d("IMA", "Ad ended: ${adMediaInfo.url}")
      }
    }
  }


  private fun initializeIMA() {
    if( isIMAInitialized || activePlayerView == null ){ return }

    isIMAInitialized = true

    activePlayerView!!.setPlayer(player);
    Log.d("IMA", "Player is configured to display Ads")
    isIMAInitialized = true

    val videoPlayerContainer: ViewGroup = activePlayerView as PlayerView
    val audioManager = context.getSystemService<AudioManager>()
    videoAdPlayerAdapter = VideoAdPlayerAdapter(player, audioManager!!)

    sdkFactory = ImaSdkFactory.getInstance()
    val adDisplayContainer =
      ImaSdkFactory.createAdDisplayContainer(videoPlayerContainer, videoAdPlayerAdapter!!)



    // Create an AdsLoader
    val settings = sdkFactory!!.createImaSdkSettings()
    adsLoader = sdkFactory!!.createAdsLoader(context, settings, adDisplayContainer)


    // Add listeners for when ads are loaded and for errors.
    adsLoader!!.addAdErrorListener(
      AdErrorListener { adErrorEvent ->

        /** An event raised when there is an error loading or playing ads.  */
        /** An event raised when there is an error loading or playing ads.  */
        Log.i("IMA", "Ad Error: " + adErrorEvent.error.message)
        resumeContent()
      })
    adsLoader?.addAdsLoadedListener { adsManagerLoadedEvent -> // Ads were successfully loaded, so get the AdsManager instance. AdsManager has
      // events for ad playback and errors.
      adsManager = adsManagerLoadedEvent.adsManager

      // Attach event and error event listeners.
      adsManager?.addAdErrorListener(
        AdErrorListener { adErrorEvent ->
          /** An event raised when there is an error loading or playing ads.  */
          Log.e("IMA", "Ad Error: " + adErrorEvent.error.message)
          val universalAdIds = adsManager?.currentAd?.universalAdIds.contentToString()
          Log.i(
            "IMA",
            ("Discarding the current ad break with universal "
              + "ad Ids: "
              + universalAdIds))
          adsManager!!.discardAdBreak()
        })

      adsManager?.addAdEventListener { adEvent ->

        /** Responds to AdEvents.  */
        if (adEvent.type != AdEventType.AD_PROGRESS) {
          Log.i("IMA", "Event: " + adEvent.type)
        }
        // These are the suggested event types to handle. For full list of
        // all ad event types, see AdEvent.AdEventType documentation.
        when (adEvent.type) {
          AdEventType.LOADED ->                         // AdEventType.LOADED is fired when ads are ready to play.

            // This sample app uses the sample tag
            // single_preroll_skippable_ad_tag_url that requires calling
            // AdsManager.start() to start ad playback.
            // If you use a different ad tag URL that returns a VMAP or
            // an ad rules playlist, the adsManager.init() function will
            // trigger ad playback automatically and the IMA SDK will
            // ignore the adsManager.start().
            // It is safe to always call adsManager.start() in the
            // LOADED event.
            adsManager?.start()

          AdEventType.CONTENT_PAUSE_REQUESTED ->                         // AdEventType.CONTENT_PAUSE_REQUESTED is fired when you
            // should pause your content and start playing an ad.
            pauseContentForAds()

          AdEventType.CONTENT_RESUME_REQUESTED ->                         // AdEventType.CONTENT_RESUME_REQUESTED is fired when the ad
            // you should play your content.
            resumeContent()

          AdEventType.ALL_ADS_COMPLETED -> {
            // Calling adsManager.destroy() triggers the function
            // VideoAdPlayer.release().
            adsManager?.destroy()
            adsManager = null
          }

          AdEventType.AD_PROGRESS ->
            Log.d("IMA", "AD PROGRESS")

          AdEventType.CLICKED -> {}
          else -> {}
        }
      }
      val adsRenderingSettings = ImaSdkFactory.getInstance().createAdsRenderingSettings()
      adsManager?.init(adsRenderingSettings)
    }

    prepare() // HACK: Re-prep sources now that IMA are initialized and ready to be injected

    // Request ads
    lastLoadedSource?.advertisement?.googleIMA?.adTagUri?.let {
      requestAds(it)
    };
  }

  private fun pauseContentForAds() {
    Log.i("IMA", "pauseContentForAds")
    savedPosition = player.currentPosition.toInt()
    player.stop()
    // Hide the buttons and seek bar controlling the video view.
//    player.setMediaController(null)
  }

  private fun resumeContent() {
    Log.i("IMA", "resumeContent")

    // Restore media item
    prepare(true)

    // Restore position and play
    player.seekTo(savedPosition.toLong())
    player.play()

    // Show ExoPlayer's controls
//    activePlayerView?.useController = true

    // Detect when content playback is completed
    player.addListener(object : Player.Listener {
      override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_ENDED) {
          videoAdPlayerAdapter?.notifyImaOnContentCompleted()
        }
      }
    })
  }

  private val playerListener = object : Player.Listener {
    override fun onIsPlayingChanged(isPlaying: Boolean) {
      this@VideoPlayer.playing = isPlaying
    }

    override fun onTracksChanged(tracks: Tracks) {
      val oldSubtitleTracks = ArrayList(subtitles.availableSubtitleTracks)
      val oldCurrentTrack = subtitles.currentSubtitleTrack
      sendEvent(PlayerEvent.TracksChanged(tracks))

      val newSubtitleTracks = subtitles.availableSubtitleTracks
      val newCurrentSubtitleTrack = subtitles.currentSubtitleTrack

      if (!oldSubtitleTracks.toArray().contentEquals(newSubtitleTracks.toArray())) {
        sendEvent(PlayerEvent.AvailableSubtitleTracksChanged(newSubtitleTracks, oldSubtitleTracks))
      }
      if (oldCurrentTrack != newCurrentSubtitleTrack) {
        sendEvent(PlayerEvent.SubtitleTrackChanged(newCurrentSubtitleTrack, oldCurrentTrack))
      }
      super.onTracksChanged(tracks)
    }

    override fun onTrackSelectionParametersChanged(parameters: TrackSelectionParameters) {
      val oldTrack = subtitles.currentSubtitleTrack
      sendEvent(PlayerEvent.TrackSelectionParametersChanged(parameters))

      val newTrack = subtitles.currentSubtitleTrack
      sendEvent(PlayerEvent.SubtitleTrackChanged(newTrack, oldTrack))
      super.onTrackSelectionParametersChanged(parameters)
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
      this@VideoPlayer.duration = 0f
      this@VideoPlayer.isLive = false
      if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
        sendEvent(PlayerEvent.PlayedToEnd())
      }
      subtitles.setSubtitlesEnabled(false)
      super.onMediaItemTransition(mediaItem, reason)
    }

    override fun onPlaybackStateChanged(@Player.State playbackState: Int) {
      if (playbackState == Player.STATE_IDLE && player.playerError != null) {
        return
      }
      if (playbackState == Player.STATE_READY) {
        this@VideoPlayer.duration = this@VideoPlayer.player.duration / 1000f
        this@VideoPlayer.isLive = this@VideoPlayer.player.isCurrentMediaItemLive
      }
      setStatus(playerStateToPlayerStatus(playbackState), null)
      super.onPlaybackStateChanged(playbackState)
    }

    override fun onVolumeChanged(volume: Float) {
      if (!muted) {
        this@VideoPlayer.volume = volume
      }
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
      this@VideoPlayer.playbackParameters = playbackParameters
      super.onPlaybackParametersChanged(playbackParameters)
    }

    override fun onPlayerErrorChanged(error: PlaybackException?) {
      error?.let {
        this@VideoPlayer.duration = 0f
        this@VideoPlayer.isLive = false
        setStatus(ERROR, error)
      } ?: run {
        setStatus(playerStateToPlayerStatus(player.playbackState), null)
      }

      super.onPlayerErrorChanged(error)
    }
  }

  init {
    // Because of the size of the ExoPlayer IMA extension, implement and enable multidex here.
    MultiDex.install(context);

    ExpoVideoPlaybackService.startService(appContext, context, serviceConnection)
    player.addListener(playerListener)
    VideoManager.registerVideoPlayer(this)

    // ExoPlayer will enable subtitles automatically at the start, we want them disabled by default
    appContext.mainQueue.launch {
      subtitles.setSubtitlesEnabled(false)
    }
  }

  override fun close() {
    appContext?.reactContext?.unbindService(serviceConnection)
    serviceConnection.playbackServiceBinder?.service?.unregisterPlayer(player)
    VideoManager.unregisterVideoPlayer(this@VideoPlayer)

    appContext?.mainQueue?.launch {
      player.removeListener(playerListener)
      player.release()
    }
    uncommittedSource = null
    lastLoadedSource = null
  }

  override fun deallocate() {
    super.deallocate()
    close()
  }

  fun changePlayerView(newPlayerView: PlayerView) {
    player.clearVideoSurface()
    player.setVideoSurfaceView(newPlayerView.videoSurfaceView as SurfaceView?)

    switchTargetView(player, activePlayerView, newPlayerView)
    activePlayerView = newPlayerView
    initializeIMA()
  }

  // HACK: This runs twice at startup due to videoView being unavailable early in the lifecycle.
  // Advertisement setup depends on videoView being available.
  fun prepare(alreadyPrepared: Boolean = false) {
    val newSource = if (alreadyPrepared) lastLoadedSource else uncommittedSource
    val mediaSource = newSource?.toMediaSource(context, activePlayerView)

    Log.d("IMA", "Prepping source ${mediaSource?.mediaItem.toString()}")
    mediaSource?.let {
      player.setMediaSource(it)
      player.prepare()
      player.playWhenReady = true // TODO: This should be configured in props or only for IMA Ads
      lastLoadedSource = newSource
      uncommittedSource = null
    } ?: run {
      player.clearMediaItems()
    }
  }

  private fun requestAds(adTagUrl: String) {
    Log.d("IMA", "Requesting Ads")
    // Create the ads request.
    val request = sdkFactory!!.createAdsRequest()
    request.adTagUrl = adTagUrl
    request.contentProgressProvider = ContentProgressProvider {
      if (player.duration <= 0) {
        return@ContentProgressProvider VideoProgressUpdate.VIDEO_TIME_NOT_READY
      }
      return@ContentProgressProvider VideoProgressUpdate(player.currentPosition, player.duration)
    }

    // Request the ad. After the ad is loaded, onAdsManagerLoaded() will be called.
    adsLoader!!.requestAds(request)
  }

  private fun applyPitchCorrection(playbackParameters: PlaybackParameters): PlaybackParameters {
    val speed = playbackParameters.speed
    val pitch = if (preservesPitch) 1f else speed
    return PlaybackParameters(speed, pitch)
  }

  private fun playerStateToPlayerStatus(@Player.State state: Int): PlayerStatus {
    return when (state) {
      Player.STATE_IDLE -> IDLE
      Player.STATE_BUFFERING -> LOADING
      Player.STATE_READY -> READY_TO_PLAY
      Player.STATE_ENDED -> {
        // When an error occurs, the player state changes to ENDED.
        if (player.playerError != null) {
          ERROR
        } else {
          IDLE
        }
      }

      else -> IDLE
    }
  }

  private fun setStatus(status: PlayerStatus, error: PlaybackException?) {
    val oldStatus = this.status
    this.status = status

    val playbackError = error?.let {
      PlaybackError(it)
    }

    if (playbackError == null && player.playbackState == Player.STATE_ENDED) {
      sendEvent(PlayerEvent.PlayedToEnd())
    }

    if (this.status != oldStatus) {
      sendEvent(PlayerEvent.StatusChanged(status, oldStatus, playbackError))
    }
  }

  fun addListener(videoPlayerListener: VideoPlayerListener) {
    if (listeners.all { it.get() != videoPlayerListener }) {
      listeners.add(WeakReference(videoPlayerListener))
    }
  }

  fun removeListener(videoPlayerListener: VideoPlayerListener) {
    listeners.removeAll { it.get() == videoPlayerListener }
  }

  private fun sendEvent(event: PlayerEvent) {
    // Emits to the native listeners
    event.emit(this, listeners.mapNotNull { it.get() })
    // Emits to the JS side
    if (event.emitToJS) {
      emit(event.name, event.jsEventPayload)
    }
  }

  // IntervalUpdateEmitter
  override fun emitTimeUpdate() {
    appContext?.mainQueue?.launch {
      val updatePayload = TimeUpdate(player.currentPosition / 1000.0, currentOffsetFromLive, currentLiveTimestamp, bufferedPosition)
      sendEvent(PlayerEvent.TimeUpdated(updatePayload))
    }
  }

  fun toMetadataRetriever(): MediaMetadataRetriever {
    val source = uncommittedSource ?: lastLoadedSource
    val uri = source?.uri ?: throw IllegalStateException("Video source is not set")
    val stringUri = uri.toString()

    val mediaMetadataRetriever = MediaMetadataRetriever()
    if (URLUtil.isFileUrl(stringUri)) {
      mediaMetadataRetriever.setDataSource(stringUri.replace("file://", ""))
    } else if (URLUtil.isContentUrl(stringUri)) {
      context.contentResolver.openFileDescriptor(uri, "r")?.use { parcelFileDescriptor ->
        FileInputStream(parcelFileDescriptor.fileDescriptor).use { inputStream ->
          mediaMetadataRetriever.setDataSource(inputStream.fd)
        }
      }
    } else {
      mediaMetadataRetriever.setDataSource(stringUri, source.headers ?: emptyMap())
    }
    return mediaMetadataRetriever
  }
}

import { useVideoPlayer, VideoSource, VideoView } from 'expo-video';
import React, { useCallback, useRef, useState } from 'react';
import { ScrollView, View } from 'react-native';

import { SAMPLE_ADS } from './VideoAdvertisementSources';
import { bigBuckBunnySource } from './videoSources';
import { styles } from './videoStyles';
import Button from '../../components/Button';

export default function VideoAdvertisementScreen() {
  const ref = useRef<VideoView>(null);
  const [isFullscreen, setIsFullscreen] = useState(false);

  const player = useVideoPlayer(
    {
      ...bigBuckBunnySource,
      advertisement: {
        googleIMA: {
          adTagUri: SAMPLE_ADS.preMidPost,
        },
      },
    } as VideoSource,
    (player) => {
      player.loop = false;
      player.showNowPlayingNotification = false;
      player.play();
    }
  );

  const toggleFullscreen = useCallback(() => {
    if (!isFullscreen) {
      ref.current?.enterFullscreen();
    } else {
      ref.current?.exitFullscreen();
    }
  }, [player]);

  return (
    <View style={styles.contentContainer}>
      <VideoView
        ref={ref}
        player={player}
        onFullscreenEnter={() => {
          console.log('Entered Fullscreen');
          setIsFullscreen(true);
        }}
        onFullscreenExit={() => {
          console.log('Exited Fullscreen');
          setIsFullscreen(false);
        }}
        style={styles.video}
      />
      <ScrollView style={styles.controlsContainer}>
        <Button style={styles.button} title="Enter Fullscreen" onPress={toggleFullscreen} />
      </ScrollView>
    </View>
  );
}

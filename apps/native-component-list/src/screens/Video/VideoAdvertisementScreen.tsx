import { useVideoPlayer, VideoView } from 'expo-video';
import React, { useCallback, useRef, useState } from 'react';
import { ScrollView, View } from 'react-native';

import { SAMPLE_ADS } from './VideoAdvertisementSources';
import { bigBuckBunnySource } from './videoSources';
import { styles } from './videoStyles';
import Button from '../../components/Button';
import ConsoleBox from '../../components/ConsoleBox';

type PlayerStatus = {
  playing: boolean;
  playingAd: boolean;
};

export default function VideoAdvertisementScreen() {
  const ref = useRef<VideoView>(null);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [playerStatus, setPlayerStatus] = useState<PlayerStatus | null>(null);
  const playerStatusFormatted = JSON.stringify(playerStatus ?? {}, null, 2);

  const player = useVideoPlayer(
    {
      ...bigBuckBunnySource,
      advertisement: {
        googleIMA: {
          adTagUrl: SAMPLE_ADS.preMidPost,
        },
      },
    },
    (player) => {
      player.loop = false;
      player.showNowPlayingNotification = false;
      player.play();
    }
  );

  const refreshPlayerStatus = () => {
    setPlayerStatus({
      playing: player.playing,
      playingAd: player.playingAd,
    });
  };

  const toggleFullscreen = useCallback(() => {
    if (!isFullscreen) {
      ref.current?.enterFullscreen();
    } else {
      ref.current?.exitFullscreen();
    }
  }, [player]);

  const onFullscreenEnter = () => {
    console.log('Entered Fullscreen');
    setIsFullscreen(true);
  };

  const onFullscreenExit = () => {
    console.log('Exited Fullscreen');
    setIsFullscreen(false);
  };

  return (
    <View style={styles.contentContainer}>
      <VideoView
        ref={ref}
        player={player}
        onFullscreenEnter={onFullscreenEnter}
        onFullscreenExit={onFullscreenExit}
        style={styles.video}
      />
      <ScrollView style={styles.controlsContainer}>
        <Button style={styles.button} title="Enter Fullscreen" onPress={toggleFullscreen} />
        <Button style={styles.button} title="Refresh Player Status" onPress={refreshPlayerStatus} />
        <ConsoleBox style={styles.consoleBox}>playerStatus: {playerStatusFormatted}</ConsoleBox>
      </ScrollView>
    </View>
  );
}

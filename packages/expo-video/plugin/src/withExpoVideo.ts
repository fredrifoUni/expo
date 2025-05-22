import { updateAndroidBuildProperty } from '@expo/config-plugins/build/android/BuildProperties';
import { withPermissions } from '@expo/config-plugins/build/android/Permissions';
import {
  AndroidConfig,
  type ConfigPlugin,
  withInfoPlist,
  withAndroidManifest,
  withPodfileProperties,
  withGradleProperties,
} from 'expo/config-plugins';

type WithExpoVideoOptions = {
  supportsBackgroundPlayback?: boolean;
  supportsInteractiveMediaAds?: boolean;
  supportsPictureInPicture?: boolean;
};

const withExpoVideo: ConfigPlugin<WithExpoVideoOptions> = (
  config,
  { supportsBackgroundPlayback, supportsInteractiveMediaAds = false, supportsPictureInPicture } = {}
) => {
  if (supportsInteractiveMediaAds) {
    withPodfileProperties(config, (config) => {
      config.modResults.useInteractiveMediaAds = supportsInteractiveMediaAds.toString();
      return config;
    });

    withGradleProperties(config, (config) => {
      config.modResults = updateAndroidBuildProperty(
        config.modResults,
        'expo.video.useInteractiveMediaAds',
        'true'
      );
      return config;
    });

    withPermissions(config, ['android.permission.ACCESS_NETWORK_STATE']);
  }

  withInfoPlist(config, (config) => {
    const currentBackgroundModes = config.modResults.UIBackgroundModes ?? [];
    const shouldEnableBackgroundAudio = supportsBackgroundPlayback || supportsPictureInPicture;

    // No-op if the values are not defined
    if (
      typeof supportsBackgroundPlayback === 'undefined' &&
      typeof supportsPictureInPicture === 'undefined'
    ) {
      return config;
    }

    if (shouldEnableBackgroundAudio && !currentBackgroundModes.includes('audio')) {
      config.modResults.UIBackgroundModes = [...currentBackgroundModes, 'audio'];
    } else if (!shouldEnableBackgroundAudio) {
      config.modResults.UIBackgroundModes = currentBackgroundModes.filter(
        (mode: string) => mode !== 'audio'
      );
    }
    return config;
  });

  withAndroidManifest(config, (config) => {
    const activity = AndroidConfig.Manifest.getMainActivityOrThrow(config.modResults);

    // No-op if the values are not defined
    if (typeof supportsPictureInPicture === 'undefined') {
      return config;
    }

    if (supportsPictureInPicture) {
      activity.$['android:supportsPictureInPicture'] = 'true';
    } else {
      delete activity.$['android:supportsPictureInPicture'];
    }
    return config;
  });
  return config;
};

export default withExpoVideo;

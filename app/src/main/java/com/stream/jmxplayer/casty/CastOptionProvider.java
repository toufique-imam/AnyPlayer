package com.stream.jmxplayer.casty;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.cast.LaunchOptions;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;
import com.google.android.gms.cast.framework.media.CastMediaOptions;
import com.google.android.gms.cast.framework.media.MediaIntentReceiver;
import com.google.android.gms.cast.framework.media.NotificationOptions;
import com.stream.jmxplayer.ui.ExpandedControlsActivity;

import java.util.Arrays;
import java.util.List;

public class CastOptionProvider implements OptionsProvider {
    @Override
    public CastOptions getCastOptions(Context context) {
        List<String> buttonActions = createButtonActions();
        int[] compatButtonAction = {1, 3};

        NotificationOptions notificationOptions = new NotificationOptions.Builder()
                .setActions(buttonActions, compatButtonAction)
                .setTargetActivityClassName(ExpandedControlsActivity.class.getName())
                .build();

        CastMediaOptions mediaOptions = new CastMediaOptions.Builder()
                .setNotificationOptions(notificationOptions)
                .setExpandedControllerActivityClassName(ExpandedControlsActivity.class.getName())
                .build();
        LaunchOptions launchOptions = new LaunchOptions.Builder()
                .setRelaunchIfRunning(true)
                .setAndroidReceiverCompatible(true).build();

        return new CastOptions.Builder()
                .setReceiverApplicationId(Casty.receiverId)
                .setCastMediaOptions(mediaOptions)
                .setLaunchOptions(launchOptions)
                .build();

    }

    @Nullable
    @Override
    public List<SessionProvider> getAdditionalSessionProviders(@NonNull Context context) {
        return null;
    }

    private List<String> createButtonActions() {
        return Arrays.asList(MediaIntentReceiver.ACTION_REWIND,
                MediaIntentReceiver.ACTION_TOGGLE_PLAYBACK,
                MediaIntentReceiver.ACTION_FORWARD,
                MediaIntentReceiver.ACTION_STOP_CASTING);
    }
}

package com.code.wlu.cp470.wellnest.utils;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;

import com.code.wlu.cp470.wellnest.R;

public class MusicService extends Service {

    public static final String ACTION_PAUSE = "MusicService.PAUSE";
    public static final String ACTION_RESUME = "MusicService.RESUME";
    private MediaPlayer player;
    private int[] playlist = {
            R.raw.track_1_dexelated,
            R.raw.track_2_honey_pot,
            R.raw.track_3_skys_lullaby
    };
    private int index = 0;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_PAUSE:
                    pauseMusic();
                    return START_STICKY;

                case ACTION_RESUME:
                    resumeMusic();
                    return START_STICKY;
            }
        }

        // Normal start
        startSong();
        return START_STICKY;
    }


    private void startSong() {
        if (player != null) {
            player.release();
        }

        player = MediaPlayer.create(this, playlist[index]);
        player.setOnCompletionListener(mp -> {
            index = (index + 1) % playlist.length;   // loop through all 3
            startSong();
        });

        player.start();
    }

    @Override
    public void onDestroy() {
        if (player != null) player.release();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // not binding
    }

    public void pauseMusic() {
        if (player != null && player.isPlaying()) {
            player.pause();
        }
    }

    public void resumeMusic() {
        if (player != null && !player.isPlaying()) {
            player.start();
        }
    }


}


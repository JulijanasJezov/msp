package com.jj.mysimpleplayer;

import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;

import com.jj.mysimpleplayer.constants.Constants;

import java.util.ArrayList;

public class PlaybackService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    private final IBinder playbackBinder = new PlaybackBinder();
    private PlaybackServiceCallbacks playbackServiceCallbacks;

    private MediaPlayer player;
    private ArrayList<Song> songLibrary;
    private int songPosition;
    private boolean isPlayerStarted = false;
    private boolean autoPlay = true;
    public PlayerNotification playerNotification;


    @Override
    public IBinder onBind(Intent intent) {
        return playbackBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        playerNotification.releaseSession();
        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        songPosition = 0;
        player = new MediaPlayer();
        playerNotification = new PlayerNotification();

        setupMusicPlayer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        playerNotification.initSession(this);
        playerNotification.handleIntent(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    public void setupMusicPlayer () {
        // Music player settings
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);

        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
    }

    public void setSongLibrary(ArrayList<Song> songs) {
        songLibrary = songs;
    }

    public void setCurrentSong(int songPos) {
        songPosition = songPos;
    }

    public int getCurrentSong() {
        return songPosition;
    }

    public void playSong() {
        player.reset();

        int currentSongId = songLibrary.get(songPosition).getId();

        Uri songUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currentSongId);

        try {
            player.setDataSource(getApplicationContext(), songUri);
        }
        catch (Exception ex) {
            Log.e("Playback Service", "Error", ex);
        }

        player.prepareAsync();
    }

    public void pauseSong(){
        player.pause();
    }

    public void unpauseSong() {
        if (!isPlayerStarted) {
            playSong();
        } else {
            player.seekTo(getCurrentPosition());
            player.start();
        }

    }

    public void seekTo(int position) {
        player.seekTo(position);
    }

    public boolean nextSong() {
        songPosition++;

        if(songPosition >= songLibrary.size()) {
            songPosition--;
            seekTo(getDuration());
            return false;
        }
        
        isPlayerStarted = false;
        playSong();

        return true;
    }

    public boolean prevSong() {
        songPosition--;
        isPlayerStarted = false;

        if(songPosition < 0) {
            songPosition++;
            playSong();
            return false;
        }

        playSong();

        return true;
    }

    public void setAutoPlay(boolean ap) {
        autoPlay = ap;
    }

    public boolean isPlaying() {
        return player.isPlaying();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (autoPlay) {
            if (playbackServiceCallbacks != null) {
                playbackServiceCallbacks.nextSong();
            } else {
                nextSong();
            }
        } else {
            autoPlay = true;
        }
    }

    public int getCurrentPosition(){
        return player.getCurrentPosition();
    }

    public int getDuration() {
        return player.isPlaying() ? player.getDuration() : Constants.UNSET_MAX_DURATION;
    }

    public boolean isPlayerStarted() {
        return isPlayerStarted;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        isPlayerStarted = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        player.reset();
        player.release();
    }

    public class PlaybackBinder extends Binder {
        PlaybackService getService() {
            return PlaybackService.this;
        }
    }

    public void setCallbacks(PlaybackServiceCallbacks callbacks) {
        playbackServiceCallbacks = callbacks;
    }

    public void removeCallbacks() {
        playbackServiceCallbacks = null;
    }
}

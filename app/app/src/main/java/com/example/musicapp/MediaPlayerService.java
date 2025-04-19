package com.example.musicapp;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.musicapp.activities.PlaySongActivity;
import com.example.musicapp.models.Song;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class MediaPlayerService extends Service implements
        MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener,
        AudioManager.OnAudioFocusChangeListener {

    private final IBinder binder = new LocalBinder();
    private MediaPlayer mediaPlayer;
    private int resumePosition;
    private int currentPosition;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private ArrayList<Song> songList;
    private int songIndex = -1;
    private Song currentSong;
    private boolean isRepeatEnabled = false;
    private boolean isShuffleEnabled = false;
    private final Handler handler = new Handler();
    private final Random random = new Random();
    private static final String TAG = "MediaPlayerService";

    public static final String ACTION_SEEK_TO = "com.example.appmusic.ACTION_SEEK_TO";
    public static final String SONG_COMPLETED = "com.example.appmusic.SONG_COMPLETED";
    public static final String ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE";
    public static final String ACTION_PREVIOUS = "ACTION_PREVIOUS";
    public static final String ACTION_NEXT = "ACTION_NEXT";
    public static final String ACTION_STOP = "ACTION_STOP";
    public static final String ACTION_TOGGLE_REPEAT = "ACTION_TOGGLE_REPEAT";
    public static final String ACTION_TOGGLE_SHUFFLE = "ACTION_TOGGLE_SHUFFLE";
    public static final String UPDATE_SEEKBAR = "UPDATE_SEEKBAR";
    public static final String ERROR_ACTION = "com.example.appmusic.ERROR";
    public static final String REPEAT_STATUS = "com.example.appmusic.REPEAT_STATUS";
    public static final String SHUFFLE_STATUS = "com.example.appmusic.SHUFFLE_STATUS";

    @Override
    public void onCreate() {
        super.onCreate();
        registerControlReceiver();
        registerBecomingNoisyReceiver();
        registerPlayNewSong();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        StorageSong storage = new StorageSong(getApplicationContext());
        songList = storage.loadSongArrayList();
        songIndex = storage.loadSongIndex();

        if (songList == null || songList.isEmpty() || songIndex < 0 || songIndex >= songList.size()) {
            Log.e(TAG, "Invalid song data: songList=" + (songList == null ? "null" : songList.size()) + ", songIndex=" + songIndex);
            broadcastError("No songs available");
            stopSelf();
            return START_NOT_STICKY;
        }

        currentSong = songList.get(songIndex);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (!requestAudioFocus()) {
            Log.e(TAG, "Failed to request audio focus");
            broadcastError("Cannot play audio at this time");
            stopSelf();
            return START_NOT_STICKY;
        }

        if (mediaPlayer == null) {
            initMediaPlayer();
        }

        // Gửi trạng thái lặp lại ban đầu
        Intent repeatIntent = new Intent(REPEAT_STATUS);
        repeatIntent.putExtra("isRepeatEnabled", isRepeatEnabled);
        LocalBroadcastManager.getInstance(this).sendBroadcast(repeatIntent);
        Log.d(TAG, "Initial repeat status sent: " + isRepeatEnabled);

        Intent shuffleIntent = new Intent(SHUFFLE_STATUS);
        shuffleIntent.putExtra("isShuffleEnabled", isShuffleEnabled);
        LocalBroadcastManager.getInstance(this).sendBroadcast(shuffleIntent);
        Log.d(TAG, "Initial shuffle status sent: " + isShuffleEnabled);

        handler.post(updateSeekbarRunnable);
        return START_STICKY;
    }

    private void initMediaPlayer() {
        if (currentSong == null || currentSong.getSongFileUrl() == null) {
            Log.e(TAG, "Invalid song or song URL");
            broadcastError("Invalid song data");
            stopSelf();
            return;
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);

        mediaPlayer.reset();
        try {
            mediaPlayer.setDataSource(currentSong.getSongFileUrl());
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e(TAG, "Error setting data source: " + e.getMessage());
            broadcastError("Failed to load song");
            stopSelf();
        }
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }

    private void playMedia() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            handler.post(updateSeekbarRunnable);
            sendMiniPlayerBroadcast();
        }
    }

    private void pauseMedia() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            resumePosition = mediaPlayer.getCurrentPosition();
        }
    }

    private void playPauseMedia() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            pauseMedia();
        } else {
            if (resumePosition > 0) {
                mediaPlayer.seekTo(resumePosition);
            }
            playMedia();
        }
    }

    private void stopMedia() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }

    private void nextSong() {
        if (songList == null || songList.isEmpty()) {
            Log.e(TAG, "Song list is empty or null");
            broadcastError("No songs available");
            stopSelf();
            return;
        }
        if (isShuffleEnabled) {
            // Chọn bài ngẫu nhiên, tránh bài hiện tại
            int newIndex;
            do {
                newIndex = random.nextInt(songList.size());
            } while (newIndex == songIndex && songList.size() > 1);
            songIndex = newIndex;
        } else {
            // Chuyển bài tuần tự
            songIndex = (songIndex >= songList.size() - 1) ? 0 : songIndex + 1;
        }
        currentSong = songList.get(songIndex);
        new StorageSong(getApplicationContext()).storeSongIndex(songIndex);
        stopMedia();
        if (mediaPlayer != null) {
            mediaPlayer.reset();
        }
        initMediaPlayer();
    }

    private void previousSong() {
        if (songList == null || songList.isEmpty()) {
            Log.e(TAG, "Song list is empty or null");
            broadcastError("No songs available");
            stopSelf();
            return;
        }
        if (isShuffleEnabled) {
            // Chọn bài ngẫu nhiên, tránh bài hiện tại
            int newIndex;
            do {
                newIndex = random.nextInt(songList.size());
            } while (newIndex == songIndex && songList.size() > 1);
            songIndex = newIndex;
        } else {
            // Chuyển bài tuần tự ngược
            songIndex = (songIndex <= 0) ? songList.size() - 1 : songIndex - 1;
        }
        currentSong = songList.get(songIndex);
        new StorageSong(getApplicationContext()).storeSongIndex(songIndex);
        stopMedia();
        if (mediaPlayer != null) {
            mediaPlayer.reset();
        }
        initMediaPlayer();
    }

    private void toggleRepeat() {
        isRepeatEnabled = !isRepeatEnabled;
        Log.d(TAG, "Repeat mode toggled: " + (isRepeatEnabled ? "Enabled" : "Disabled"));
        Intent intent = new Intent(REPEAT_STATUS);
        intent.putExtra("isRepeatEnabled", isRepeatEnabled);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void toggleShuffle() {
        isShuffleEnabled = !isShuffleEnabled;
        Log.d(TAG, "Shuffle mode toggled: " + (isShuffleEnabled ? "Enabled" : "Disabled"));
        Intent intent = new Intent(SHUFFLE_STATUS);
        intent.putExtra("isShuffleEnabled", isShuffleEnabled);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private final Runnable updateSeekbarRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                currentPosition = mediaPlayer.getCurrentPosition();
                sendSeekbarUpdate();
            }
            handler.postDelayed(this, 1000);
        }
    };

    private void sendSeekbarUpdate() {
        Intent intent = new Intent(UPDATE_SEEKBAR);
        intent.putExtra("currentPosition", currentPosition);
        sendBroadcast(intent);
    }

    private void sendMiniPlayerBroadcast() {
        Intent intent = new Intent(PlaySongActivity.MINI_PLAYER);
        intent.putExtra("song", currentSong);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.d(TAG, "MiniPlayer broadcast sent for song: " + (currentSong != null ? currentSong.getName() : "null"));
    }

    private void broadcastError(String message) {
        Intent intent = new Intent(ERROR_ACTION);
        intent.putExtra("errorMessage", message);
        sendBroadcast(intent);
    }

    private final BroadcastReceiver controlReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            switch (action) {
                case ACTION_NEXT:
                    nextSong();
                    break;
                case ACTION_PLAY_PAUSE:
                    playPauseMedia();
                    break;
                case ACTION_PREVIOUS:
                    previousSong();
                    break;
                case ACTION_SEEK_TO:
                    int seekPosition = intent.getIntExtra("seekPosition", 0);
                    seekTo(seekPosition);
                    break;
                case ACTION_TOGGLE_REPEAT:
                    toggleRepeat();
                    break;
                case ACTION_TOGGLE_SHUFFLE:
                    toggleShuffle();
                    break;
                case PlaySongActivity.PLAY_NEW_SONG_ACTION:
                    songIndex = new StorageSong(context).loadSongIndex();
                    if (songIndex >= 0 && songIndex < songList.size()) {
                        currentSong = songList.get(songIndex);
                        stopMedia();
                        if (mediaPlayer != null) {
                            mediaPlayer.reset();
                        }
                        initMediaPlayer();
                    } else {
                        broadcastError("Invalid song index");
                        stopSelf();
                    }
                    break;
            }
        }
    };

    private void registerControlReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_NEXT);
        filter.addAction(ACTION_PLAY_PAUSE);
        filter.addAction(ACTION_PREVIOUS);
        filter.addAction(ACTION_SEEK_TO);
        filter.addAction(ACTION_TOGGLE_REPEAT);
        filter.addAction(ACTION_TOGGLE_SHUFFLE);
        filter.addAction(PlaySongActivity.PLAY_NEW_SONG_ACTION);
        registerReceiver(controlReceiver, filter, Context.RECEIVER_EXPORTED);
    }

    private final BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            pauseMedia();
        }
    };

    private void registerBecomingNoisyReceiver() {
        IntentFilter filter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, filter);
    }

    private final BroadcastReceiver playNewSongReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            songIndex = new StorageSong(context).loadSongIndex();
            if (songIndex >= 0 && songIndex < songList.size()) {
                currentSong = songList.get(songIndex);
                stopMedia();
                if (mediaPlayer != null) {
                    mediaPlayer.reset();
                }
                initMediaPlayer();
            } else {
                broadcastError("Invalid song index");
                stopSelf();
            }
        }
    };

    private void registerPlayNewSong() {
        IntentFilter filter = new IntentFilter(PlaySongActivity.PLAY_NEW_SONG_ACTION);
        registerReceiver(playNewSongReceiver, filter, Context.RECEIVER_EXPORTED);
    }

    @Override
    public void onAudioFocusChange(int focusState) {
        switch (focusState) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if (mediaPlayer == null) {
                    initMediaPlayer();
                } else if (!mediaPlayer.isPlaying()) {
                    playMedia();
                }
                mediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                if (mediaPlayer != null) {
                    stopMedia();
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                pauseMedia();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.setVolume(0.1f, 0.1f);
                }
                break;
        }
    }

    private boolean requestAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(this)
                .build();
        return audioManager.requestAudioFocus(audioFocusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private boolean removeAudioFocus() {
        return audioManager != null && audioManager.abandonAudioFocusRequest(audioFocusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "Song completed, repeat mode: " + isRepeatEnabled);
        Intent intent = new Intent(SONG_COMPLETED);
        sendBroadcast(intent);
        if (isRepeatEnabled) {
            // Phát lại bài hát hiện tại
            stopMedia();
            if (mediaPlayer != null) {
                mediaPlayer.reset();
            }
            initMediaPlayer();
        } else {
            nextSong();
        }
        sendMiniPlayerBroadcast();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        playMedia();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "MediaPlayer error: what=" + what + ", extra=" + extra);
        broadcastError("Media playback error: " + what);
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        Log.d(TAG, "MediaPlayer info: what=" + what + ", extra=" + extra);
        return false;
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            stopMedia();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (audioManager != null) {
            removeAudioFocus();
        }
        try {
            unregisterReceiver(controlReceiver);
            unregisterReceiver(becomingNoisyReceiver);
            unregisterReceiver(playNewSongReceiver);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Receiver not registered: " + e.getMessage());
        }
        handler.removeCallbacks(updateSeekbarRunnable);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public Song getCurrentSong() {
        return currentSong;
    }

    public boolean isShuffleEnabled() {
        return isShuffleEnabled;
    }

    public boolean isRepeatEnabled() {
        return isRepeatEnabled;
    }

    public class LocalBinder extends Binder {
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }
}
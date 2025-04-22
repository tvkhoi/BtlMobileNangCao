package com.example.musicapp;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.example.musicapp.activities.PlaySongActivity;
import com.example.musicapp.models.Song;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class MediaPlayerService extends Service implements
        MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, AudioManager.OnAudioFocusChangeListener {

    private final IBinder binder = new LocalBinder();
    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private ArrayList<Song> songList;
    private int songIndex = -1;
    private Song currentSong;
    private boolean isRepeatEnabled = false;
    private boolean isShuffleEnabled = false;
    private boolean isPreparing = false;
    private int pendingSeekPosition = -1;
    private final Handler handler = new Handler();
    private final Random random = new Random();
    private static final String TAG = "MediaPlayerService";

    public static final String ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE";
    public static final String ACTION_NEXT = "ACTION_NEXT";
    public static final String ACTION_PREVIOUS = "ACTION_PREVIOUS";
    public static final String ACTION_SEEK_TO = "ACTION_SEEK_TO";
    public static final String ACTION_TOGGLE_REPEAT = "ACTION_TOGGLE_REPEAT";
    public static final String ACTION_TOGGLE_SHUFFLE = "ACTION_TOGGLE_SHUFFLE";
    public static final String UPDATE_SEEKBAR = "UPDATE_SEEKBAR";
    public static final String SONG_COMPLETED = "SONG_COMPLETED";
    public static final String ERROR_ACTION = "ERROR_ACTION";
    public static final String REPEAT_STATUS = "REPEAT_STATUS";
    public static final String SHUFFLE_STATUS = "SHUFFLE_STATUS";
    public static final String PLAYBACK_STATE_CHANGED = "PLAYBACK_STATE_CHANGED";

    @Override
    public void onCreate() {
        super.onCreate();
        registerReceivers();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        StorageSong storage = StorageSong.getInstance();
        songList = storage.loadSongArrayList();
        songIndex = storage.loadSongIndex();

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
//                ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
//                        != PackageManager.PERMISSION_GRANTED) {
//            broadcastError("Notification permission required for playback");
//            stopSelf();
//            return START_NOT_STICKY;
//        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "music_channel")
                .setContentTitle("Music Player")
                .setContentText("Waiting for song")
                .setSmallIcon(R.drawable.ic_apple_music_icon);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    "music_channel", "Music Player", android.app.NotificationManager.IMPORTANCE_LOW);
            android.app.NotificationManager manager = getSystemService(android.app.NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
        startForeground(1, builder.build());

        if (!requestAudioFocus()) {
            broadcastError("Cannot request audio focus");
            return START_STICKY;
        }

        if (songList != null && !songList.isEmpty() && songIndex >= 0 && songIndex < songList.size()) {
            currentSong = songList.get(songIndex);
            initMediaPlayer();
        } else {
            broadcastError("Invalid song list or index");
        }

        broadcastStatus();
        handler.post(updateSeekBarRunnable);
        return START_STICKY;
    }

    private void initMediaPlayer() {
        if (currentSong == null || currentSong.getSongFileUrl() == null) {
            broadcastError("Invalid song data");
            return;
        }
        if (currentSong.getSongFileUrl().startsWith("http") && !isNetworkAvailable()) {
            broadcastError("No network connection");
            return;
        }
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.reset();
                mediaPlayer.release();
            } catch (Exception e) {
                Log.e(TAG, "Error resetting MediaPlayer: " + e.getMessage());
            }
            mediaPlayer = null;
        }
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
        try {
            mediaPlayer.setDataSource(currentSong.getSongFileUrl());
            isPreparing = true;
            mediaPlayer.prepareAsync();
            Log.d(TAG, "Preparing MediaPlayer for song: " + currentSong.getName());
        } catch (IOException e) {
            Log.e(TAG, "Failed to load song: " + e.getMessage());
            broadcastError("Failed to load song: " + e.getMessage());
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

    private void playMedia() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            updateNotification();
            broadcastSongUpdate();
            Intent stateIntent = new Intent(PLAYBACK_STATE_CHANGED);
            stateIntent.putExtra("isPlaying", true);
            LocalBroadcastManager.getInstance(this).sendBroadcast(stateIntent);
            Log.d(TAG, "Playing song: " + currentSong.getName());
        }
    }

    private void pauseMedia() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            updateNotification();
            Intent stateIntent = new Intent(PLAYBACK_STATE_CHANGED);
            stateIntent.putExtra("isPlaying", false);
            LocalBroadcastManager.getInstance(this).sendBroadcast(stateIntent);
            Log.d(TAG, "Paused song: " + currentSong.getName());
        }
    }

    private void nextSong() {
        if (songList == null || songList.isEmpty()) {
            broadcastError("No songs available");
            return;
        }
        if (songIndex < 0 || songIndex >= songList.size()) {
            songIndex = 0;
        }
        if (isShuffleEnabled && !isRepeatEnabled) {
            int newIndex;
            do {
                newIndex = random.nextInt(songList.size());
            } while (newIndex == songIndex && songList.size() > 1);
            songIndex = newIndex;
        } else {
            songIndex = (songIndex + 1) % songList.size();
        }
        currentSong = songList.get(songIndex);
        StorageSong.getInstance().storeSongIndex(songIndex);
        Log.d(TAG, "Next song selected: " + currentSong.getName() + ", index: " + songIndex);
        initMediaPlayer();
        broadcastSongUpdate();
    }

    private void previousSong() {
        if (songList == null || songList.isEmpty()) {
            broadcastError("No songs available");
            return;
        }
        if (songIndex < 0 || songIndex >= songList.size()) {
            songIndex = 0;
        }
        if (isShuffleEnabled && !isRepeatEnabled) {
            int newIndex;
            do {
                newIndex = random.nextInt(songList.size());
            } while (newIndex == songIndex && songList.size() > 1);
            songIndex = newIndex;
        } else {
            songIndex = (songIndex - 1 < 0) ? songList.size() - 1 : songIndex - 1;
        }
        currentSong = songList.get(songIndex);
        StorageSong.getInstance().storeSongIndex(songIndex);
        Log.d(TAG, "Previous song selected: " + currentSong.getName() + ", index: " + songIndex);
        initMediaPlayer();
        broadcastSongUpdate();
    }

    private void toggleRepeat() {
        isRepeatEnabled = !isRepeatEnabled;
        Intent intent = new Intent(REPEAT_STATUS);
        intent.putExtra("isRepeatEnabled", isRepeatEnabled);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.d(TAG, "Repeat toggled: " + isRepeatEnabled);
    }

    private void toggleShuffle() {
        isShuffleEnabled = !isShuffleEnabled;
        Intent intent = new Intent(SHUFFLE_STATUS);
        intent.putExtra("isShuffleEnabled", isShuffleEnabled);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.d(TAG, "Shuffle toggled: " + isShuffleEnabled);
    }

    private void updateNotification() {
        String title = currentSong != null ? currentSong.getName() : "Unknown";
        String text = currentSong != null ? currentSong.getArtist() : "Unknown";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "music_channel")
                .setContentTitle("Playing: " + title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_apple_music_icon);
        android.app.NotificationManager manager = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(1, builder.build());
    }

    private final Runnable updateSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                int currentPos = mediaPlayer.getCurrentPosition();
                Intent intent = new Intent(UPDATE_SEEKBAR);
                intent.putExtra("currentPosition", currentPos);
                LocalBroadcastManager.getInstance(MediaPlayerService.this).sendBroadcast(intent);
            }
            handler.postDelayed(this, 1000);
        }
    };

    private void broadcastSongUpdate() {
        Intent intent = new Intent(PlaySongActivity.MINI_PLAYER);
        intent.putExtra("song", currentSong);
        intent.putExtra("currentPosition", getCurrentPosition());
        intent.putExtra("isPlaying", isPlaying());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.d(TAG, "Broadcasting song update: " + (currentSong != null ? currentSong.getName() : "null") + ", position: " + getCurrentPosition() + ", isPlaying: " + isPlaying());
    }

    private void broadcastError(String message) {
        Intent intent = new Intent(ERROR_ACTION);
        intent.putExtra("errorMessage", message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.e(TAG, "Error broadcast: " + message);
    }

    private void broadcastStatus() {
        Intent repeatIntent = new Intent(REPEAT_STATUS);
        repeatIntent.putExtra("isRepeatEnabled", isRepeatEnabled);
        LocalBroadcastManager.getInstance(this).sendBroadcast(repeatIntent);

        Intent shuffleIntent = new Intent(SHUFFLE_STATUS);
        shuffleIntent.putExtra("isShuffleEnabled", isShuffleEnabled);
        LocalBroadcastManager.getInstance(this).sendBroadcast(shuffleIntent);
    }

    private void registerReceivers() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PLAY_PAUSE);
        filter.addAction(ACTION_NEXT);
        filter.addAction(ACTION_PREVIOUS);
        filter.addAction(ACTION_SEEK_TO);
        filter.addAction(ACTION_TOGGLE_REPEAT);
        filter.addAction(ACTION_TOGGLE_SHUFFLE);
        filter.addAction(PlaySongActivity.PLAY_NEW_SONG_ACTION);
        filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, filter);
        registerReceiver(broadcastReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            Log.d(TAG, "Received broadcast: " + action);
            switch (action) {
                case PlaySongActivity.PLAY_NEW_SONG_ACTION:
                    StorageSong storage = StorageSong.getInstance();
                    songIndex = storage.loadSongIndex();
                    songList = storage.loadSongArrayList();
                    if (songList == null || songList.isEmpty() || songIndex < 0 || songIndex >= songList.size()) {
                        broadcastError("Invalid song data");
                        Log.e(TAG, "PLAY_NEW_SONG_ACTION: Invalid song data");
                        return;
                    }
                    Song newSong = songList.get(songIndex);
                    String newSongUrl = newSong.getSongFileUrl() != null ? newSong.getSongFileUrl().trim().toLowerCase() : "";
                    String currentSongUrl = currentSong != null && currentSong.getSongFileUrl() != null ? currentSong.getSongFileUrl().trim().toLowerCase() : "";
                    int seekPosition = intent.getIntExtra("seekPosition", -1);

                    if (!newSongUrl.isEmpty() && newSongUrl.equals(currentSongUrl) && !isPreparing && mediaPlayer != null) {
                        Log.d(TAG, "PLAY_NEW_SONG_ACTION: Same song, updating UI only");
                        if (seekPosition >= 0 && Math.abs(mediaPlayer.getCurrentPosition() - seekPosition) > 1000) {
                            mediaPlayer.seekTo(seekPosition);
                            Log.d(TAG, "PLAY_NEW_SONG_ACTION: Seeking to position " + seekPosition);
                        }
                        broadcastSongUpdate();
                        updateNotification();
                    } else {
                        currentSong = newSong;
                        pendingSeekPosition = seekPosition;
                        initMediaPlayer();
                        Log.d(TAG, "PLAY_NEW_SONG_ACTION: Different song, initializing MediaPlayer with seekPosition=" + seekPosition);
                    }
                    break;
                case ACTION_PLAY_PAUSE:
                    if (mediaPlayer == null || isPreparing) {
                        broadcastError("Player not ready");
                        return;
                    }
                    if (mediaPlayer.isPlaying()) {
                        pauseMedia();
                    } else {
                        playMedia();
                    }
                    break;
                case ACTION_NEXT:
                    nextSong();
                    break;
                case ACTION_PREVIOUS:
                    previousSong();
                    break;
                case ACTION_SEEK_TO:
                    int position = intent.getIntExtra("seekPosition", 0);
                    if (mediaPlayer != null && !isPreparing) {
                        mediaPlayer.seekTo(position);
                        Log.d(TAG, "ACTION_SEEK_TO: Seek to position " + position);
                    } else {
                        pendingSeekPosition = position;
                        Log.d(TAG, "ACTION_SEEK_TO: MediaPlayer not ready, storing seekPosition=" + position);
                    }
                    break;
                case ACTION_TOGGLE_REPEAT:
                    toggleRepeat();
                    break;
                case ACTION_TOGGLE_SHUFFLE:
                    toggleShuffle();
                    break;
                case AudioManager.ACTION_AUDIO_BECOMING_NOISY:
                    pauseMedia();
                    break;
            }
        }
    };

    private boolean requestAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(this)
                .build();
        return audioManager.requestAudioFocus(audioFocusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                    playMedia();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                if (mediaPlayer != null) {
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

    @Override
    public void onPrepared(MediaPlayer mp) {
        isPreparing = false;
        if (pendingSeekPosition >= 0 && Math.abs(mediaPlayer.getCurrentPosition() - pendingSeekPosition) > 1000) {
            mediaPlayer.seekTo(pendingSeekPosition);
            Log.d(TAG, "MediaPlayer prepared, seeking to position: " + pendingSeekPosition);
        }
        pendingSeekPosition = -1;
        playMedia();
        Log.d(TAG, "MediaPlayer prepared, starting playback");
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Intent intent = new Intent(SONG_COMPLETED);
        intent.putExtra("song", currentSong);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        if (isRepeatEnabled) {
            initMediaPlayer();
        } else {
            nextSong();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        broadcastError("Playback error: " + what);
        Log.e(TAG, "MediaPlayer error: what=" + what + ", extra=" + extra);
        return false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (audioManager != null) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest);
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        unregisterReceiver(broadcastReceiver);
        handler.removeCallbacks(updateSeekBarRunnable);
        Log.d(TAG, "Service destroyed");
    }

    public boolean isPlaying() {
        try {
            return mediaPlayer != null && mediaPlayer.isPlaying();
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error checking isPlaying: " + e.getMessage());
            return false;
        }
    }

    public Song getCurrentSong() {
        return currentSong;
    }

    public boolean isRepeatEnabled() {
        return isRepeatEnabled;
    }

    public boolean isShuffleEnabled() {
        return isShuffleEnabled;
    }

    public int getCurrentPosition() {
        try {
            return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error getting current position: " + e.getMessage());
            return 0;
        }
    }

    public class LocalBinder extends Binder {
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }
}
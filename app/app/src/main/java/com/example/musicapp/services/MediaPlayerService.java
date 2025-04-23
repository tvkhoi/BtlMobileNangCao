package com.example.musicapp.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.example.musicapp.R;
import com.example.musicapp.activities.MainActivity;
import com.example.musicapp.models.Song;

public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener {
    private static final String TAG = "MediaPlayerService";
    private static final String CHANNEL_ID = "music_channel";
    private static final int NOTIFICATION_ID = 1;

    private final IBinder binder = new LocalBinder();
    private MediaPlayer mediaPlayer;
    private Song currentSong;
    private boolean isPlaying = false;
    private PlaybackListener playbackListener;
    private boolean isPrepared = false;

    public interface PlaybackListener {
        void onPlaybackStateChanged(boolean isPlaying, int position);
        void onError(String errorMessage);
        void onSongPrepared(int duration);
        void onSongCompleted();
    }

    public class LocalBinder extends Binder {
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initializeMediaPlayer();
        createNotificationChannel();
    }

    private void initializeMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        isPrepared = false;
        Log.d(TAG, "MediaPlayer initialized");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setPlaybackListener(PlaybackListener listener) {
        this.playbackListener = listener;
    }

    public void playSong(Song song) {
        if (song == null || TextUtils.isEmpty(song.getSongFileUrl())) {
            notifyError("Invalid song or song URL is null/empty");
            Log.e(TAG, "Invalid song or song URL: song=" + (song == null ? "null" : song.getName()));
            return;
        }
        try {
            if (currentSong != null && currentSong.getSongId().equals(song.getSongId()) && isPrepared && !isPlaying) {
                mediaPlayer.start();
                isPlaying = true;
                updateNotification(song, true);
                notifyPlaybackState(true, mediaPlayer.getCurrentPosition());
                Log.d(TAG, "Resuming existing song: " + song.getName());
                return;
            }
            initializeMediaPlayer();
            currentSong = song;
            mediaPlayer.setDataSource(song.getSongFileUrl());
            mediaPlayer.prepareAsync();
            startForeground(NOTIFICATION_ID, createNotification(song));
            Log.d(TAG, "Preparing song: " + song.getName() + ", URL: " + song.getSongFileUrl());
        } catch (Exception e) {
            Log.e(TAG, "Error preparing song: " + e.getMessage());
            notifyError("Error preparing song: " + e.getMessage());
        }
    }

    public void pauseSong() {
        if (isPlaying && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            updateNotification(currentSong, false);
            notifyPlaybackState(false, mediaPlayer.getCurrentPosition());
            Log.d(TAG, "Song paused");
        }
    }

    public void resumeSong() {
        if (!isPlaying && isPrepared && currentSong != null) {
            mediaPlayer.start();
            isPlaying = true;
            updateNotification(currentSong, true);
            notifyPlaybackState(true, mediaPlayer.getCurrentPosition());
            Log.d(TAG, "Song resumed");
        }
    }

    public void stopSong() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            isPlaying = false;
            isPrepared = false;
            stopForeground(true);
            notifyPlaybackState(false, 0);
            Log.d(TAG, "Song stopped");
        }
    }

    public boolean isPrepared() {return isPrepared;}

    public boolean isPlaying() {
        return isPlaying;
    }

    public Song getCurrentSong() {
        return currentSong;
    }

    public int getCurrentPosition() {
        return mediaPlayer != null && isPrepared ? mediaPlayer.getCurrentPosition() : 0;
    }

    public void seekTo(int position) {
        if (mediaPlayer != null && isPrepared) {
            try {
                mediaPlayer.seekTo(position);
                notifyPlaybackState(isPlaying, position);
                Log.d(TAG, "Seek to position: " + position);
            } catch (IllegalStateException e) {
                notifyError("Cannot seek: " + e.getMessage());
                Log.e(TAG, "Seek error: " + e.getMessage());
            }
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        isPrepared = true;
        mediaPlayer.start();
        isPlaying = true;
        notifyPlaybackState(true, 0);
        if (playbackListener != null) {
            playbackListener.onSongPrepared(mediaPlayer.getDuration());
        }
        Log.d(TAG, "Song prepared and started: " + (currentSong != null ? currentSong.getName() : "unknown"));
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        isPlaying = false;
        isPrepared = false;
        stopForeground(true);
        if (playbackListener != null) {
            playbackListener.onSongCompleted();
        }
        Log.d(TAG, "Song completed: " + (currentSong != null ? currentSong.getName() : "unknown"));
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "MediaPlayer error: what=" + what + ", extra=" + extra);
        isPlaying = false;
        isPrepared = false;
        notifyError("MediaPlayer error: what=" + what + ", extra=" + extra);
        initializeMediaPlayer();
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        Log.d(TAG, "Service destroyed");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Music Playback", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification(Song song) {
        if (song == null) {
            return new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Unknown Song")
                    .setContentText("Unknown Artist")
                    .setSmallIcon(R.drawable.ic_apple_music_icon)
                    .setOngoing(true)
                    .build();
        }
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(song.getName())
                .setContentText(song.getArtist())
                .setSmallIcon(R.drawable.ic_apple_music_icon)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(Song song, boolean isPlaying) {
        Notification notification = createNotification(song);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, notification);
    }

    private void notifyPlaybackState(boolean isPlaying, int position) {
        if (playbackListener != null) {
            playbackListener.onPlaybackStateChanged(isPlaying, position);
        }
    }

    private void notifyError(String errorMessage) {
        if (playbackListener != null) {
            playbackListener.onError(errorMessage);
        }
    }
}
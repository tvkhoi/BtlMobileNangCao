package com.example.musicapp.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.musicapp.R;
import com.example.musicapp.activities.MainActivity;
import com.example.musicapp.models.Song;

public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener, AudioManager.OnAudioFocusChangeListener {
    private static final String TAG = "MediaPlayerService";
    private static final String CHANNEL_ID = "music_channel";
    private static final int NOTIFICATION_ID = 1;

    private static final String ACTION_PLAY_PAUSE = "com.example.musicapp.ACTION_PLAY_PAUSE";
    private static final String ACTION_NEXT = "com.example.musicapp.ACTION_NEXT";
    private static final String ACTION_PREVIOUS = "com.example.musicapp.ACTION_PREVIOUS";
    private static final String ACTION_DISMISS = "com.example.musicapp.ACTION_DISMISS";

    private final IBinder binder = new LocalBinder();
    private MediaPlayer mediaPlayer;
    private Song currentSong;
    private boolean isPlaying = false;
    private PlaybackListener playbackListener;
    private boolean isPrepared = false;
    private BroadcastReceiver notificationReceiver;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;

    public interface PlaybackListener {
        void onPlaybackStateChanged(boolean isPlaying, int position);
        void onError(String errorMessage);
        void onSongPrepared(int duration);
        void onSongCompleted();
        void onPreviousSong();
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

        // Khởi tạo AudioManager
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        notificationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(TAG, "Broadcast received: " + action);
                if (action != null) {
                    switch (action) {
                        case ACTION_PLAY_PAUSE:
                            Log.d(TAG, "Action Play/Pause received, isPlaying=" + isPlaying);
                            if (isPlaying()) {
                                pauseSong();
                            } else {
                                resumeSong();
                            }
                            break;
                        case ACTION_NEXT:
                            Log.d(TAG, "Action Next received");
                            if (playbackListener != null) {
                                playbackListener.onSongCompleted();
                            }
                            break;
                        case ACTION_PREVIOUS:
                            Log.d(TAG, "Action Previous received");
                            if (playbackListener != null) {
                                playbackListener.onPreviousSong();
                            }
                            break;
                        case ACTION_DISMISS:
                            Log.d(TAG, "Notification dismissed, stopping service");
                            stopSong();
                            stopSelf();
                            break;
                    }
                } else {
                    Log.w(TAG, "Received broadcast with null action");
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PLAY_PAUSE);
        filter.addAction(ACTION_NEXT);
        filter.addAction(ACTION_PREVIOUS);
        filter.addAction(ACTION_DISMISS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            registerReceiver(notificationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(notificationReceiver, filter);
        }
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
            // Yêu cầu focus âm thanh trước khi phát
            if (!requestAudioFocus()) {
                notifyError("Cannot play song: Audio focus not granted");
                return;
            }

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
            startForeground(NOTIFICATION_ID, createNotification(song, false));
            notifyPlaybackState(false, 0);
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
            if (requestAudioFocus()) {
                mediaPlayer.start();
                isPlaying = true;
                updateNotification(currentSong, true);
                notifyPlaybackState(true, mediaPlayer.getCurrentPosition());
                Log.d(TAG, "Song resumed");
            } else {
                notifyError("Cannot resume song: Audio focus not granted");
            }
        }
    }

    public void stopSong() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            isPlaying = false;
            isPrepared = false;
            stopForeground(true);
            notifyPlaybackState(false, 0);
            abandonAudioFocus();
            Log.d(TAG, "Song stopped");
        }
    }

    public boolean isPrepared() { return isPrepared; }

    public boolean isPlaying() { return isPlaying; }

    public Song getCurrentSong() { return currentSong; }

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

    public int getDuration() {
        if (mediaPlayer != null && isPrepared) {
            int duration = mediaPlayer.getDuration();
            Log.d(TAG, "getDuration: duration=" + duration);
            return duration;
        }
        Log.w(TAG, "getDuration: MediaPlayer not prepared or null");
        return 0;
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
        updateNotification(currentSong, true);
        Log.d(TAG, "Song prepared and started: " + (currentSong != null ? currentSong.getName() : "unknown"));
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        isPlaying = false;
        isPrepared = false;
        abandonAudioFocus();
        if (playbackListener != null) {
            playbackListener.onSongCompleted();
        }
        Log.d(TAG, "Song completed: " + (currentSong != null ? currentSong.getName() : "unknown"));
        if (playbackListener == null) {
            stopSelf();
            Log.d(TAG, "No playback listener, stopping service");
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "MediaPlayer error: what=" + what + ", extra=" + extra);
        isPlaying = false;
        isPrepared = false;
        notifyError("MediaPlayer error: what=" + what + ", extra=" + extra);
        initializeMediaPlayer();
        updateNotification(currentSong, false);
        abandonAudioFocus();
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        unregisterReceiver(notificationReceiver);
        abandonAudioFocus();
        Log.d(TAG, "Service destroyed");
        stopForeground(true);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Music Playback", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification(Song song, boolean isPlaying) {
        if (song == null) {
            return new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Unknown Song")
                    .setContentText("Unknown Artist")
                    .setSmallIcon(R.drawable.ic_apple_music_icon)
                    .setOngoing(true)
                    .build();
        }

        // Create RemoteViews for custom notification layout
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_custom);

        // Set song title and artist
        remoteViews.setTextViewText(R.id.notification_song_title, song.getName());
        remoteViews.setTextViewText(R.id.notification_song_artist, song.getArtist());

        // Load album art
        Bitmap albumArt = null;
        if (song.getImageUrl() != null && !song.getImageUrl().isEmpty()) {
            Log.d(TAG, "Loading song image from URL: " + song.getImageUrl());
            try {
                FutureTarget<Bitmap> futureTarget = Glide.with(this)
                        .asBitmap()
                        .load(song.getImageUrl())
                        .placeholder(R.drawable.song)
                        .error(R.drawable.song)
                        .submit();
                albumArt = futureTarget.get();
                if (albumArt != null) {
                    remoteViews.setImageViewBitmap(R.id.notification_album_art, albumArt);
                    Log.d(TAG, "Song image loaded successfully");
                } else {
                    Log.w(TAG, "Song image bitmap is null");
                    remoteViews.setImageViewResource(R.id.notification_album_art, R.drawable.song);
                }
                Glide.with(this).clear(futureTarget);
            } catch (Exception e) {
                Log.e(TAG, "Failed to load song image for notification: " + e.getMessage());
                remoteViews.setImageViewResource(R.id.notification_album_art, R.drawable.song);
            }
        } else {
            Log.w(TAG, "Song image URL is null or empty for song: " + song.getName());
            remoteViews.setImageViewResource(R.id.notification_album_art, R.drawable.song);
        }

        // Set play/pause button icon
        remoteViews.setImageViewResource(R.id.notification_play_pause, isPlaying ? R.drawable.pause_icon : R.drawable.play_arrow);

        // Set up PendingIntents for actions with unique request codes
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent playPauseIntent = new Intent(ACTION_PLAY_PAUSE);
        playPauseIntent.setPackage(getPackageName());
        PendingIntent playPausePendingIntent = PendingIntent.getBroadcast(this, 1, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent nextIntent = new Intent(ACTION_NEXT);
        nextIntent.setPackage(getPackageName());
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(this, 2, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent previousIntent = new Intent(ACTION_PREVIOUS);
        previousIntent.setPackage(getPackageName());
        PendingIntent previousPendingIntent = PendingIntent.getBroadcast(this, 3, previousIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent dismissIntent = new Intent(ACTION_DISMISS);
        dismissIntent.setPackage(getPackageName());
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(this, 4, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Set click listeners for buttons
        remoteViews.setOnClickPendingIntent(R.id.notification_prev, previousPendingIntent);
        remoteViews.setOnClickPendingIntent(R.id.notification_play_pause, playPausePendingIntent);
        remoteViews.setOnClickPendingIntent(R.id.notification_next, nextPendingIntent);

        // Build the notification
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_apple_music_icon)
                .setContentIntent(pendingIntent)
                .setDeleteIntent(dismissPendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCustomContentView(remoteViews)
                .setCustomBigContentView(remoteViews)
                .build();

        return notification;
    }

    private void updateNotification(Song song, boolean isPlaying) {
        if (song == null) return;
        this.isPlaying = isPlaying;
        Notification notification = createNotification(song, isPlaying);
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

    // Quản lý focus âm thanh
    private boolean requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(audioAttributes)
                    .setOnAudioFocusChangeListener(this)
                    .build();
            int result = audioManager.requestAudioFocus(audioFocusRequest);
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        } else {
            int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
    }

    private void abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
            }
        } else {
            audioManager.abandonAudioFocus(this);
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
                Log.d(TAG, "Audio focus lost, stopping playback");
                stopSong();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                Log.d(TAG, "Audio focus lost transiently, pausing playback");
                pauseSong();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                Log.d(TAG, "Audio focus lost transiently, can duck");
                // Giảm âm lượng nếu cần
                if (mediaPlayer != null) {
                    mediaPlayer.setVolume(0.2f, 0.2f);
                }
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                Log.d(TAG, "Audio focus gained");
                if (mediaPlayer != null && !isPlaying && isPrepared) {
                    mediaPlayer.setVolume(1.0f, 1.0f);
                    resumeSong();
                }
                break;
        }
    }
}
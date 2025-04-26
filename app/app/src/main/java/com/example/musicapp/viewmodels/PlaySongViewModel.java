package com.example.musicapp.viewmodels;

import android.text.TextUtils;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.musicapp.models.Song;
import com.example.musicapp.services.MediaPlayerService;
import java.util.ArrayList;
import java.util.Collections;

public class PlaySongViewModel extends ViewModel {
    private static final String TAG = "PlaySongViewModel";
    private MutableLiveData<Song> currentSong = new MutableLiveData<>();
    private MutableLiveData<Boolean> isPlaying = new MutableLiveData<>();
    private MutableLiveData<String> error = new MutableLiveData<>();
    private MutableLiveData<Integer> songDuration = new MutableLiveData<>();
    private MutableLiveData<Boolean> resetSeekBar = new MutableLiveData<>();
    private MutableLiveData<ArrayList<Song>> songList = new MutableLiveData<>();
    private MutableLiveData<Integer> songIndex = new MutableLiveData<>();
    private MediaPlayerService mediaPlayerService;
    private boolean isRepeatEnabled = false;
    private boolean isShuffleEnabled = false;
    private ArrayList<Integer> shuffleOrder;
    private long lastCompletionTime = 0;

    public void setMediaPlayerService(MediaPlayerService service) {
        this.mediaPlayerService = service;
        mediaPlayerService.setPlaybackListener(new MediaPlayerService.PlaybackListener() {
            @Override
            public void onPlaybackStateChanged(boolean isPlaying, int position) {
                PlaySongViewModel.this.isPlaying.setValue(isPlaying);
                Log.d(TAG, "Playback state changed: isPlaying=" + isPlaying + ", position=" + position);
            }

            @Override
            public void onError(String errorMessage) {
                error.setValue(errorMessage);
                Log.e(TAG, "Error: " + errorMessage);
            }

            @Override
            public void onSongPrepared(int duration) {
                songDuration.setValue(duration);
                resetSeekBar.setValue(true);
                Log.d(TAG, "Song prepared, duration=" + duration);
            }

            @Override
            public void onSongCompleted() {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastCompletionTime < 500) {
                    Log.w(TAG, "Ignoring rapid onSongCompleted to prevent looping");
                    return;
                }
                lastCompletionTime = currentTime;
                Log.d(TAG, "Song completed, triggering playNextSong");
                playNextSong();
            }
            @Override
            public void onPreviousSong() {
                playPreviousSong();
            }
        });
    }

    public void setSongDuration(int duration) {
        if (duration > 0) {
            songDuration.setValue(duration);
            Log.d(TAG, "setSongDuration: duration=" + duration);
        } else {
            Log.w(TAG, "setSongDuration: Invalid duration=" + duration);
        }
    }

    public void setSongList(ArrayList<Song> songs, int position) {
        if (songs != null && !songs.isEmpty() && position >= 0 && position < songs.size()) {
            songList.setValue(new ArrayList<>(songs)); // Create a new copy to avoid external modifications
            songIndex.setValue(position);
            currentSong.setValue(songs.get(position));
            resetSeekBar.setValue(true);
            Log.d(TAG, "Set song list: size=" + songs.size() + ", position=" + position);
        } else {
            error.setValue("Invalid song list or position");
            Log.e(TAG, "Invalid song list or position: list=" + (songs == null ? "null" : songs.size()) + ", position=" + position);
        }
    }

    public void playSong(Song song) {
        if (song == null || TextUtils.isEmpty(song.getSongFileUrl())) {
            error.setValue("Song is null or has invalid URL");
            Log.e(TAG, "Attempted to play invalid song: " + (song == null ? "null" : song.getName()));
            return;
        }
        if (mediaPlayerService != null) {
            mediaPlayerService.playSong(song);
            currentSong.setValue(song);
            ArrayList<Song> songs = songList.getValue();
            if (songs != null) {
                int index = songs.indexOf(song);
                if (index >= 0) {
                    songIndex.setValue(index);
                }
            }
            resetSeekBar.setValue(true);
            Log.d(TAG, "Playing song: " + song.getName() + ", URL: " + song.getSongFileUrl());
        } else {
            error.setValue("MediaPlayerService not initialized");
            Log.e(TAG, "MediaPlayerService not initialized");
        }
    }

    public void togglePlayPause() {
        if (mediaPlayerService == null) {
            error.setValue("MediaPlayerService not initialized");
            Log.e(TAG, "MediaPlayerService not initialized for togglePlayPause");
            return;
        }
        if (mediaPlayerService.isPlaying()) {
            mediaPlayerService.pauseSong();
            isPlaying.setValue(false);
            Log.d(TAG, "Pausing song");
        } else {
            if (mediaPlayerService.isPrepared() && mediaPlayerService.getCurrentSong() != null) {
                mediaPlayerService.resumeSong();
                isPlaying.setValue(true);
                Log.d(TAG, "Resuming song");
            } else {
                Song current = currentSong.getValue();
                if (current != null) {
                    mediaPlayerService.playSong(current);
                    isPlaying.setValue(true);
                    Log.d(TAG, "Playing song from togglePlayPause: " + current.getName());
                } else {
                    error.setValue("No song selected to play");
                    Log.e(TAG, "No song selected to play");
                }
            }
        }
    }

    public void setIsPlaying(boolean isPlaying) {
        Log.d(TAG, "setIsPlaying called: requested isPlaying=" + isPlaying + ", current isPlaying=" + (mediaPlayerService != null ? mediaPlayerService.isPlaying() : "null") + ", isPrepared=" + (mediaPlayerService != null ? mediaPlayerService.isPrepared() : "null"));
        this.isPlaying.setValue(isPlaying);
        if (mediaPlayerService == null) {
            error.setValue("MediaPlayerService not initialized");
            Log.e(TAG, "MediaPlayerService not initialized for setIsPlaying");
            return;
        }
        Song current = currentSong.getValue();
        if (current == null || TextUtils.isEmpty(current.getSongFileUrl())) {
            error.setValue("No valid song to play");
            Log.e(TAG, "No valid song to play: currentSong=" + (current == null ? "null" : current.getName()));
            return;
        }
        if (isPlaying) {
            if (!mediaPlayerService.isPlaying()) {
                Song serviceCurrentSong = mediaPlayerService.getCurrentSong();
                if (mediaPlayerService.isPrepared() && serviceCurrentSong != null && serviceCurrentSong.getSongId().equals(current.getSongId())) {
                    mediaPlayerService.resumeSong();
                    Log.d(TAG, "Set isPlaying=true, resuming song: " + current.getName());
                } else {
                    mediaPlayerService.playSong(current);
                    Log.d(TAG, "Set isPlaying=true, playing song: " + current.getName());
                }
            } else {
                Log.d(TAG, "Set isPlaying=true, already playing: " + current.getName());
                // Đảm bảo trạng thái đồng bộ
                this.isPlaying.setValue(true);
            }
        } else {
            if (mediaPlayerService.isPlaying()) {
                mediaPlayerService.pauseSong();
                Log.d(TAG, "Set isPlaying=false, pausing song");
            } else {
                Log.d(TAG, "Set isPlaying=false, already paused");
            }
        }
    }

    public void setRepeatEnabled(boolean enabled) {
        isRepeatEnabled = enabled;
        if (enabled && isShuffleEnabled) {
            isShuffleEnabled = false;
            shuffleOrder = null;
            Log.d(TAG, "Repeat enabled, shuffle disabled");
        }
        Log.d(TAG, "Repeat enabled: " + isRepeatEnabled);
    }

    public boolean isRepeatEnabled() {
        return isRepeatEnabled;
    }

    public void setShuffleEnabled(boolean enabled) {
        isShuffleEnabled = enabled;
        if (enabled && isRepeatEnabled) {
            isRepeatEnabled = false;
            Log.d(TAG, "Shuffle enabled, repeat disabled");
        }
        if (enabled) {
            generateShuffleOrder();
        } else {
            shuffleOrder = null;
        }
        Log.d(TAG, "Shuffle enabled: " + isShuffleEnabled);
    }

    public boolean isShuffleEnabled() {
        return isShuffleEnabled;
    }

    private void generateShuffleOrder() {
        ArrayList<Song> songs = songList.getValue();
        if (songs == null || songs.isEmpty()) {
            Log.e(TAG, "Cannot generate shuffle order: song list is null or empty");
            return;
        }
        shuffleOrder = new ArrayList<>();
        for (int i = 0; i < songs.size(); i++) {
            shuffleOrder.add(i);
        }
        Collections.shuffle(shuffleOrder);
        Log.d(TAG, "Generated shuffle order: " + shuffleOrder);
    }

    public void playNextSong() {
        ArrayList<Song> songs = songList.getValue();
        Integer currentIndex = songIndex.getValue();
        if (songs == null || songs.isEmpty() || currentIndex == null || currentIndex < 0 || currentIndex >= songs.size()) {
            error.setValue("No songs available to play next");
            Log.e(TAG, "No songs available to play next: list=" + (songs == null ? "null" : songs.size()) + ", index=" + currentIndex);
            return;
        }
        Log.d(TAG, "Playing next song, repeat=" + isRepeatEnabled + ", shuffle=" + isShuffleEnabled);
        int nextIndex;
        if (isRepeatEnabled) {
            nextIndex = currentIndex;
            Log.d(TAG, "Repeat enabled, replaying current song: " + songs.get(currentIndex).getName());
        } else if (isShuffleEnabled && shuffleOrder != null && !shuffleOrder.isEmpty()) {
            int currentShuffleIndex = shuffleOrder.indexOf(currentIndex);
            if (currentShuffleIndex == -1) {
                generateShuffleOrder();
                currentShuffleIndex = shuffleOrder.indexOf(currentIndex);
            }
            int nextShuffleIndex = (currentShuffleIndex + 1) % shuffleOrder.size();
            nextIndex = shuffleOrder.get(nextShuffleIndex);
            Log.d(TAG, "Shuffle mode, next song index: " + nextIndex);
        } else {
            nextIndex = (currentIndex + 1) % songs.size();
            Log.d(TAG, "Normal mode, next song index: " + nextIndex);
        }
        songIndex.setValue(nextIndex);
        currentSong.setValue(songs.get(nextIndex));
        resetSeekBar.setValue(true);
        playSong(songs.get(nextIndex));
    }

    public void playPreviousSong() {
        ArrayList<Song> songs = songList.getValue();
        Integer currentIndex = songIndex.getValue();
        if (songs == null || songs.isEmpty() || currentIndex == null || currentIndex < 0 || currentIndex >= songs.size()) {
            error.setValue("No songs available to play previous");
            Log.e(TAG, "No songs available to play previous: list=" + (songs == null ? "null" : songs.size()) + ", index=" + currentIndex);
            return;
        }
        int prevIndex;
        if (isRepeatEnabled) {
            prevIndex = currentIndex;
            Log.d(TAG, "Repeat enabled, replaying current song: " + songs.get(currentIndex).getName());
        } else if (isShuffleEnabled && shuffleOrder != null && !shuffleOrder.isEmpty()) {
            int currentShuffleIndex = shuffleOrder.indexOf(currentIndex);
            if (currentShuffleIndex == -1) {
                generateShuffleOrder();
                currentShuffleIndex = shuffleOrder.indexOf(currentIndex);
            }
            int prevShuffleIndex = (currentShuffleIndex - 1 + shuffleOrder.size()) % shuffleOrder.size();
            prevIndex = shuffleOrder.get(prevShuffleIndex);
            Log.d(TAG, "Shuffle mode, previous song index: " + prevIndex);
        } else {
            prevIndex = (currentIndex - 1 + songs.size()) % songs.size();
            Log.d(TAG, "Normal mode, previous song index: " + prevIndex);
        }
        songIndex.setValue(prevIndex);
        currentSong.setValue(songs.get(prevIndex));
        resetSeekBar.setValue(true);
        playSong(songs.get(prevIndex));
    }

    public LiveData<Song> getCurrentSong() {
        return currentSong;
    }

    public LiveData<Boolean> getIsPlaying() {
        return isPlaying;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Integer> getSongDuration() {
        return songDuration;
    }

    public LiveData<Boolean> getResetSeekBar() {
        return resetSeekBar;
    }

    public LiveData<ArrayList<Song>> getSongList() {
        return songList;
    }

    public LiveData<Integer> getSongIndex() {
        return songIndex;
    }
}
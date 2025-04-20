package com.example.musicapp.activities;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicapp.MediaPlayerService;
import com.example.musicapp.R;
import com.example.musicapp.StorageSong;
import com.example.musicapp.adapters.PlaySongAdapter;
import com.example.musicapp.models.Song;

import java.util.ArrayList;

public class PlaySongActivity extends AppCompatActivity {
    private ArrayList<Song> songList;
    private ImageView imgSong, imgNext, imgPrevious, imgPlayPause, imgMinimized, imgRepeat, imgShuffle;
    private RecyclerView recyclerView;
    private PlaySongAdapter adapter;
    private SeekBar seekBar;
    private TextView tvSongName, tvArtist, tvTotalDuration, tvCurrentDuration;
    private MediaPlayerService player;
    private boolean serviceBound = false;
    private boolean isPlaying = true;
    private boolean isRepeatEnabled = false;
    private boolean isShuffleEnabled = false;
    private int songIndex;
    private static final String TAG = "PlaySongActivity";

    public static final String PLAY_NEW_SONG_ACTION = "com.example.appmusic.PLAY_NEW_SONG";
    public static final String SONG_COMPLETED = "com.example.appmusic.SONG_COMPLETED";
    public static final String ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE";
    public static final String ACTION_PREVIOUS = "ACTION_PREVIOUS";
    public static final String ACTION_NEXT = "ACTION_NEXT";
    public static final String ACTION_TOGGLE_REPEAT = "ACTION_TOGGLE_REPEAT";
    public static final String ACTION_TOGGLE_SHUFFLE = "ACTION_TOGGLE_SHUFFLE";
    public static final String MINI_PLAYER = "MINI_PLAYER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_song);
        applyWindowInsets();
        initViews();
        loadSongData();
        setupListeners();
        registerReceivers();
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initViews() {
        imgSong = findViewById(R.id.imgSong_ActiPlaySong);
        seekBar = findViewById(R.id.seekBarSong);
        tvSongName = findViewById(R.id.tvNameSong_ActiPlaySong);
        imgNext = findViewById(R.id.playNextSong_icon);
        imgPrevious = findViewById(R.id.playPreSong_icon);
        imgPlayPause = findViewById(R.id.playSong_icon);
        tvArtist = findViewById(R.id.tvNameArtist_ActiPlaySong);
        recyclerView = findViewById(R.id.recy_ActiPlaySong);
        tvTotalDuration = findViewById(R.id.totalDuration_song);
        tvCurrentDuration = findViewById(R.id.duration_song);
        imgMinimized = findViewById(R.id.imgToMinimizePlayer);
        imgRepeat = findViewById(R.id.repeatSong_icon);
        imgShuffle = findViewById(R.id.RandomSong_icon);
    }

    private void loadSongData() {
        songIndex = getIntent().getIntExtra("position", -1);
        songList = (ArrayList<Song>) getIntent().getSerializableExtra("songList");

        if (songList == null || songIndex < 0 || songIndex >= songList.size()) {
            Log.e(TAG, "Invalid song data");
            finish();
            return;
        }

        Song song = songList.get(songIndex);
        updateSongUI(song);
        playAudio(songIndex);
        setupRecyclerView();
        setTotalDuration();
    }

    private void updateSongUI(Song song) {
        Glide.with(this).load(song.getImageUrl()).into(imgSong);
        tvSongName.setText(song.getName());
        tvArtist.setText(song.getArtist());
        setTotalDuration();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PlaySongAdapter(songList, this);
        recyclerView.setAdapter(adapter);
    }

    private void setTotalDuration() {
        long duration = songList.get(songIndex).getTotalDuration();
        seekBar.setMax((int) duration);
        tvTotalDuration.setText(formatTime((int) duration));
    }

    private void setupListeners() {
        imgMinimized.setOnClickListener(v -> minimizePlayer());
        imgNext.setOnClickListener(v -> playNextSong());
        imgPrevious.setOnClickListener(v -> playPreviousSong());
        imgPlayPause.setOnClickListener(v -> togglePlayPause());
        imgRepeat.setOnClickListener(v -> toggleRepeat());
        imgShuffle.setOnClickListener(v -> toggleShuffle());
        setupSeekBar();
    }

    private void minimizePlayer() {
        if (songList != null && songIndex >= 0 && songIndex < songList.size()) {
            Intent intent = new Intent(MINI_PLAYER);
            intent.putExtra("song", songList.get(songIndex));
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            finish();
        }
    }

    private void playNextSong() {
        sendBroadcast(new Intent(ACTION_NEXT));
        songIndex = (songIndex < songList.size() - 1) ? songIndex + 1 : 0;
        updateSongUI(songList.get(songIndex));
        resetSeekBar();
        isPlaying = true;
        imgPlayPause.setImageResource(R.drawable.pause_icon);
    }

    private void playPreviousSong() {
        sendBroadcast(new Intent(ACTION_PREVIOUS));
        songIndex = (songIndex > 0) ? songIndex - 1 : songList.size() - 1;
        updateSongUI(songList.get(songIndex));
        resetSeekBar();
        isPlaying = true;
        imgPlayPause.setImageResource(R.drawable.pause_icon);
    }

    private void togglePlayPause() {
        isPlaying = !isPlaying;
        imgPlayPause.setImageResource(isPlaying ? R.drawable.pause_icon : R.drawable.play_arrow);
        sendBroadcast(new Intent(ACTION_PLAY_PAUSE));
    }

    private void toggleRepeat() {
        isRepeatEnabled = !isRepeatEnabled;
        imgRepeat.setImageResource(isRepeatEnabled ? R.drawable.icon_repeat_50_on : R.drawable.repeat_icon);
        sendBroadcast(new Intent(ACTION_TOGGLE_REPEAT));
        Log.d(TAG, "Repeat mode toggled: " + (isRepeatEnabled ? "Enabled" : "Disabled"));
        if (isRepeatEnabled && isShuffleEnabled) {
            Toast.makeText(this, "Repeat enabled: Current song will repeat, shuffle will apply when repeat is off", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleShuffle() {
        isShuffleEnabled = !isShuffleEnabled;
        imgShuffle.setImageResource(isShuffleEnabled ? R.drawable.icons_random_24_on : R.drawable.icons_random_24_off);
        sendBroadcast(new Intent(ACTION_TOGGLE_SHUFFLE));
        Log.d(TAG, "Shuffle mode toggled: " + (isShuffleEnabled ? "Enabled" : "Disabled"));
        // Hiển thị toast nếu cả repeat và shuffle bật
        if (isRepeatEnabled && isShuffleEnabled) {
            Toast.makeText(this, "Repeat enabled: Current song will repeat, shuffle will apply when repeat is off", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetSeekBar() {
        seekBar.setProgress(0);
        tvCurrentDuration.setText(formatTime(0));
    }

    private void setupSeekBar() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    tvCurrentDuration.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Intent intent = new Intent(MediaPlayerService.ACTION_SEEK_TO);
                intent.putExtra("seekPosition", seekBar.getProgress());
                sendBroadcast(intent);
            }
        });
    }

    private void registerReceivers() {
        IntentFilter seekBarFilter = new IntentFilter(MediaPlayerService.UPDATE_SEEKBAR);
        registerReceiver(updateSeekBarReceiver, seekBarFilter, Context.RECEIVER_EXPORTED);

        IntentFilter songCompletedFilter = new IntentFilter(SONG_COMPLETED);
        registerReceiver(songCompletedReceiver, songCompletedFilter, Context.RECEIVER_EXPORTED);

        IntentFilter repeatStatusFilter = new IntentFilter(MediaPlayerService.REPEAT_STATUS);
        registerReceiver(repeatStatusReceiver, repeatStatusFilter, Context.RECEIVER_EXPORTED);

        IntentFilter shuffleStatusFilter = new IntentFilter(MediaPlayerService.SHUFFLE_STATUS);
        registerReceiver(shuffleStatusReceiver, shuffleStatusFilter, Context.RECEIVER_EXPORTED);
    }

    private final BroadcastReceiver updateSeekBarReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int currentPos = intent.getIntExtra("currentPosition", -1);
            if (currentPos >= 0) {
                seekBar.setProgress(currentPos);
                tvCurrentDuration.setText(formatTime(currentPos));
            }
        }
    };

    private final BroadcastReceiver songCompletedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            resetSeekBar();
            isPlaying = true;
            imgPlayPause.setImageResource(R.drawable.pause_icon);
            if (!isRepeatEnabled) {
                songIndex = (songIndex < songList.size() - 1) ? songIndex + 1 : 0;
                updateSongUI(songList.get(songIndex));
            }
        }
    };

    private final BroadcastReceiver repeatStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            isRepeatEnabled = intent.getBooleanExtra("isRepeatEnabled", false);
            imgRepeat.setImageResource(isRepeatEnabled ? R.drawable.icon_repeat_50_on : R.drawable.repeat_icon);
            Log.d(TAG, "Received repeat status: " + isRepeatEnabled);
            // Hiển thị toast nếu cả repeat và shuffle bật
            if (isRepeatEnabled && isShuffleEnabled) {
                Toast.makeText(context, "Repeat enabled: Current song will repeat, shuffle will apply when repeat is off", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private final BroadcastReceiver shuffleStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            isShuffleEnabled = intent.getBooleanExtra("isShuffleEnabled", false);
            imgShuffle.setImageResource(isShuffleEnabled ? R.drawable.icons_random_24_on : R.drawable.icons_random_24_off);
            Log.d(TAG, "Received shuffle status: " + isShuffleEnabled);
            // Hiển thị toast nếu cả repeat và shuffle bật
            if (isRepeatEnabled && isShuffleEnabled) {
                Toast.makeText(context, "Repeat enabled: Current song will repeat, shuffle will apply when repeat is off", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private String formatTime(int timeInMillis) {
        if (timeInMillis <= 0) return "00:00";
        int totalSeconds = timeInMillis / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            player = binder.getService();
            serviceBound = true;
            // Đồng bộ trạng thái lặp lại khi kết nối với service
            isRepeatEnabled = player.isRepeatEnabled();
            isShuffleEnabled = player.isShuffleEnabled();
            imgShuffle.setImageResource(isShuffleEnabled ? R.drawable.icons_random_24_on : R.drawable.icons_random_24_off);
            imgRepeat.setImageResource(isRepeatEnabled ? R.drawable.icon_repeat_50_on : R.drawable.repeat_icon);
            Log.d(TAG, "Service connected, repeat mode: " + isRepeatEnabled + ", shuffle mode: " + isShuffleEnabled);
            // Hiển thị toast nếu cả repeat và shuffle bật
            if (isRepeatEnabled && isShuffleEnabled) {
                Toast.makeText(PlaySongActivity.this, "Repeat enabled: Current song will repeat, shuffle will apply when repeat is off", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    private void playAudio(int songIndex) {
        StorageSong storage = new StorageSong(getApplicationContext());
        storage.storeSongArrayList(songList);
        storage.storeSongIndex(songIndex);

        Intent intent = new Intent(this, MediaPlayerService.class);
        Intent broadcastIntent = new Intent(PLAY_NEW_SONG_ACTION);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);

        if (!serviceBound) {
            startService(intent);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("ServiceState", serviceBound);
        outState.putBoolean("RepeatState", isRepeatEnabled);
        outState.putBoolean("ShuffleState", isShuffleEnabled);
        outState.putBoolean("PlayingState", isPlaying);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("ServiceState");
        isPlaying = savedInstanceState.getBoolean("PlayingState");
        isRepeatEnabled = savedInstanceState.getBoolean("RepeatState");
        imgRepeat.setImageResource(isRepeatEnabled ? R.drawable.icon_repeat_50_on : R.drawable.repeat_icon);
        isShuffleEnabled = savedInstanceState.getBoolean("ShuffleState");
        imgShuffle.setImageResource(isShuffleEnabled ? R.drawable.icons_random_24_on : R.drawable.icons_random_24_off);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
        }
        try {
            unregisterReceiver(updateSeekBarReceiver);
            unregisterReceiver(songCompletedReceiver);
            unregisterReceiver(repeatStatusReceiver);
            unregisterReceiver(shuffleStatusReceiver);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Receiver not registered: " + e.getMessage());
        }
    }
}
package com.example.musicapp.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.musicapp.R;
import com.example.musicapp.SongDownloadManager;
import com.example.musicapp.adapters.PlaySongAdapter;
import com.example.musicapp.models.Song;
import com.example.musicapp.services.MediaPlayerService;
import com.example.musicapp.viewmodels.PlaySongViewModel;

import java.util.ArrayList;
import java.util.List;

public class PlaySongActivity extends AppCompatActivity {
    private ImageView imgSong, imgPlayPause, imgNext, imgPrevious, imgRepeat, imgShuffle, imgMinimize, iconDownload_ActiPlaySong;
    private TextView tvSongName, tvArtist, tvCurrentTime, tvTotalTime;
    private SeekBar seekBar;
    private RecyclerView recyclerView;
    private PlaySongAdapter adapter;
    private List<Song> songList;
    private int songIndex;
    private PlaySongViewModel viewModel;
    private MediaPlayerService player;
    private boolean serviceBound = false;
    private static final String TAG = "PlaySongActivity";
    private HandlerThread seekBarThread;
    private Handler seekBarHandler;
    private Runnable updateSeekBar;

    public static final String MINI_PLAYER = "MINI_PLAYER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_song);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        loadSongData();
        setupRecyclerView();
        startAndBindService();
        setupSeekBarThread();
    }

    private void initViews() {
        imgSong = findViewById(R.id.imgSong_ActiPlaySong);
        tvSongName = findViewById(R.id.tvNameSong_ActiPlaySong);
        tvArtist = findViewById(R.id.tvNameArtist_ActiPlaySong);
        seekBar = findViewById(R.id.seekBarSong);
        tvCurrentTime = findViewById(R.id.duration_song);
        tvTotalTime = findViewById(R.id.totalDuration_song);
        imgPlayPause = findViewById(R.id.playSong_icon);
        imgNext = findViewById(R.id.playNextSong_icon);
        imgPrevious = findViewById(R.id.playPreSong_icon);
        imgRepeat = findViewById(R.id.repeatSong_icon);
        imgShuffle = findViewById(R.id.RandomSong_icon);
        imgMinimize = findViewById(R.id.imgToMinimizePlayer);
        recyclerView = findViewById(R.id.recy_ActiPlaySong);
        iconDownload_ActiPlaySong = findViewById(R.id.iconDownload_ActiPlaySong);
    }

    private void loadSongData() {
        songList = (ArrayList<Song>) getIntent().getSerializableExtra("songList");
        songIndex = getIntent().getIntExtra("position", -1);
        int currentPosition = getIntent().getIntExtra("currentPosition", 0);

        if (songList == null || songList.isEmpty() || songIndex < 0 || songIndex >= songList.size()) {
            Toast.makeText(this, "Invalid song data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Song song = songList.get(songIndex);
        updateUI(song);
        if (currentPosition > 0) {
            seekBar.setProgress(currentPosition);
            tvCurrentTime.setText(formatTime(currentPosition));
        }
        Log.d(TAG, "Loaded song: " + song.getName() + ", index: " + songIndex);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(PlaySongViewModel.class);
        viewModel.setSongList((ArrayList<Song>) songList, songIndex);

        // Song and index observers
        viewModel.getCurrentSong().observe(this, song -> {
            if (song != null) {
                updateUI(song);
                Integer index = viewModel.getSongIndex().getValue();
                if (index != null && index >= 0 && index < songList.size()) {
                    songIndex = index;
                    adapter.notifyDataSetChanged();
                }
                seekBar.setProgress(0);
                tvCurrentTime.setText(formatTime(0));
                Log.d(TAG, "Current song changed: " + song.getName());
            }
        });
        viewModel.getSongIndex().observe(this, index -> {
            if (index != null && index >= 0 && index < songList.size()) {
                songIndex = index;
                adapter.notifyDataSetChanged();
                Log.d(TAG, "Song index updated: " + index);
            }
        });

        // Playback state observers
        viewModel.getIsPlaying().observe(this, isPlaying -> {
            if (isPlaying != null) {
                imgPlayPause.setImageResource(isPlaying ? R.drawable.pause_icon : R.drawable.play_arrow);
                if (isPlaying) startSeekBarUpdate(); else stopSeekBarUpdate();
                Log.d(TAG, "Is playing: " + isPlaying);
            }
        });
        viewModel.getSongDuration().observe(this, duration -> {
            if (duration != null && duration > 0) {
                seekBar.setMax(duration);
                tvTotalTime.setText(formatTime(duration));
                Log.d(TAG, "Song duration: " + duration);
            }
        });
        viewModel.getResetSeekBar().observe(this, reset -> {
            if (reset != null && reset) {
                seekBar.setProgress(0);
                tvCurrentTime.setText(formatTime(0));
                Log.d(TAG, "Reset SeekBar triggered");
            }
        });

        // Error and song list observers
        viewModel.getError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error: " + error);
            }
        });
        viewModel.getSongList().observe(this, songs -> {
            if (songs != null) {
                songList = new ArrayList<>(songs);
                if (adapter != null) {
                    adapter.updateSongs((ArrayList<Song>) songList);
                }
                Log.d(TAG, "Song list updated: size=" + songs.size());
            }
        });
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        adapter = new PlaySongAdapter(new ArrayList<>(songList), this, position -> {
            songIndex = position;
            viewModel.setSongList((ArrayList<Song>) songList, songIndex);
            viewModel.playSong(songList.get(songIndex));
            seekBar.setProgress(0);
            tvCurrentTime.setText(formatTime(0));
            Log.d(TAG, "Selected song from recycler: index=" + position);
        });
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        imgMinimize.setOnClickListener(v -> {
            Song currentSong = viewModel.getCurrentSong().getValue();
            ArrayList<Song> currentSongList = viewModel.getSongList().getValue();
            Integer currentSongIndex = viewModel.getSongIndex().getValue();
            int currentPosition = player != null ? player.getCurrentPosition() : seekBar.getProgress();
            boolean isPlaying = viewModel.getIsPlaying().getValue() != null && viewModel.getIsPlaying().getValue();

            Intent intent = new Intent(MINI_PLAYER);
            intent.putExtra("song", currentSong);
            intent.putExtra("songList", currentSongList);
            intent.putExtra("songIndex", currentSongIndex != null ? currentSongIndex : -1);
            intent.putExtra("currentPosition", currentPosition);
            intent.putExtra("isPlaying", isPlaying);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            Log.d(TAG, "Minimize clicked, broadcast sent: song=" + (currentSong != null ? currentSong.getName() : "null") +
                    ", songListSize=" + (currentSongList != null ? currentSongList.size() : "null") +
                    ", songIndex=" + currentSongIndex + ", position=" + currentPosition + ", isPlaying=" + isPlaying);
            finish();
        });
        imgPlayPause.setOnClickListener(v -> viewModel.togglePlayPause());
        imgNext.setOnClickListener(v -> viewModel.playNextSong());
        imgPrevious.setOnClickListener(v -> viewModel.playPreviousSong());
        imgRepeat.setOnClickListener(v -> {
            boolean newRepeatState = !viewModel.isRepeatEnabled();
            viewModel.setRepeatEnabled(newRepeatState);
            imgRepeat.setImageResource(newRepeatState ? R.drawable.icon_repeat_50_on : R.drawable.repeat_icon);
            Toast.makeText(this, newRepeatState ? "Repeat enabled" : "Repeat disabled", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Repeat toggled: " + newRepeatState);
        });
        imgShuffle.setOnClickListener(v -> {
            boolean newShuffleState = !viewModel.isShuffleEnabled();
            viewModel.setShuffleEnabled(newShuffleState);
            imgShuffle.setImageResource(newShuffleState ? R.drawable.icons_random_24_on : R.drawable.icons_random_24_off);
            Toast.makeText(this, newShuffleState ? "Shuffle enabled" : "Shuffle disabled", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Shuffle toggled: " + newShuffleState);
        });
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) tvCurrentTime.setText(formatTime(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopSeekBarUpdate();
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (player != null) player.seekTo(seekBar.getProgress());
                startSeekBarUpdate();
            }
        });
        iconDownload_ActiPlaySong.setOnClickListener(v -> {
            Song song = viewModel.getCurrentSong().getValue();
            if (song == null) {
                Toast.makeText(this, "No song selected", Toast.LENGTH_SHORT).show();
                return;
            }
            SongDownloadManager downloadManager = SongDownloadManager.getInstance(this);
            if (downloadManager.isSongDownloaded(song)) {
                Toast.makeText(this, "Song already downloaded", Toast.LENGTH_SHORT).show();
                return;
            }
            downloadManager.downloadSong(song, new SongDownloadManager.DownloadCallback() {
                @Override
                public void onSuccess(String localPath) {
                    runOnUiThread(() -> Toast.makeText(PlaySongActivity.this, "Download completed: " + song.getName(), Toast.LENGTH_SHORT).show());
                }
                @Override
                public void onError(String errorMessage) {
                    runOnUiThread(() -> Toast.makeText(PlaySongActivity.this, "Download failed: " + errorMessage, Toast.LENGTH_LONG).show());
                }
                @Override
                public void onProgress(int progress) {}
            });
        });
    }

    private void updateUI(Song song) {
        if (song == null) return;
        Glide.with(this).load(song.getImageUrl() != null ? song.getImageUrl() : R.drawable.song).into(imgSong);
        tvSongName.setText(song.getName() != null ? song.getName() : "Unknown");
        tvArtist.setText(song.getArtist() != null ? song.getArtist() : "Unknown");
        if (adapter != null) adapter.notifyItemChanged(songIndex);
        Log.d(TAG, "UI updated: song=" + song.getName());
    }

    private void setupSeekBarThread() {
        seekBarThread = new HandlerThread("SeekBarUpdateThread");
        seekBarThread.start();
        seekBarHandler = new Handler(seekBarThread.getLooper());
    }

    private void startSeekBarUpdate() {
        stopSeekBarUpdate();
        updateSeekBar = () -> {
            if (player != null && player.isPlaying() && viewModel.getCurrentSong().getValue() != null) {
                int currentPosition = player.getCurrentPosition();
                runOnUiThread(() -> {
                    seekBar.setProgress(currentPosition);
                    tvCurrentTime.setText(formatTime(currentPosition));
                });
                Log.d(TAG, "SeekBar updated: position=" + currentPosition);
            }
            seekBarHandler.postDelayed(updateSeekBar, 500);
        };
        seekBarHandler.post(updateSeekBar);
    }

    private void stopSeekBarUpdate() {
        if (updateSeekBar != null) {
            seekBarHandler.removeCallbacks(updateSeekBar);
        }
    }

    private String formatTime(int millis) {
        int seconds = millis / 1000;
        int minutes = seconds / 60;
        seconds %= 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void startAndBindService() {
        Intent intent = new Intent(this, MediaPlayerService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            player = binder.getService();
            serviceBound = true;
            setupViewModel();
            setupListeners();
            viewModel.setMediaPlayerService(player);
//
            // Khôi phục trạng thái phát nhạc
            Song currentSong = viewModel.getCurrentSong().getValue();
            boolean isPlaying = viewModel.getIsPlaying().getValue() != null && viewModel.getIsPlaying().getValue();
            int currentPosition = getIntent().getIntExtra("currentPosition", 0);

            if (currentSong != null) {
                if (player.getCurrentSong() != null && player.getCurrentSong().getSongId().equals(currentSong.getSongId()) && player.isPrepared()) {
                    // Đồng bộ trạng thái từ MediaPlayerService
                    viewModel.setIsPlaying(player.isPlaying());
                    // Cập nhật seekBar và thời gian
                    int duration = player.getDuration();
                    if (duration > 0) {
                        seekBar.setMax(duration);
                        tvTotalTime.setText(formatTime(duration));
                        viewModel.setSongDuration(duration); // Đồng bộ với viewModel
                    }
                    if (currentPosition > 0) {
                        player.seekTo(currentPosition);
                        seekBar.setProgress(currentPosition);
                        tvCurrentTime.setText(formatTime(currentPosition));
                    } else if (player.isPlaying()) {
                        seekBar.setProgress(player.getCurrentPosition());
                        tvCurrentTime.setText(formatTime(player.getCurrentPosition()));
                    }
                    Log.d(TAG, "Restored from MediaPlayerService: song=" + currentSong.getName() +
                            ", isPlaying=" + player.isPlaying() + ", position=" + player.getCurrentPosition() +
                            ", duration=" + duration);
                } else {
                    // Phát bài hát từ viewModel nếu MediaPlayerService không có bài hát phù hợp
                    viewModel.playSong(currentSong);
                    int duration = currentSong.getTotalDuration() > 0 ? currentSong.getTotalDuration() : player.getDuration();
                    if (duration > 0) {
                        seekBar.setMax(duration);
                        tvTotalTime.setText(formatTime(duration));
                        viewModel.setSongDuration(duration); // Đồng bộ với viewModel
                    }
                    if (currentPosition > 0) {
                        player.seekTo(currentPosition);
                        seekBar.setProgress(currentPosition);
                        tvCurrentTime.setText(formatTime(currentPosition));
                    }
                    Log.d(TAG, "Playing from viewModel: song=" + currentSong.getName() +
                            ", position=" + currentPosition + ", duration=" + duration);
                }
            } else {
                // Dữ liệu từ Intent
                boolean intentIsPlaying = getIntent().getBooleanExtra("isPlaying", false);
                if (intentIsPlaying && songList != null && songIndex >= 0) {
                    Song song = songList.get(songIndex);
                    viewModel.playSong(song);
                    int duration = song.getTotalDuration() > 0 ? song.getTotalDuration() : player.getDuration();
                    if (duration > 0) {
                        seekBar.setMax(duration);
                        tvTotalTime.setText(formatTime(duration));
                        viewModel.setSongDuration(duration); // Đồng bộ với viewModel
                    }
                    if (currentPosition > 0) {
                        player.seekTo(currentPosition);
                        seekBar.setProgress(currentPosition);
                        tvCurrentTime.setText(formatTime(currentPosition));
                    }
                    Log.d(TAG, "Playing from Intent: song=" + song.getName() +
                            ", position=" + currentPosition + ", duration=" + duration);
                }
            }
            imgRepeat.setImageResource(viewModel.isRepeatEnabled() ? R.drawable.icon_repeat_50_on : R.drawable.repeat_icon);
            imgShuffle.setImageResource(viewModel.isShuffleEnabled() ? R.drawable.icons_random_24_on : R.drawable.icons_random_24_off);
            Log.d(TAG, "Service connected, repeat=" + viewModel.isRepeatEnabled() + ", shuffle=" + viewModel.isShuffleEnabled());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            player = null;
            Log.d(TAG, "Service disconnected");
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSeekBarUpdate();
        if (seekBarThread != null) {
            seekBarThread.quitSafely();
        }
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }
}
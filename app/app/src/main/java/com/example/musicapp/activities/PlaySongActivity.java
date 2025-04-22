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
import com.example.musicapp.SongDownloadManager;
import com.example.musicapp.StorageSong;
import com.example.musicapp.adapters.PlaySongAdapter;
import com.example.musicapp.models.Song;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class PlaySongActivity extends AppCompatActivity {
    private ImageView imgSong, imgPlayPause, imgNext, imgPrevious, imgRepeat, imgShuffle, imgMinimize, iconDownload_ActiPlaySong, imgFavorite;
    private TextView tvSongName, tvArtist, tvCurrentTime, tvTotalTime;
    private SeekBar seekBar;
    private RecyclerView recyclerView;
    private PlaySongAdapter adapter;
    private ArrayList<Song> songList;
    private int songIndex;
    private MediaPlayerService player;
    private boolean serviceBound = false;
    private boolean isPlaying = false;
    private boolean isRepeatEnabled = false;
    private boolean isShuffleEnabled = false;
    private static final String TAG = "PlaySongActivity";
    private DatabaseReference songsRef;

    public static final String MINI_PLAYER = "MINI_PLAYER";
    public static final String PLAY_NEW_SONG_ACTION = "com.example.appmusic.PLAY_NEW_SONG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_song);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        songsRef = FirebaseDatabase.getInstance().getReference("FrameList/2/listSongs");

        initViews();
        loadSongData();
        setupRecyclerView();
        setupListeners();
        registerReceivers();
        startAndBindService();
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
        imgFavorite = findViewById(R.id.iconFavorite_ActiPlaySong);
    }

    private void loadSongData() {
        songList = (ArrayList<Song>) getIntent().getSerializableExtra("songList");
        songIndex = getIntent().getIntExtra("position", -1);
        int currentPosition = getIntent().getIntExtra("currentPosition", 0);
        isPlaying = getIntent().getBooleanExtra("isPlaying", false);

        if (songList == null || songList.isEmpty() || songIndex < 0 || songIndex >= songList.size()) {
            Toast.makeText(this, "Invalid song data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        StorageSong storage = StorageSong.getInstance();
        storage.storeSongArrayList(songList);
        storage.storeSongIndex(songIndex);

        Song song = songList.get(songIndex);
        updateUI(song);
        seekBar.setMax((int) song.getTotalDuration());
        tvTotalTime.setText(formatTime((int) song.getTotalDuration()));
        if (currentPosition > 0) {
            seekBar.setProgress(currentPosition);
            tvCurrentTime.setText(formatTime(currentPosition));
        }
        imgPlayPause.setImageResource(isPlaying ? R.drawable.pause_icon : R.drawable.play_arrow);
        Log.d(TAG, "Loaded song: " + song.getName() + ", index: " + songIndex);
    }

    private void setupRecyclerView() {
        LinearLayoutManager linear = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(linear);
        adapter = new PlaySongAdapter(songList, this, position -> {
            songIndex = position;
            StorageSong storage = StorageSong.getInstance();
            storage.storeSongIndex(songIndex);
            Intent intent = new Intent(PLAY_NEW_SONG_ACTION);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            updateUI(songList.get(songIndex));
            seekBar.setMax((int) songList.get(songIndex).getTotalDuration());
            tvTotalTime.setText(formatTime((int) songList.get(songIndex).getTotalDuration()));
            isPlaying = true;
            imgPlayPause.setImageResource(R.drawable.pause_icon);
            seekBar.setProgress(0);
            tvCurrentTime.setText(formatTime(0));
            Log.d(TAG, "RecyclerView song selected: " + songList.get(songIndex).getName());
        });
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        imgMinimize.setOnClickListener(v -> {
            Intent intent = new Intent(MINI_PLAYER);
            intent.putExtra("song", songList.get(songIndex));
            intent.putExtra("currentPosition", seekBar.getProgress());
            intent.putExtra("isPlaying", isPlaying);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            finish();
        });
        imgPlayPause.setOnClickListener(v -> {
            if (player != null && player.getCurrentSong() != null) {
                isPlaying = !isPlaying;
                imgPlayPause.setImageResource(isPlaying ? R.drawable.pause_icon : R.drawable.play_arrow);
                Intent intent = new Intent(MediaPlayerService.ACTION_PLAY_PAUSE);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                Log.d(TAG, "Play/Pause clicked, isPlaying toggled to: " + isPlaying);
            } else {
                Toast.makeText(this, "Player not ready", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Play/Pause clicked: Player or current song is null");
            }
        });
        imgNext.setOnClickListener(v -> {
            Intent intent = new Intent(MediaPlayerService.ACTION_NEXT);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        });
        imgPrevious.setOnClickListener(v -> {
            Intent intent = new Intent(MediaPlayerService.ACTION_PREVIOUS);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        });
        imgRepeat.setOnClickListener(v -> {
            Intent intent = new Intent(MediaPlayerService.ACTION_TOGGLE_REPEAT);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        });
        imgShuffle.setOnClickListener(v -> {
            Intent intent = new Intent(MediaPlayerService.ACTION_TOGGLE_SHUFFLE);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        });
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    tvCurrentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Intent intent = new Intent(MediaPlayerService.ACTION_SEEK_TO);
                intent.putExtra("seekPosition", seekBar.getProgress());
                LocalBroadcastManager.getInstance(PlaySongActivity.this).sendBroadcast(intent);
            }
        });
        iconDownload_ActiPlaySong.setOnClickListener(v -> {
            Song song = songList.get(songIndex);
            SongDownloadManager downloadManager = SongDownloadManager.getInstance(PlaySongActivity.this);

            // Check if song is already downloaded
            if (downloadManager.isSongDownloaded(song)) {
                Toast.makeText(PlaySongActivity.this, "Song already downloaded", Toast.LENGTH_SHORT).show();
                return;
            }

            // Start download
            downloadManager.downloadSong(song, new SongDownloadManager.DownloadCallback() {
                @Override
                public void onSuccess(String localPath) {
                    // Update UI or notify user
                    runOnUiThread(() -> {
                        Toast.makeText(PlaySongActivity.this, "Download completed: " + song.getName(), Toast.LENGTH_SHORT).show();
                        iconDownload_ActiPlaySong.setImageResource(R.drawable.icon_download); // Optional: Change icon
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    runOnUiThread(() -> {
                        Toast.makeText(PlaySongActivity.this, "Download failed: " + errorMessage, Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onProgress(int progress) {
                    // Optional: Update progress UI
                    runOnUiThread(() -> {
                        Log.d(TAG, "Download progress: " + progress + "%");
                        // Có thể hiển thị ProgressBar nếu cần
                    });
                }
            });
        });
        imgFavorite.setOnClickListener(v -> {
            Song song = songList.get(songIndex);
            int newLiked = (song.getLiked() == 1) ? 0 : 1; // Đảo ngược giá trị liked: 1 -> 0, 0 -> 1
            song.setLiked(newLiked);

            // Lưu giá trị liked (kiểu int) lên Firebase
            songsRef.child(String.valueOf(songIndex)).child("liked").setValue(newLiked)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Successfully updated liked for song: " + song.getName());
                        updateUI(song); // Cập nhật giao diện sau khi lưu thành công
                        Toast.makeText(PlaySongActivity.this, "You liked this song!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update liked: " + e.getMessage());
                        // Đảo ngược giá trị liked nếu lưu thất bại
                        song.setLiked(newLiked == 1 ? 0 : 1);
                        updateUI(song); // Cập nhật lại giao diện để khớp với giá trị thực tế
                        Toast.makeText(PlaySongActivity.this, "Failed to update favorite status", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void updateUI(Song song) {
        if (song == null) return;

        if (song.getLiked()==1) {
            Glide.with(this).load(R.drawable.favorite_click).into(imgFavorite);
        } else {
            Glide.with(this).load(R.drawable.favorite_icon).into(imgFavorite);
        }
        Glide.with(this).load(song.getImageUrl()).into(imgSong);
        tvSongName.setText(song.getName());
        tvArtist.setText(song.getArtist());

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        Log.d(TAG, "UI updated for song: " + song.getName());
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
            isRepeatEnabled = player.isRepeatEnabled();
            isShuffleEnabled = player.isShuffleEnabled();
            isPlaying = player.isPlaying();
            imgRepeat.setImageResource(isRepeatEnabled ? R.drawable.icon_repeat_50_on : R.drawable.repeat_icon);
            imgShuffle.setImageResource(isShuffleEnabled ? R.drawable.icons_random_24_on : R.drawable.icons_random_24_off);
            imgPlayPause.setImageResource(isPlaying ? R.drawable.pause_icon : R.drawable.play_arrow);

            Song currentSong = player.getCurrentSong();
            if (currentSong != null && songList != null) {
                int intentSongIndex = getIntent().getIntExtra("position", -1);
                int intentCurrentPosition = getIntent().getIntExtra("currentPosition", 0);
                boolean intentIsPlaying = getIntent().getBooleanExtra("isPlaying", false);
                String intentSongUrl = songList.get(intentSongIndex).getSongFileUrl();
                String currentSongUrl = currentSong.getSongFileUrl();

                if (intentSongIndex >= 0 && intentSongUrl != null && currentSongUrl != null &&
                        intentSongUrl.trim().equals(currentSongUrl.trim())) {
                    songIndex = intentSongIndex;
                    isPlaying = intentIsPlaying;
                    updateUI(currentSong);
                    seekBar.setMax((int) currentSong.getTotalDuration());
                    tvTotalTime.setText(formatTime((int) currentSong.getTotalDuration()));
                    if (intentCurrentPosition > 0) {
                        seekBar.setProgress(intentCurrentPosition);
                        tvCurrentTime.setText(formatTime(intentCurrentPosition));
                        if (player.getCurrentPosition() != intentCurrentPosition && !player.isPlaying()) {
                            Intent seekIntent = new Intent(MediaPlayerService.ACTION_SEEK_TO);
                            seekIntent.putExtra("seekPosition", intentCurrentPosition);
                            LocalBroadcastManager.getInstance(PlaySongActivity.this).sendBroadcast(seekIntent);
                        }
                    }
                    imgPlayPause.setImageResource(isPlaying ? R.drawable.pause_icon : R.drawable.play_arrow);
                    Log.d(TAG, "Service connected: Same song, using intent data, position=" + intentCurrentPosition);
                } else {
                    songIndex = songList.indexOf(currentSong);
                    if (songIndex < 0) {
                        songIndex = intentSongIndex;
                        StorageSong.getInstance().storeSongIndex(songIndex);
                        Intent newSongIntent = new Intent(PLAY_NEW_SONG_ACTION);
                        LocalBroadcastManager.getInstance(PlaySongActivity.this).sendBroadcast(newSongIntent);
                    }
                    updateUI(currentSong);
                    seekBar.setMax((int) currentSong.getTotalDuration());
                    tvTotalTime.setText(formatTime((int) currentSong.getTotalDuration()));
                    int currentPosition = player.getCurrentPosition();
                    if (currentPosition > 0) {
                        seekBar.setProgress(currentPosition);
                        tvCurrentTime.setText(formatTime(currentPosition));
                    }
                    Log.d(TAG, "Service connected: Different song, songIndex=" + songIndex);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            player = null;
        }
    };

    private void registerReceivers() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(MediaPlayerService.UPDATE_SEEKBAR);
        filter.addAction(MediaPlayerService.SONG_COMPLETED);
        filter.addAction(MediaPlayerService.ACTION_PLAY_PAUSE);
        filter.addAction(MediaPlayerService.ACTION_NEXT);
        filter.addAction(MediaPlayerService.ACTION_PREVIOUS);
        filter.addAction(MediaPlayerService.REPEAT_STATUS);
        filter.addAction(MediaPlayerService.SHUFFLE_STATUS);
        filter.addAction(MediaPlayerService.ERROR_ACTION);
        filter.addAction(MediaPlayerService.PLAYBACK_STATE_CHANGED);
        filter.addAction(MINI_PLAYER);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, filter);
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            Log.d(TAG, "Received broadcast: " + action);
            switch (action) {
                case MediaPlayerService.UPDATE_SEEKBAR:
                    int currentPos = intent.getIntExtra("currentPosition", -1);
                    if (currentPos >= 0) {
                        seekBar.setProgress(currentPos);
                        tvCurrentTime.setText(formatTime(currentPos));
                    }
                    break;
                case MediaPlayerService.SONG_COMPLETED:
                case MediaPlayerService.ACTION_NEXT:
                case MediaPlayerService.ACTION_PREVIOUS:
                    StorageSong storage = StorageSong.getInstance();
                    songIndex = storage.loadSongIndex();
                    if (songIndex < 0 || songIndex >= songList.size()) {
                        songIndex = 0;
                        storage.storeSongIndex(songIndex);
                        Toast.makeText(context, "Song index reset to 0 due to invalid value", Toast.LENGTH_SHORT).show();
                    }
                    Song song = songList.get(songIndex);
                    updateUI(song);
                    seekBar.setMax((int) song.getTotalDuration());
                    tvTotalTime.setText(formatTime((int) song.getTotalDuration()));
                    seekBar.setProgress(0);
                    tvCurrentTime.setText(formatTime(0));
                    isPlaying = true;
                    imgPlayPause.setImageResource(R.drawable.pause_icon);
                    break;
                case MediaPlayerService.PLAYBACK_STATE_CHANGED:
                    isPlaying = intent.getBooleanExtra("isPlaying", isPlaying);
                    imgPlayPause.setImageResource(isPlaying ? R.drawable.pause_icon : R.drawable.play_arrow);
                    Log.d(TAG, "PLAYBACK_STATE_CHANGED: isPlaying updated to " + isPlaying);
                    break;
                case MediaPlayerService.ACTION_PLAY_PAUSE:
                    if (player == null || player.getCurrentSong() == null) {
                        isPlaying = false;
                        imgPlayPause.setImageResource(R.drawable.play_arrow);
                        Toast.makeText(context, "Player not ready", Toast.LENGTH_SHORT).show();
                        Log.w(TAG, "ACTION_PLAY_PAUSE: Player or current song is null");
                    }
                    break;
                case MediaPlayerService.REPEAT_STATUS:
                    isRepeatEnabled = intent.getBooleanExtra("isRepeatEnabled", false);
                    imgRepeat.setImageResource(isRepeatEnabled ? R.drawable.icon_repeat_50_on : R.drawable.repeat_icon);
                    if (isRepeatEnabled && isShuffleEnabled) {
                        Toast.makeText(context, "Repeat enabled: Shuffle will apply when repeat is off", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case MediaPlayerService.SHUFFLE_STATUS:
                    isShuffleEnabled = intent.getBooleanExtra("isShuffleEnabled", false);
                    imgShuffle.setImageResource(isShuffleEnabled ? R.drawable.icons_random_24_on : R.drawable.icons_random_24_off);
                    if (isRepeatEnabled && isShuffleEnabled) {
                        Toast.makeText(context, "Repeat enabled: Shuffle will apply when repeat is off", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case MediaPlayerService.ERROR_ACTION:
                    String errorMessage = intent.getStringExtra("errorMessage");
                    Toast.makeText(context, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                    isPlaying = false;
                    imgPlayPause.setImageResource(R.drawable.play_arrow);
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
            Intent intent = new Intent(this, MediaPlayerService.class);
            stopService(intent);
            Log.d(TAG, "Stopped MediaPlayerService");
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        Log.d(TAG, "Activity destroyed");
    }
}
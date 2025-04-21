package com.example.musicapp.fragments;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.bumptech.glide.Glide;
import com.example.musicapp.MediaPlayerService;
import com.example.musicapp.R;
import com.example.musicapp.activities.PlaySongActivity;
import com.example.musicapp.models.Song;
import com.example.musicapp.StorageSong;
import java.util.ArrayList;

public class MiniPlayerFragment extends Fragment {
    private ImageView imgSong, imgPlayPause, imgNext, imgPrevious;
    private TextView tvSongName, tvArtist;
    private SeekBar seekBar;
    private View miniPlayerContainer;
    private MediaPlayerService player;
    private boolean serviceBound = false;
    private boolean isPlaying = false;
    private long lastToastTime = 0;
    private static final long TOAST_INTERVAL = 2000;
    private static final String TAG = "MiniPlayerFragment";

    public static MiniPlayerFragment newInstance() {
        return new MiniPlayerFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mini_player, container, false);
        initViews(view);
        setupListeners();
        bindToMediaPlayerService();
        registerReceivers();
        return view;
    }

    private void initViews(View view) {
        miniPlayerContainer = view.findViewById(R.id.miniPlayerContainer);
        imgSong = view.findViewById(R.id.miniImage_frag);
        tvSongName = view.findViewById(R.id.miniNameSong_frag);
        tvArtist = view.findViewById(R.id.miniArtist_frag);
        imgPlayPause = view.findViewById(R.id.miniplaySong_icon_frag);
        imgNext = view.findViewById(R.id.miniplayNextSong_frag);
        imgPrevious = view.findViewById(R.id.minibuttonPreSong_frag);
        seekBar = view.findViewById(R.id.miniPlayerSeekBar_frag);
        miniPlayerContainer.setVisibility(View.GONE);
    }

    private void setupListeners() {
        miniPlayerContainer.setOnClickListener(v -> openPlaySongActivity());
        imgPlayPause.setOnClickListener(v -> {
            Intent intent = new Intent(MediaPlayerService.ACTION_PLAY_PAUSE);
            LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent);
        });
        imgNext.setOnClickListener(v -> {
            Intent intent = new Intent(MediaPlayerService.ACTION_NEXT);
            LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent);
        });
        imgPrevious.setOnClickListener(v -> {
            Intent intent = new Intent(MediaPlayerService.ACTION_PREVIOUS);
            LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent);
        });
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Intent intent = new Intent(MediaPlayerService.ACTION_SEEK_TO);
                intent.putExtra("seekPosition", seekBar.getProgress());
                LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent);
            }
        });
    }

    private void openPlaySongActivity() {
        if (player == null || player.getCurrentSong() == null) {
            showToast("No song selected");
            return;
        }
        StorageSong storage = new StorageSong(requireContext());
        ArrayList<Song> songList = storage.loadSongArrayList();
        int songIndex = storage.loadSongIndex();
        if (songList == null || songList.isEmpty() || songIndex < 0 || songIndex >= songList.size()) {
            showToast("Cannot open player: Invalid song list");
            return;
        }

        Song currentSong = player.getCurrentSong();
        int currentPosition = player.getCurrentPosition();
        boolean isPlaying = player.isPlaying();

        Intent intent = new Intent(requireContext(), PlaySongActivity.class);
        intent.putExtra("songList", songList);
        intent.putExtra("position", songIndex);
        intent.putExtra("currentPosition", currentPosition);
        intent.putExtra("isPlaying", isPlaying);
        startActivity(intent);
        Log.d(TAG, "Opening PlaySongActivity with songIndex=" + songIndex + ", currentPosition=" + currentPosition + ", isPlaying=" + isPlaying);
    }

    private void showToast(String message) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastToastTime >= TOAST_INTERVAL) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            lastToastTime = currentTime;
        }
    }

    private void updateUI(Song song, boolean isPlaying, int currentPosition) {
        if (song == null || getActivity() == null) {
            miniPlayerContainer.setVisibility(View.GONE);
            Log.w(TAG, "Attempted to update UI with null song or activity");
            return;
        }
        this.isPlaying = isPlaying;
        Glide.with(getActivity()).load(song.getImageUrl()).into(imgSong);
        tvSongName.setText(song.getName());
        tvArtist.setText(song.getArtist());
        seekBar.setMax((int) song.getTotalDuration());
        seekBar.setProgress(currentPosition);
        imgPlayPause.setImageResource(isPlaying ? R.drawable.pause_icon : R.drawable.play_arrow);
        miniPlayerContainer.setVisibility(View.VISIBLE);
        Log.d(TAG, "UI updated for song: " + song.getName() + ", isPlaying: " + isPlaying + ", position: " + currentPosition);
    }

    private void bindToMediaPlayerService() {
        Intent intent = new Intent(requireContext(), MediaPlayerService.class);
        requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            player = binder.getService();
            serviceBound = true;
            imgPlayPause.setImageResource(isPlaying ? R.drawable.pause_icon : R.drawable.play_arrow);
            updateUI(player.getCurrentSong(), player.isPlaying(), player.getCurrentPosition());
            Log.d(TAG, "Service connected, current song: " + (player.getCurrentSong() != null ? player.getCurrentSong().getName() : "null"));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            player = null;
            Log.d(TAG, "Service disconnected");
        }
    };

    private void registerReceivers() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(PlaySongActivity.MINI_PLAYER);
        filter.addAction(MediaPlayerService.UPDATE_SEEKBAR);
        filter.addAction(MediaPlayerService.ACTION_PLAY_PAUSE);
        filter.addAction(MediaPlayerService.ACTION_NEXT);
        filter.addAction(MediaPlayerService.ACTION_PREVIOUS);
        filter.addAction(MediaPlayerService.ERROR_ACTION);
        filter.addAction(MediaPlayerService.SONG_COMPLETED);
        filter.addAction(MediaPlayerService.PLAYBACK_STARTED);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(broadcastReceiver, filter);
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            Log.d(TAG, "Received broadcast: " + action);
            switch (action) {
                case PlaySongActivity.MINI_PLAYER:
                case MediaPlayerService.SONG_COMPLETED:
                case MediaPlayerService.ACTION_NEXT:
                case MediaPlayerService.ACTION_PREVIOUS:
                    Song song = (Song) intent.getSerializableExtra("song");
                    boolean isPlayingf = intent.getBooleanExtra("isPlaying", false);
                    int currentPosition = intent.getIntExtra("currentPosition", 0);
                    if (song != null) {
                        updateUI(song, isPlayingf, currentPosition);
                    } else {
                        showToast("No song data available");
                        miniPlayerContainer.setVisibility(View.GONE);
                    }
                    break;
                case MediaPlayerService.UPDATE_SEEKBAR:
                    int currentPos = intent.getIntExtra("currentPosition", -1);
                    if (currentPos >= 0) seekBar.setProgress(currentPos);
                    break;
                case MediaPlayerService.ACTION_PLAY_PAUSE:
                    isPlaying = !isPlaying;
                    imgPlayPause.setImageResource(isPlaying ? R.drawable.pause_icon : R.drawable.play_arrow);
                    break;
                case MediaPlayerService.ERROR_ACTION:
                    String errorMessage = intent.getStringExtra("errorMessage");
                    showToast("Error: " + errorMessage);
                    miniPlayerContainer.setVisibility(View.GONE);
                    break;
                case MediaPlayerService.PLAYBACK_STARTED:
                    isPlaying = true;
                    imgPlayPause.setImageResource(R.drawable.pause_icon);
                    break;
            }
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (serviceBound) {
            requireContext().unbindService(serviceConnection);
        }
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(broadcastReceiver);
        Log.d(TAG, "Fragment view destroyed");
    }
}
package com.example.musicapp.fragments;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.bumptech.glide.Glide;
import com.example.musicapp.R;
import com.example.musicapp.activities.PlaySongActivity;
import com.example.musicapp.models.Song;
import com.example.musicapp.services.MediaPlayerService;
import com.example.musicapp.viewmodels.PlaySongViewModel;
import java.util.ArrayList;

public class MiniPlayerFragment extends Fragment {
    private ImageView imgSong, imgPlayPause, imgNext, imgPrevious;
    private TextView tvSongName, tvArtist;
    private SeekBar seekBar;
    private View miniPlayerContainer;
    private MediaPlayerService player;
    private boolean serviceBound = false;
    private PlaySongViewModel viewModel;
    private Handler seekBarHandler;
    private Runnable seekBarRunnable;
    private static final String TAG = "MiniPlayerFragment";
    private BroadcastReceiver miniPlayerReceiver;

    public static MiniPlayerFragment newInstance() {
        return new MiniPlayerFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Khởi tạo BroadcastReceiver để nhận broadcast từ PlaySongActivity
        miniPlayerReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Song song = (Song) intent.getSerializableExtra("song");
                int currentPosition = intent.getIntExtra("currentPosition", 0);
                boolean isPlaying = intent.getBooleanExtra("isPlaying", false);
                Log.d(TAG, "Received broadcast: song=" + (song != null ? song.getName() : "null") + ", position=" + currentPosition + ", isPlaying=" + isPlaying);

                if (song != null) {
                    // Lấy songList từ viewModel
                    ArrayList<Song> songList = viewModel.getSongList().getValue();
                    if (songList == null) {
                        songList = new ArrayList<>();
                    }
                    // Kiểm tra xem bài hát có trong songList không
                    int songIndex = songList.indexOf(song);
                    if (songIndex < 0) {
                        // Nếu bài hát không có trong danh sách, thêm nó
                        songList.add(song);
                        songIndex = songList.size() - 1;
                    }
                    // Cập nhật viewModel
                    viewModel.setSongList(songList, songIndex);
                    viewModel.setIsPlaying(isPlaying);
                    if (currentPosition > 0) {
                        seekBar.setProgress(currentPosition);
                    }
                    updateUI(song, isPlaying, currentPosition);
                } else {
                    Log.e(TAG, "Received null song in broadcast");
                    miniPlayerContainer.setVisibility(View.GONE);
                }
            }
        };
        // Đăng ký BroadcastReceiver với LocalBroadcastManager
        IntentFilter filter = new IntentFilter(PlaySongActivity.MINI_PLAYER);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(miniPlayerReceiver, filter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mini_player, container, false);
        initViews(view);
        setupViewModel();
        setupListeners();
        bindToMediaPlayerService();
        setupSeekBarUpdate();
        loadInitialSong();
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

    private void loadInitialSong() {
        Song song = viewModel.getCurrentSong().getValue();
        Integer songIndex = viewModel.getSongIndex().getValue();
        ArrayList<Song> songList = viewModel.getSongList().getValue();
        if (song != null && songList != null && songIndex != null && songIndex >= 0 && songIndex < songList.size()) {
            updateUI(song, viewModel.getIsPlaying().getValue() != null && viewModel.getIsPlaying().getValue(), 0);
            Log.d(TAG, "Loaded initial song: " + song.getName() + ", index=" + songIndex);
        } else {
            Log.d(TAG, "No initial song available");
            miniPlayerContainer.setVisibility(View.GONE);
        }
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(requireActivity()).get(PlaySongViewModel.class);
        viewModel.getCurrentSong().observe(getViewLifecycleOwner(), song -> {
            if (song != null) {
                boolean isPlaying = viewModel.getIsPlaying().getValue() != null && viewModel.getIsPlaying().getValue();
                updateUI(song, isPlaying, 0);
                Log.d(TAG, "Current song updated: " + song.getName());
            } else {
                miniPlayerContainer.setVisibility(View.GONE);
                Log.d(TAG, "Current song is null, hiding mini player");
            }
        });
        viewModel.getIsPlaying().observe(getViewLifecycleOwner(), isPlaying -> {
            if (isPlaying != null) {
                imgPlayPause.setImageResource(isPlaying ? R.drawable.pause_icon : R.drawable.play_arrow);
                if (isPlaying) startSeekBarUpdate(); else stopSeekBarUpdate();
                Log.d(TAG, "Is playing updated: " + isPlaying);
            }
        });
        viewModel.getSongDuration().observe(getViewLifecycleOwner(), duration -> {
            if (duration != null && duration > 0) {
                seekBar.setMax(duration);
                Log.d(TAG, "Song duration updated: " + duration);
            }
        });
        viewModel.getResetSeekBar().observe(getViewLifecycleOwner(), reset -> {
            if (reset != null && reset) {
                seekBar.setProgress(0);
                Log.d(TAG, "Reset SeekBar triggered");
            }
        });
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error observed: " + error);
            }
        });
    }

    private void setupListeners() {
        miniPlayerContainer.setOnClickListener(v -> openPlaySongActivity());
        imgPlayPause.setOnClickListener(v -> viewModel.togglePlayPause());
        imgNext.setOnClickListener(v -> viewModel.playNextSong());
        imgPrevious.setOnClickListener(v -> viewModel.playPreviousSong());
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}
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
    }

    private void openPlaySongActivity() {
        ArrayList<Song> songList = viewModel.getSongList().getValue();
        Integer songIndex = viewModel.getSongIndex().getValue();
        if (songList == null || songList.isEmpty() || songIndex == null || songIndex < 0 || songIndex >= songList.size()) {
            Toast.makeText(requireContext(), "Cannot open player: Invalid song list", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Cannot open PlaySongActivity: list=" + (songList == null ? "null" : songList.size()) + ", index=" + songIndex);
            return;
        }
        Intent intent = new Intent(requireContext(), PlaySongActivity.class);
        intent.putExtra("songList", songList);
        intent.putExtra("position", songIndex);
        intent.putExtra("currentPosition", player != null ? player.getCurrentPosition() : seekBar.getProgress());
        intent.putExtra("isPlaying", viewModel.getIsPlaying().getValue() != null && viewModel.getIsPlaying().getValue());
        startActivity(intent);
        Log.d(TAG, "Opening PlaySongActivity: songIndex=" + songIndex);
    }

    private void updateUI(Song song, boolean isPlaying, int currentPosition) {
        if (song == null || getActivity() == null) {
            miniPlayerContainer.setVisibility(View.GONE);
            Log.d(TAG, "Update UI skipped: song or activity is null");
            return;
        }
        Glide.with(getActivity()).load(song.getImageUrl() != null ? song.getImageUrl() : R.drawable.song).into(imgSong);
        tvSongName.setText(song.getName() != null ? song.getName() : "Unknown");
        tvArtist.setText(song.getArtist() != null ? song.getArtist() : "Unknown");
        seekBar.setProgress(currentPosition);
        imgPlayPause.setImageResource(isPlaying ? R.drawable.pause_icon : R.drawable.play_arrow);
        miniPlayerContainer.setVisibility(View.VISIBLE);
        Log.d(TAG, "UI updated: song=" + song.getName() + ", isPlaying=" + isPlaying + ", position=" + currentPosition);
    }

    private void setupSeekBarUpdate() {
        seekBarHandler = new Handler(requireActivity().getMainLooper());
        seekBarRunnable = () -> {
            if (player != null && player.isPlaying() && viewModel.getCurrentSong().getValue() != null) {
                int currentPosition = player.getCurrentPosition();
                seekBar.setProgress(currentPosition);
                Log.d(TAG, "SeekBar updated: position=" + currentPosition);
            }
            seekBarHandler.postDelayed(seekBarRunnable, 500);
        };
    }

    private void startSeekBarUpdate() {
        stopSeekBarUpdate();
        seekBarHandler.post(seekBarRunnable);
    }

    private void stopSeekBarUpdate() {
        if (seekBarHandler != null && seekBarRunnable != null) {
            seekBarHandler.removeCallbacks(seekBarRunnable);
        }
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
            viewModel.setMediaPlayerService(player);
            Song currentSong = player.getCurrentSong();
            if (currentSong != null) {
                updateUI(currentSong, player.isPlaying(), player.getCurrentPosition());
                Log.d(TAG, "Service connected, current song: " + currentSong.getName());
            } else {
                loadInitialSong();
                Log.d(TAG, "Service connected, no current song, loading initial song");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            player = null;
            Log.d(TAG, "Service disconnected");
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopSeekBarUpdate();
        if (serviceBound) {
            requireContext().unbindService(serviceConnection);
            serviceBound = false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (miniPlayerReceiver != null) {
            LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(miniPlayerReceiver);
            miniPlayerReceiver = null;
        }
    }
}
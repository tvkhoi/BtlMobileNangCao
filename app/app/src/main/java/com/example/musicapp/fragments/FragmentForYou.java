package com.example.musicapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.musicapp.R;
import com.example.musicapp.activities.PlaySongActivity;
import com.example.musicapp.adapters.ContentFragForYouAdapter;
import com.example.musicapp.models.Frame;
import com.example.musicapp.models.Song;
import com.example.musicapp.viewmodels.ForYouViewModel;
import java.util.ArrayList;

public class FragmentForYou extends Fragment {
    private RecyclerView recyclerView;
    private ContentFragForYouAdapter adapter;
    private ForYouViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_foryou, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.rc_FragForYou);
        viewModel = new ViewModelProvider(this).get(ForYouViewModel.class);

        adapter = new ContentFragForYouAdapter(new ArrayList<>(), getActivity(), new ContentFragForYouAdapter.OnItemClickListener() {
            @Override
            public void onSongClick(int framePosition, int songPosition) {
                Frame frame = adapter.getFrameList().get(framePosition);
                Intent intent = new Intent(getActivity(), PlaySongActivity.class);
                intent.putExtra("songList", new ArrayList<>(frame.getDisplaySongs()));
                intent.putExtra("position", songPosition);
                intent.putExtra("isPlaying", true);
                startActivity(intent);
            }

            @Override
            public void onArtistClick(int framePosition, int artistPosition) {
                Frame frame = adapter.getFrameList().get(framePosition);
                String artistId = frame.getDisplayArtists().get(artistPosition).getArtistId();
                viewModel.getSongsByArtist(artistId).observe(getViewLifecycleOwner(), songs -> {
                    if (songs == null || songs.isEmpty()) {
                        Toast.makeText(getActivity(), "No songs found for this artist", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Intent intent = new Intent(getActivity(), PlaySongActivity.class);
                    intent.putExtra("songList", new ArrayList<>(songs));
                    intent.putExtra("position", 0);
                    intent.putExtra("isPlaying", true);
                    startActivity(intent);
                });
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter);

        viewModel.getEnrichedFrames().observe(getViewLifecycleOwner(), frames -> {
            if (frames != null) {
                adapter.updateFrames(frames);
            } else {
                Toast.makeText(getActivity(), "Failed to load frames", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
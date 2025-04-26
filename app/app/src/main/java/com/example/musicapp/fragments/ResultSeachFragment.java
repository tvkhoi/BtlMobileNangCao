package com.example.musicapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicapp.R;
import com.example.musicapp.activities.PlaySongActivity;
import com.example.musicapp.adapters.SongAdapter;
import com.example.musicapp.models.Song;
import com.example.musicapp.repository.MusicRepository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ResultSeachFragment extends Fragment {

    private RecyclerView recyclerView;
    private SongAdapter songAdapter;
    private List<Song> searchResults;
    private MusicRepository musicRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_result_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerViewSearchResults);
        searchResults = new ArrayList<>();
        musicRepository = MusicRepository.getInstance();

        // Lấy danh sách bài hát từ Bundle
        Bundle args = getArguments();
        if (args != null) {
            searchResults = (ArrayList<Song>) args.getSerializable("songs");
        }

        // Thiết lập RecyclerView cho kết quả tìm kiếm
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        songAdapter = new SongAdapter(getActivity(), searchResults, song -> {
            // Khi nhấp vào bài hát, lấy danh sách bài hát của nghệ sĩ
            musicRepository.getSongsByArtist(song.getArtistId()).observe(getViewLifecycleOwner(), songs -> {
                Intent intent = new Intent(getActivity(), PlaySongActivity.class);
                intent.putExtra("songList", (Serializable) songs);
                intent.putExtra("position", songs.indexOf(song));
                intent.putExtra("currentPosition", 0);
                int s =songs.indexOf(song);
                intent.putExtra("isPlaying", false);
                startActivity(intent);
            });
        });
        recyclerView.setAdapter(songAdapter);
    }
}
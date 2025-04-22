package com.example.musicapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicapp.R;
import com.example.musicapp.SongDownloadManager;
import com.example.musicapp.activities.PlaySongActivity;
import com.example.musicapp.adapters.AdapterListViewLibraries;
import com.example.musicapp.adapters.AdapterRecyLibraries;
import com.example.musicapp.models.ItemLibraries;
import com.example.musicapp.models.Song;

import java.util.ArrayList;
import java.util.List;

public class FragmentLibraries extends Fragment {
    private RecyclerView recyclerView;
    private ListView listView;
    private AdapterRecyLibraries adapterRecy;
    private AdapterListViewLibraries adapterListView;
    private List<ItemLibraries> libraries;
    private ArrayList<Song> downloadedSongs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_libraries, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.recLibraries);
        listView = view.findViewById(R.id.listView_libraries);

        libraries = new ArrayList<>();
        downloadedSongs = new ArrayList<>();
        adapterRecy = new AdapterRecyLibraries(getActivity(), libraries,this);
        GridLayoutManager manager = new GridLayoutManager(getActivity(), 2);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapterRecy);

        // Initialize ListView adapter
        adapterListView = new AdapterListViewLibraries(getActivity(), downloadedSongs);
        listView.setAdapter(adapterListView);

        initData();

        // Set ListView item click listener
        listView.setOnItemClickListener((parent, view1, position, id) -> {
            if (downloadedSongs.isEmpty()) {
                Toast.makeText(getActivity(), "No downloaded songs available", Toast.LENGTH_SHORT).show();
                return;
            }

            // Start PlaySongActivity with downloaded songs
            Intent intent = new Intent(getActivity(), PlaySongActivity.class);
            intent.putExtra("songList", downloadedSongs);
            intent.putExtra("position", position); // Start with selected song
            intent.putExtra("currentPosition", 0);
            intent.putExtra("isPlaying", false);
            startActivity(intent);
        });
    }

    private void initData() {
        String[] stringNameItem = {"Đã thích", "Download"};
        int[] icons = {R.drawable.favorite_icon, R.drawable.download_for_offline};
        // Get number of downloaded songs
        SongDownloadManager downloadManager = SongDownloadManager.getInstance(getActivity());
        int downloadedCount = downloadManager.getDownloadedSongsCount();
        String[] stringNumSong = {" ", downloadedCount + " bài"};

        for (int i = 0; i < stringNameItem.length; i++) {
            libraries.add(new ItemLibraries(icons[i], stringNameItem[i], stringNumSong[i]));
        }
        adapterRecy.notifyDataSetChanged();
    }

    public void updateDownloadedSongs(ArrayList<Song> songs) {
        downloadedSongs.clear();
        downloadedSongs.addAll(songs);
        adapterListView.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh downloaded songs count and list
        SongDownloadManager downloadManager = SongDownloadManager.getInstance(getActivity());
        int downloadedCount = downloadManager.getDownloadedSongsCount();
        if (!libraries.isEmpty()) {
            libraries.get(1).setNumSong(downloadedCount + " bài"); // Update "Download" item
            adapterRecy.notifyDataSetChanged();
        }

        // Update ListView with downloaded songs
        downloadedSongs.clear();
        downloadedSongs.addAll(downloadManager.getDownloadedSongs());
        adapterListView.notifyDataSetChanged();
    }
}
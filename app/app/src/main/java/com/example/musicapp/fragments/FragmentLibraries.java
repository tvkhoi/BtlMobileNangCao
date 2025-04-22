package com.example.musicapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicapp.R;
import com.example.musicapp.SongDownloadManager;
import com.example.musicapp.adapters.AdapterRecyLibraries;
import com.example.musicapp.models.ItemLibraries;



import java.util.ArrayList;
import java.util.List;


public class FragmentLibraries extends Fragment {
    private RecyclerView recyclerView;
    private ListView listView;
    private AdapterRecyLibraries adapterRecy;
    //private AdapterListViewLibraries adapterListView;
    //private List<Music> musicList;
    private List<ItemLibraries> libraries;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.fragment_libraries,container,false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.recLibraries);
        listView = view.findViewById(R.id.listView_libraries);

        libraries=new ArrayList<>();
        adapterRecy = new AdapterRecyLibraries(getActivity(),libraries);
        GridLayoutManager manager = new GridLayoutManager(getActivity(),2);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapterRecy);
        initData();
    }

    private void initData() {
        String[] stringNameItem = {"Đã thích","Download", "Danh sách","Nghệ sĩ"};
        int[] icons={R.drawable.favorite_icon,R.drawable.download_for_offline,
                R.drawable.queue_music,R.drawable.artist};
        // Get number of downloaded songs
        SongDownloadManager downloadManager = SongDownloadManager.getInstance(getActivity());
        int downloadedCount = downloadManager.getDownloadedSongsCount();
        String[] stringNumSong = {" ",downloadedCount +" bài", " ", " "};

        for (int i = 0; i < stringNameItem.length; i++) {
            libraries.add(new ItemLibraries(icons[i],stringNameItem[i],stringNumSong[i]));
        }
        adapterRecy.notifyDataSetChanged();
    }
}

package com.example.musicapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicapp.R;
import com.example.musicapp.activities.PlaySongActivity;
import com.example.musicapp.adapters.CategoryAdapter;
import com.example.musicapp.adapters.TrendingArtistAdapter;
import com.example.musicapp.models.Song;
import com.example.musicapp.models.Artist;
import com.example.musicapp.models.Frame;
import com.example.musicapp.repository.MusicRepository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FragmentSearch extends Fragment {
    private RecyclerView recyclerView1, recyclerView2;
    private TrendingArtistAdapter adapterArtist;
    private CategoryAdapter adapterCategory;
    private SearchView searchView;
    private List<Artist> artistList;  // Sử dụng Artist thay vì Song
    private List<Frame> categoryList; // Sử dụng Frame thay vì Song
    private List<Song> allSongs;
    private MusicRepository musicRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView1 = view.findViewById(R.id.recyclerViewTrendingArtists);
        recyclerView2 = view.findViewById(R.id.recyclerViewCategories);
        searchView = view.findViewById(R.id.searchView);

        // Khởi tạo danh sách
        artistList = new ArrayList<>();
        categoryList = new ArrayList<>();
        allSongs = new ArrayList<>();
        musicRepository = MusicRepository.getInstance();

        // Thiết lập RecyclerView cho nghệ sĩ
        LinearLayoutManager linear = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        recyclerView1.setLayoutManager(linear);
        adapterArtist = new TrendingArtistAdapter(getActivity(), artistList, artist -> {
            // Khi nhấp vào nghệ sĩ, lấy danh sách bài hát của nghệ sĩ đó
            musicRepository.getSongsByArtist(artist.getArtistId()).observe(getViewLifecycleOwner(), songs -> {
                Intent intent = new Intent(getActivity(), PlaySongActivity.class);
                intent.putExtra("songList", (Serializable) songs);
                intent.putExtra("position", 0); // Mặc định phát bài đầu tiên
                intent.putExtra("currentPosition", 0);
                intent.putExtra("isPlaying", false);
                startActivity(intent);
            });
        });
        recyclerView1.setAdapter(adapterArtist);

        // Thiết lập RecyclerView cho danh mục
        GridLayoutManager grid = new GridLayoutManager(getActivity(), 2);
        recyclerView2.setLayoutManager(grid);
        adapterCategory = new CategoryAdapter(getActivity(), categoryList, (frame, song) -> {
            // Khi nhấp vào bài hát trong frame, lấy danh sách bài hát của nghệ sĩ
            musicRepository.getSongsByArtist(song.getArtistId()).observe(getViewLifecycleOwner(), songs -> {
                Intent intent = new Intent(getActivity(), PlaySongActivity.class);
                intent.putExtra("songList", (Serializable) songs);
                intent.putExtra("position", songs.indexOf(song));
                intent.putExtra("currentPosition", 0);
                intent.putExtra("isPlaying", false);
                startActivity(intent);
            });
        });
        recyclerView2.setAdapter(adapterCategory);

        // Tải dữ liệu từ MusicRepository
        loadData();

        // Xử lý tìm kiếm
        setupSearchView();
    }

    private void loadData() {
        // Lấy danh sách nghệ sĩ
        musicRepository.getArtists().observe(getViewLifecycleOwner(), artists -> {
            artistList.clear();
            artistList.addAll(artists);
            adapterArtist.notifyDataSetChanged();
        });

        // Lấy danh sách danh mục (frames)
        musicRepository.getFrames().observe(getViewLifecycleOwner(), frames -> {
            categoryList.clear();
            categoryList.addAll(frames);
            adapterCategory.notifyDataSetChanged();
        });

        // Lấy tất cả bài hát để tìm kiếm
        musicRepository.getSongs().observe(getViewLifecycleOwner(), songs -> {
            allSongs.clear();
            allSongs.addAll(songs);
        });
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                navigateToResultFragment(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    hideResultFragment();
                } else {
                    navigateToResultFragment(newText);
                }
                return true;
            }
        });

        searchView.setOnCloseListener(() -> {
            hideResultFragment();
            return true;
        });
    }

    private void navigateToResultFragment(String query) {
        List<Song> filteredSongs = new ArrayList<>();
        for (Song song : allSongs) {
            if (song.getName().toLowerCase().contains(query.toLowerCase()) ||
                    (song.getArtist() != null && song.getArtist().toLowerCase().contains(query.toLowerCase()))) {
                filteredSongs.add(song);
            }
        }

        // Hiển thị ResultSeachFragment
        Bundle bundle = new Bundle();
        bundle.putSerializable("songs", (Serializable) filteredSongs);

        ResultSeachFragment resultFragment = new ResultSeachFragment();
        resultFragment.setArguments(bundle);

        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(R.id.result_container, resultFragment);
        transaction.addToBackStack(null);
        transaction.commit();

        View resultContainer = getView().findViewById(R.id.result_container);
        if (resultContainer != null) {
            resultContainer.setVisibility(View.VISIBLE);
        }
    }

    private void hideResultFragment() {
        View resultContainer = getView().findViewById(R.id.result_container);
        if (resultContainer != null) {
            resultContainer.setVisibility(View.GONE);
        }

        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        Fragment resultFragment = getChildFragmentManager().findFragmentById(R.id.result_container);
        if (resultFragment != null) {
            transaction.remove(resultFragment).commit();
        }
    }
}
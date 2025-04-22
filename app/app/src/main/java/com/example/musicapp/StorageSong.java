package com.example.musicapp;

import android.widget.Toast;

import com.example.musicapp.models.Song;
import java.util.ArrayList;

public class StorageSong {
    // Singleton instance
    private static StorageSong instance;

    // In-memory storage
    private static ArrayList<Song> songArrayList = new ArrayList<>();
    private static int songIndex = -1;

    private StorageSong() {
        // Private constructor to prevent instantiation
    }

    // Get singleton instance
    public static synchronized StorageSong getInstance() {
        if (instance == null) {
            instance = new StorageSong();
        }
        return instance;
    }

    // Store song list
    public void storeSongArrayList(ArrayList<Song> songArrayList) {
        StorageSong.songArrayList = new ArrayList<>(songArrayList); // Create a copy to avoid external modifications
    }

    // Load song list
    public ArrayList<Song> loadSongArrayList() {
        return songArrayList != null ? new ArrayList<>(songArrayList) : new ArrayList<>();
    }

    // Store song index
    public void storeSongIndex(int index) {
        songIndex = index;
    }

    // Load song index
    public int loadSongIndex() {
        return songIndex;
    }

    // No longer needed since data is not persistent
    public void clearCachedSong() {
        songArrayList = new ArrayList<>();
        songIndex = -1;
    }
}
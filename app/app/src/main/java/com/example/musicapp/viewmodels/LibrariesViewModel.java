package com.example.musicapp.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.musicapp.models.Artist;
import com.example.musicapp.models.Song;
import com.example.musicapp.repository.MusicRepository;
import java.util.ArrayList;

public class LibrariesViewModel extends ViewModel {
    private final MusicRepository repository;
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public LibrariesViewModel() {
        repository = MusicRepository.getInstance();
    }

    public LiveData<ArrayList<Song>> getSongs() {
        return repository.getSongs();
    }

    public LiveData<ArrayList<Artist>> getArtists() {
        return repository.getArtists();
    }

    public void addSong(Song song, String frameId) {
        repository.addSong(song, frameId);
    }

    public void updateSong(Song song) {
        repository.updateSong(song);
    }

    public void deleteSong(String songId) {
        repository.deleteSong(songId);
    }

    public void addArtist(Artist artist) {
        repository.addArtist(artist);
    }

    public void updateArtist(Artist artist) {
        repository.updateArtist(artist);
    }

    public void deleteArtist(String artistId) {
        repository.deleteArtist(artistId);
    }

    public LiveData<String> getError() {
        return error;
    }
}
package com.example.musicapp.repository;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.musicapp.models.Artist;
import com.example.musicapp.models.Frame;
import com.example.musicapp.models.Song;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MusicRepository {
    private static MusicRepository instance;
    private final DatabaseReference framesRef;
    private final DatabaseReference songsRef;
    private final DatabaseReference artistsRef;
    private final MutableLiveData<ArrayList<Frame>> framesLiveData;
    private final MutableLiveData<ArrayList<Song>> rawSongsLiveData;
    private final MutableLiveData<ArrayList<Artist>> artistsLiveData;
    private final MutableLiveData<ArrayList<Song>> songsLiveData;

    private MusicRepository() {
        framesRef = FirebaseDatabase.getInstance().getReference("frames");
        songsRef = FirebaseDatabase.getInstance().getReference("songs");
        artistsRef = FirebaseDatabase.getInstance().getReference("artists");
        framesLiveData = new MutableLiveData<>();
        rawSongsLiveData = new MutableLiveData<>();
        artistsLiveData = new MutableLiveData<>();
        songsLiveData = new MutableLiveData<>();
        setupRealtimeListeners();
    }

    public static synchronized MusicRepository getInstance() {
        if (instance == null) {
            instance = new MusicRepository();
        }
        return instance;
    }

    private void setupRealtimeListeners() {
        framesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<Frame> frames = new ArrayList<>();
                for (DataSnapshot frameSnapshot : snapshot.getChildren()) {
                    Frame frame = frameSnapshot.getValue(Frame.class);
                    if (frame != null) {
                        frame.setFrameId(frameSnapshot.getKey());
                        frames.add(frame);
                    }
                }
                framesLiveData.setValue(frames);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                framesLiveData.setValue(new ArrayList<>());
            }
        });

        songsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<Song> songs = new ArrayList<>();
                for (DataSnapshot songSnapshot : snapshot.getChildren()) {
                    Song song = songSnapshot.getValue(Song.class);
                    if (song != null && !TextUtils.isEmpty(song.getSongFileUrl())) {
                        song.setSongId(songSnapshot.getKey());
                        songs.add(song);
                    }
                }
                rawSongsLiveData.setValue(songs);
                updateSongsLiveData();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                rawSongsLiveData.setValue(new ArrayList<>());
                updateSongsLiveData();
            }
        });

        artistsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<Artist> artists = new ArrayList<>();
                for (DataSnapshot artistSnapshot : snapshot.getChildren()) {
                    Artist artist = artistSnapshot.getValue(Artist.class);
                    if (artist != null) {
                        artist.setArtistId(artistSnapshot.getKey());
                        artists.add(artist);
                    }
                }
                artistsLiveData.setValue(artists);
                updateSongsLiveData();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                artistsLiveData.setValue(new ArrayList<>());
                updateSongsLiveData();
            }
        });
    }

    private void updateSongsLiveData() {
        ArrayList<Song> songs = rawSongsLiveData.getValue();
        ArrayList<Artist> artists = artistsLiveData.getValue();
        if (songs == null || artists == null) {
            songsLiveData.setValue(new ArrayList<>());
            return;
        }

        Map<String, String> artistMap = new HashMap<>();
        for (Artist artist : artists) {
            artistMap.put(artist.getArtistId(), artist.getName());
        }

        ArrayList<Song> enrichedSongs = new ArrayList<>();
        for (Song song : songs) {
            Song enrichedSong = song;
            String artistName = artistMap.get(song.getArtistId());
            enrichedSong.setArtist(artistName != null ? artistName : "Unknown");
            enrichedSongs.add(enrichedSong);
        }
        songsLiveData.setValue(enrichedSongs);
    }

    public LiveData<ArrayList<Frame>> getFrames() {
        return framesLiveData;
    }

    public LiveData<ArrayList<Song>> getSongs() {
        return songsLiveData;
    }

    public LiveData<ArrayList<Artist>> getArtists() {
        return artistsLiveData;
    }

    public LiveData<ArrayList<Song>> getSongsByArtist(String artistId) {
        MutableLiveData<ArrayList<Song>> songsByArtist = new MutableLiveData<>();
        songsRef.orderByChild("artistId").equalTo(artistId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<Song> songs = new ArrayList<>();
                Map<String, String> artistMap = new HashMap<>();
                ArrayList<Artist> artists = artistsLiveData.getValue();
                if (artists != null) {
                    for (Artist artist : artists) {
                        artistMap.put(artist.getArtistId(), artist.getName());
                    }
                }
                for (DataSnapshot songSnapshot : snapshot.getChildren()) {
                    Song song = songSnapshot.getValue(Song.class);
                    if (song != null && !TextUtils.isEmpty(song.getSongFileUrl())) {
                        song.setSongId(songSnapshot.getKey());
                        String artistName = artistMap.get(song.getArtistId());
                        song.setArtist(artistName != null ? artistName : "Unknown");
                        songs.add(song);
                    }
                }
                songsByArtist.setValue(songs);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                songsByArtist.setValue(new ArrayList<>());
            }
        });
        return songsByArtist;
    }

    public void addSong(Song song, String frameId) {
        if (TextUtils.isEmpty(song.getSongFileUrl())) {
            return;
        }
        String songId = songsRef.push().getKey();
        song.setSongId(songId);
        songsRef.child(songId).setValue(song).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Frame.FrameSong frameSong = new Frame.FrameSong();
                frameSong.setSongId(songId);
                frameSong.setOrder(999);
                framesRef.child(frameId).child("songs").push().setValue(frameSong);
            }
        });
    }

    public void updateSong(Song song) {
        if (TextUtils.isEmpty(song.getSongFileUrl())) {
            return;
        }
        songsRef.child(song.getSongId()).setValue(song);
    }

    public void deleteSong(String songId) {
        songsRef.child(songId).removeValue();
        framesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot frameSnapshot : snapshot.getChildren()) {
                    for (DataSnapshot songSnapshot : frameSnapshot.child("songs").getChildren()) {
                        if (songSnapshot.child("songId").getValue(String.class).equals(songId)) {
                            songSnapshot.getRef().removeValue();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void addArtist(Artist artist) {
        String artistId = artistsRef.push().getKey();
        artist.setArtistId(artistId);
        artistsRef.child(artistId).setValue(artist).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Frame.FrameArtist frameArtist = new Frame.FrameArtist();
                frameArtist.setArtistId(artistId);
                frameArtist.setOrder(999);
                framesRef.child("frameId").child("artists").push().setValue(frameArtist);
            }
        });
    }

    public void updateArtist(Artist artist) {
        artistsRef.child(artist.getArtistId()).setValue(artist);
    }

    public void deleteArtist(String artistId) {
        artistsRef.child(artistId).removeValue();
        framesRef.child("frameId").child("artists").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot artistSnapshot : snapshot.getChildren()) {
                    if (artistSnapshot.child("artistId").getValue(String.class).equals(artistId)) {
                        artistSnapshot.getRef().removeValue();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
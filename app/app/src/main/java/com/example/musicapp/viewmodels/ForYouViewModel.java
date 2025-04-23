package com.example.musicapp.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.musicapp.models.Artist;
import com.example.musicapp.models.Frame;
import com.example.musicapp.models.Song;
import com.example.musicapp.repository.MusicRepository;
import java.util.ArrayList;
import java.util.Collections;

public class ForYouViewModel extends ViewModel {
    private final MusicRepository repository;
    private final MutableLiveData<ArrayList<Frame>> enrichedFramesLiveData;
    private final ArrayList<LiveData<?>> observers = new ArrayList<>();

    public ForYouViewModel() {
        repository = MusicRepository.getInstance();
        enrichedFramesLiveData = new MutableLiveData<>();
        enrichFrames();
    }

    private void enrichFrames() {
        LiveData<ArrayList<Frame>> framesLiveData = repository.getFrames();
        observers.add(framesLiveData);
        framesLiveData.observeForever(frames -> {
            if (frames == null) return;
            ArrayList<Frame> enrichedFrames = new ArrayList<>();
            for (Frame frame : frames) {
                Frame enrichedFrame = new Frame();
                enrichedFrame.setFrameId(frame.getFrameId());
                enrichedFrame.setName(frame.getName());
                enrichedFrame.setType(frame.getType());
                enrichedFrame.setSongs(frame.getSongs());
                enrichedFrame.setArtists(frame.getArtists());

                if (frame.getType() == 2) {
                    LiveData<ArrayList<Artist>> artistsLiveData = repository.getArtists();
                    observers.add(artistsLiveData);
                    artistsLiveData.observeForever(artists -> {
                        ArrayList<Artist> displayArtists = new ArrayList<>();
                        for (Frame.FrameArtist frameArtist : frame.getArtists()) {
                            for (Artist artist : artists) {
                                if (artist.getArtistId().equals(frameArtist.getArtistId())) {
                                    displayArtists.add(artist);
                                    break;
                                }
                            }
                        }
                        Collections.sort(displayArtists, (a1, a2) -> {
                            int order1 = frame.getArtists().stream()
                                    .filter(fa -> fa.getArtistId().equals(a1.getArtistId()))
                                    .findFirst()
                                    .map(Frame.FrameArtist::getOrder)
                                    .orElse(0);
                            int order2 = frame.getArtists().stream()
                                    .filter(fa -> fa.getArtistId().equals(a2.getArtistId()))
                                    .findFirst()
                                    .map(Frame.FrameArtist::getOrder)
                                    .orElse(0);
                            return Integer.compare(order1, order2);
                        });
                        enrichedFrame.setDisplayArtists(displayArtists);
                        updateEnrichedFrames(enrichedFrames, enrichedFrame);
                    });
                } else {
                    LiveData<ArrayList<Song>> songsLiveData = repository.getSongs();
                    observers.add(songsLiveData);
                    songsLiveData.observeForever(songs -> {
                        ArrayList<Song> displaySongs = new ArrayList<>();
                        for (Frame.FrameSong frameSong : frame.getSongs()) {
                            for (Song song : songs) {
                                if (song.getSongId().equals(frameSong.getSongId())) {
                                    displaySongs.add(song);
                                    break;
                                }
                            }
                        }
                        Collections.sort(displaySongs, (s1, s2) -> {
                            int order1 = frame.getSongs().stream()
                                    .filter(fs -> fs.getSongId().equals(s1.getSongId()))
                                    .findFirst()
                                    .map(Frame.FrameSong::getOrder)
                                    .orElse(0);
                            int order2 = frame.getSongs().stream()
                                    .filter(fs -> fs.getSongId().equals(s2.getSongId()))
                                    .findFirst()
                                    .map(Frame.FrameSong::getOrder)
                                    .orElse(0);
                            return Integer.compare(order1, order2);
                        });
                        enrichedFrame.setDisplaySongs(displaySongs);
                        updateEnrichedFrames(enrichedFrames, enrichedFrame);
                    });
                }
            }
        });
    }

    private void updateEnrichedFrames(ArrayList<Frame> enrichedFrames, Frame enrichedFrame) {
        int index = -1;
        for (int i = 0; i < enrichedFrames.size(); i++) {
            if (enrichedFrames.get(i).getFrameId().equals(enrichedFrame.getFrameId())) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            enrichedFrames.set(index, enrichedFrame);
        } else {
            enrichedFrames.add(enrichedFrame);
        }
        enrichedFramesLiveData.setValue(new ArrayList<>(enrichedFrames));
    }

    public LiveData<ArrayList<Frame>> getEnrichedFrames() {
        return enrichedFramesLiveData;
    }

    public LiveData<ArrayList<Song>> getSongsByArtist(String artistId) {
        return repository.getSongsByArtist(artistId);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        for (LiveData<?> observer : observers) {
            observer.removeObservers(null);
        }
        observers.clear();
    }
}
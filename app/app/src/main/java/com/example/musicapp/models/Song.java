package com.example.musicapp.models;

import java.io.Serializable;

public class Song implements Serializable {
    private String songId;
    private String name;
    private String artistId;
    private String artist;
    private String imageUrl;
    private String songFileUrl;
    private int totalDuration;

    public Song() {}

    public String getSongId() {
        return songId;
    }
    public void setSongId(String songId) {
        this.songId = songId;
    }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getArtistId() { return artistId; }
    public void setArtistId(String artistId) { this.artistId = artistId; }
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getSongFileUrl() { return songFileUrl; }
    public void setSongFileUrl(String songFileUrl) { this.songFileUrl = songFileUrl; }
    public int getTotalDuration() { return totalDuration; }
    public void setTotalDuration(int totalDuration) { this.totalDuration = totalDuration; }
}
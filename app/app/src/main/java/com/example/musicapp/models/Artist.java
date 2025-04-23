package com.example.musicapp.models;

import java.io.Serializable;

public class Artist implements Serializable {
    private String artistId;
    private String name;
    private String imageUrl;

    public Artist() {}


    public String getArtistId() {
        return artistId;
    }
    public void setArtistId(String artistId) {
        this.artistId = artistId;
    }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
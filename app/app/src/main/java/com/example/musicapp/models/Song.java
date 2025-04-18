package com.example.musicapp.models;


import java.io.Serializable;

public class Song implements Serializable {
    private String name; // Tên nghệ sĩ
    private String imageUrl; // ID của hình ảnh bai hat
    private String songFileUrl;
    private String artist;
    private long totalDuration;
    public Song() {
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public Song(String name, String imageUrl, String songFileUrl) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.songFileUrl = songFileUrl;
    }

    public Song(String name, String imageUrl) {
        this.name = name;
        this.imageUrl = imageUrl;
    }

    public long getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(long duration) {
        this.totalDuration = duration;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getName() {
            return name;
        }
    public String getImageUrl() {
        return imageUrl;
    }

    public String getSongFileUrl() {
        return songFileUrl;
    }

    public void setSongFileUrl(String songFileUrl) {
        this.songFileUrl = songFileUrl;
    }
}

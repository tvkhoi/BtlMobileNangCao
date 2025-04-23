package com.example.musicapp.models;

import java.util.ArrayList;

public class Frame {
    private String frameId;
    private String name;
    private int type;
    private ArrayList<FrameSong> songs;
    private ArrayList<FrameArtist> artists;
    private ArrayList<Song> displaySongs;
    private ArrayList<Artist> displayArtists;

    public Frame() {
        songs = new ArrayList<>();
        artists = new ArrayList<>();
        displaySongs = new ArrayList<>();
        displayArtists = new ArrayList<>();
    }

    public String getFrameId() {
        return frameId;
    }
    public void setFrameId(String frameId) {
        this.frameId = frameId;
    }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getType() { return type; }
    public void setType(int type) { this.type = type; }
    public ArrayList<FrameSong> getSongs() { return songs; }
    public void setSongs(ArrayList<FrameSong> songs) { this.songs = songs; }
    public ArrayList<FrameArtist> getArtists() { return artists; }
    public void setArtists(ArrayList<FrameArtist> artists) { this.artists = artists; }
    public ArrayList<Song> getDisplaySongs() { return displaySongs; }
    public void setDisplaySongs(ArrayList<Song> displaySongs) { this.displaySongs = displaySongs; }
    public ArrayList<Artist> getDisplayArtists() { return displayArtists; }
    public void setDisplayArtists(ArrayList<Artist> displayArtists) { this.displayArtists = displayArtists; }

    public static class FrameSong {
        private String songId;
        private int order;

        public FrameSong() {}

        public String getSongId() { return songId; }
        public void setSongId(String songId) { this.songId = songId; }
        public int getOrder() { return order; }
        public void setOrder(int order) { this.order = order; }
    }

    public static class FrameArtist {
        private String artistId;
        private int order;

        public FrameArtist() {}

        public String getArtistId() { return artistId; }
        public void setArtistId(String artistId) { this.artistId = artistId; }
        public int getOrder() { return order; }
        public void setOrder(int order) { this.order = order; }
    }
}
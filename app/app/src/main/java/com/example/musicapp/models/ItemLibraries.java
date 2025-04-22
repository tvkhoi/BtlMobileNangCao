package com.example.musicapp.models;

public class ItemLibraries {
    private int iconItem;
    private String nameItem;
    private String numSong;

    public ItemLibraries(int iconItem, String nameItem, String numSong) {
        this.iconItem = iconItem;
        this.nameItem = nameItem;
        this.numSong = numSong;
    }

    public int getIconItem() {
        return iconItem;
    }

    public void setIconItem(int iconItem) {
        this.iconItem = iconItem;
    }

    public String getNameItem() {
        return nameItem;
    }

    public void setNameItem(String nameItem) {
        this.nameItem = nameItem;
    }

    public String getNumSong() {
        return numSong;
    }

    public void setNumSong(String numSong) {
        this.numSong = numSong;
    }
}

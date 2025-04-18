package com.example.musicapp;

import android.view.View;

import com.example.musicapp.models.Song;

import java.util.ArrayList;

public interface IMyOnClickListener {
    public void myOnClick(View view, Song song);
    public void myClickToSendArrayList(int position, ArrayList<Song> songList);
}

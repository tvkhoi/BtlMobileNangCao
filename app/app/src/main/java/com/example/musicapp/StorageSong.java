package com.example.musicapp;

import android.content.Context;
import android.content.SharedPreferences;


import com.example.musicapp.models.Song;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;


public class StorageSong {
    private final String STORAGE = "com.example.appmusic.STORAGESONG";
    private SharedPreferences sharedPreferences;
    private Context context;

    public StorageSong(Context context) {
        this.context = context;
    }
    //lưu danh sách bài hát vào sharedPreferences
    public void storeSongArrayList(ArrayList<Song> songArrayList){
        sharedPreferences=context.getSharedPreferences(STORAGE,Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(songArrayList);
        editor.putString("songArrayList",json);
        editor.apply();
    }
    public ArrayList<Song> loadSongArrayList(){
        sharedPreferences = context.getSharedPreferences(STORAGE,Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString("songArrayList", null);
        Type type = new TypeToken<ArrayList<Song>>() {
        }.getType();
        ArrayList<Song> result = gson.fromJson(json, type);
        return  result != null ? result : new ArrayList<>();
    }
    public void storeSongIndex(int index){
        sharedPreferences = context.getSharedPreferences(STORAGE,Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("songIndex",index);
        editor.apply();
    }
    public int loadSongIndex(){
        sharedPreferences = context.getSharedPreferences(STORAGE,Context.MODE_PRIVATE);
        //-1 nếu không tìm thấy
        return sharedPreferences.getInt("songIndex",-1);
    }
    public void clearCachedSong(){
        sharedPreferences = context.getSharedPreferences(STORAGE,Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.commit();
    }
}

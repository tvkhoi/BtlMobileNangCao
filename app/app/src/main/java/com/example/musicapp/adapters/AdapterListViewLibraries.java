package com.example.musicapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.musicapp.R;
import com.example.musicapp.models.Song;

import java.util.List;

public class AdapterListViewLibraries extends ArrayAdapter<Song> {
    private List<Song> musicList;
    private Context context;


    public AdapterListViewLibraries(@NonNull Context context, List<Song> musicList) {
        super(context,R.layout.item_song_libraries , musicList);
        this.context=context;
        this.musicList = musicList;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_song_libraries,parent,false);
        TextView tv1,tv2;
        tv1=view.findViewById(R.id.nameSong_libraries);
        tv2=view.findViewById(R.id.nameArtist_libraries);
        ImageView img= view.findViewById(R.id.imgSong_libraries);
        Song music = musicList.get(position);

        tv1.setText(music.getName());
        tv2.setText(music.getArtist());
        Glide.with(context).load(music.getImageUrl()).into(img);
        return view;
    }
}

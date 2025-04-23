package com.example.musicapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicapp.R;
import com.example.musicapp.models.Song;

import java.util.ArrayList;

public class PlaySongAdapter extends RecyclerView.Adapter<PlaySongAdapter.ViewHolder> {
    private ArrayList<Song> songArrayList;
    private Context context;
    private OnSongClickListener listener;

    public interface OnSongClickListener {
        void onSongClick(int position);
    }

    public PlaySongAdapter(ArrayList<Song> songArrayList, Context context, OnSongClickListener listener) {
        this.songArrayList = songArrayList;
        this.context = context;
        this.listener = listener;
    }

    public void updateSongs(ArrayList<Song> newSongs) {
        this.songArrayList = newSongs;
        notifyDataSetChanged();
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_song_libraries,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Song song = songArrayList.get(position);
        holder.nameSong.setText(song.getName());
        Glide.with(context).load(song.getImageUrl())
                .placeholder(R.drawable.song)
                .into(holder.imgSong);
        holder.nameArtist.setText(song.getArtistId());
        holder.itemView.setOnClickListener(v -> listener.onSongClick(position));
    }

    @Override
    public int getItemCount() {
        if(songArrayList!=null)
            return songArrayList.size();
        return 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        private TextView nameSong,nameArtist;
        private ImageView imgSong;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameSong= itemView.findViewById(R.id.nameSong_libraries);
            imgSong= itemView.findViewById(R.id.imgSong_libraries);
            nameArtist= itemView.findViewById(R.id.nameArtist_libraries);
        }
    }
}

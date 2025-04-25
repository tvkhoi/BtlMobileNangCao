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

import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private List<Song> songs;
    private Context context;
    private OnSongClickListener listener;

    public interface OnSongClickListener {
        void onSongClick(Song song);
    }

    public SongAdapter(Context context, List<Song> songs, OnSongClickListener listener) {
        this.songs = songs;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.tvSongName.setText(song.getName());
        holder.tvArtistName.setText(song.getArtist());
        Glide.with(context)
                .load(song.getImageUrl())
                .placeholder(R.drawable.song)
                .into(holder.ivSongImage);

        holder.itemView.setOnClickListener(v -> listener.onSongClick(song));
    }

    @Override
    public int getItemCount() {
        return songs != null ? songs.size() : 0;
    }

    public static class SongViewHolder extends RecyclerView.ViewHolder {
        ImageView ivSongImage;
        TextView tvSongName;
        TextView tvArtistName;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            ivSongImage = itemView.findViewById(R.id.ivSongImage);
            tvSongName = itemView.findViewById(R.id.tvSongName);
            tvArtistName = itemView.findViewById(R.id.tvArtistName);
        }
    }
}
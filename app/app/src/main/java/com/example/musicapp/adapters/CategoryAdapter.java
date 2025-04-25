package com.example.musicapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicapp.R;
import com.example.musicapp.models.Frame;
import com.example.musicapp.models.Song;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private List<Frame> categories;
    private Context context;
    private OnSongClickListener songClickListener;

    public interface OnSongClickListener {
        void onSongClick(Frame frame, Song song);
    }

    public CategoryAdapter(Context context, List<Frame> categories, OnSongClickListener listener) {
        this.categories = categories;
        this.context = context;
        this.songClickListener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Frame frame = categories.get(position);
        holder.textViewCategory.setText(frame.getName());

        // Hiển thị danh sách bài hát trong frame
        List<Song> songs = frame.getDisplaySongs();
        if (songs == null) songs = new ArrayList<>();

        SongAdapter songAdapter = new SongAdapter(context, songs, song -> {
            songClickListener.onSongClick(frame, song);
        });
        holder.recyclerViewSongs.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        holder.recyclerViewSongs.setAdapter(songAdapter);
    }

    @Override
    public int getItemCount() {
        return categories != null ? categories.size() : 0;
    }

    public static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView textViewCategory;
        RecyclerView recyclerViewSongs;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewCategory = itemView.findViewById(R.id.textViewCategory);
            recyclerViewSongs = itemView.findViewById(R.id.recyclerViewSongs);
        }
    }
}
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
import com.example.musicapp.models.Artist;

import java.util.List;

public class TrendingArtistAdapter extends RecyclerView.Adapter<TrendingArtistAdapter.ArtistViewHolder> {

    private List<Artist> artists;
    private Context context;
    private OnArtistClickListener listener;

    public interface OnArtistClickListener {
        void onArtistClick(Artist artist);
    }

    public TrendingArtistAdapter(Context context, List<Artist> artists, OnArtistClickListener listener) {
        this.artists = artists;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ArtistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_trending_artist, parent, false);
        return new ArtistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArtistViewHolder holder, int position) {
        Artist artist = artists.get(position);
        holder.textViewArtistName.setText(artist.getName());
        Glide.with(context)
                .load(artist.getImageUrl())
                .placeholder(R.drawable.song)
                .into(holder.imageViewArtist);

        holder.itemView.setOnClickListener(v -> listener.onArtistClick(artist));
    }

    @Override
    public int getItemCount() {
        return artists != null ? artists.size() : 0;
    }

    public static class ArtistViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewArtist;
        TextView textViewArtistName;

        public ArtistViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewArtist = itemView.findViewById(R.id.imageViewArtist);
            textViewArtistName = itemView.findViewById(R.id.textViewArtistName);
        }
    }
}
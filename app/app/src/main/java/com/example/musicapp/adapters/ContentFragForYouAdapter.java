package com.example.musicapp.adapters;

import android.content.Context;
import android.content.Intent;
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
import com.example.musicapp.activities.PlaySongActivity;
import com.example.musicapp.models.Artist;
import com.example.musicapp.models.Frame;
import com.example.musicapp.models.Song;
import java.util.ArrayList;

public class ContentFragForYouAdapter extends RecyclerView.Adapter<ContentFragForYouAdapter.ViewHolder> {
    private ArrayList<Frame> frameList;
    private Context context;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onSongClick(int framePosition, int songPosition);
        void onArtistClick(int framePosition, int artistPosition);
    }

    public ContentFragForYouAdapter(ArrayList<Frame> frameList, Context context, OnItemClickListener listener) {
        this.frameList = frameList;
        this.context = context;
        this.listener = listener;
    }

    public void updateFrames(ArrayList<Frame> frames) {
        this.frameList.clear();
        this.frameList.addAll(frames);
        notifyDataSetChanged();
    }

    public ArrayList<Frame> getFrameList() {
        return frameList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_fragment_content, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Frame frame = frameList.get(position);
        holder.tvFrameName.setText(frame.getName() != null ? frame.getName() : "Unknown");

        // Tắt nested scrolling cho RecyclerView con để tránh xung đột
        holder.recyclerView.setNestedScrollingEnabled(false);

        if (frame.getType() == 2) {
            ArtistAdapter artistAdapter = new ArtistAdapter(frame.getDisplayArtists(), position);
            holder.recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
            holder.recyclerView.setAdapter(artistAdapter);
        } else {
            SongAdapter songAdapter = new SongAdapter(frame.getDisplaySongs(), position);
            holder.recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
            holder.recyclerView.setAdapter(songAdapter);
        }
    }

    @Override
    public int getItemCount() {
        return frameList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFrameName;
        RecyclerView recyclerView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFrameName = itemView.findViewById(R.id.tvRecently);
            recyclerView = itemView.findViewById(R.id.recRecentlySong);
        }
    }

    private class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {
        private ArrayList<Song> songs;
        private int framePosition;

        public SongAdapter(ArrayList<Song> songs, int framePosition) {
            this.songs = songs;
            this.framePosition = framePosition;
        }

        @NonNull
        @Override
        public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_recently_song, parent, false);
            return new SongViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
            Song song = songs.get(position);
            holder.tvSongName.setText(song.getName() != null ? song.getName() : "Unknown");
            Glide.with(context).load(song.getImageUrl() != null ? song.getImageUrl() : R.drawable.song).into(holder.imgSong);

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, PlaySongActivity.class);
                intent.putExtra("songList", songs);
                intent.putExtra("position", position);
                intent.putExtra("isPlaying", true);
                context.startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return songs.size();
        }

        public class SongViewHolder extends RecyclerView.ViewHolder {
            ImageView imgSong;
            TextView tvSongName;

            public SongViewHolder(@NonNull View itemView) {
                super(itemView);
                imgSong = itemView.findViewById(R.id.imgSong);
                tvSongName = itemView.findViewById(R.id.nameSong);
            }
        }
    }

    private class ArtistAdapter extends RecyclerView.Adapter<ArtistAdapter.ArtistViewHolder> {
        private ArrayList<Artist> artists;
        private int framePosition;

        public ArtistAdapter(ArrayList<Artist> artists, int framePosition) {
            this.artists = artists;
            this.framePosition = framePosition;
        }

        @NonNull
        @Override
        public ArtistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_artist, parent, false);
            return new ArtistViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ArtistViewHolder holder, int position) {
            Artist artist = artists.get(position);
            holder.tvArtistName.setText(artist.getName() != null ? artist.getName() : "Unknown");
            Glide.with(context).load(artist.getImageUrl() != null ? artist.getImageUrl() : R.drawable.artist).into(holder.imgArtist);

            holder.itemView.setOnClickListener(v -> listener.onArtistClick(framePosition, position));
        }

        @Override
        public int getItemCount() {
            return artists.size();
        }

        public class ArtistViewHolder extends RecyclerView.ViewHolder {
            ImageView imgArtist;
            TextView tvArtistName;

            public ArtistViewHolder(@NonNull View itemView) {
                super(itemView);
                imgArtist = itemView.findViewById(R.id.imgArtist);
                tvArtistName = itemView.findViewById(R.id.tvArtistName);
            }
        }
    }
}
package com.example.musicapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicapp.R;
import com.example.musicapp.SongDownloadManager;
import com.example.musicapp.fragments.FragmentLibraries;
import com.example.musicapp.models.ItemLibraries;
import com.example.musicapp.models.Song;

import java.util.ArrayList;
import java.util.List;

public class AdapterRecyLibraries extends RecyclerView.Adapter<AdapterRecyLibraries.ItemLibrariesViewHolder> {

    private Context context;
    private List<ItemLibraries> libraries;
    private FragmentLibraries fragment; // Reference to FragmentLibraries

    public AdapterRecyLibraries(Context context, List<ItemLibraries> libraries, FragmentLibraries fragment) {
        this.context = context;
        this.libraries = libraries;
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public ItemLibrariesViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_libraries, viewGroup, false);
        return new ItemLibrariesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemLibrariesViewHolder holder, int position) {
        ItemLibraries item = libraries.get(position);
        holder.tv1.setText(item.getNameItem());
        holder.tv2.setText(item.getNumSong());
        holder.img.setImageResource(item.getIconItem());

        holder.itemView.setOnClickListener(v -> {
            if (item.getNameItem().equals("Download")) {
                // Get downloaded songs
                SongDownloadManager downloadManager = SongDownloadManager.getInstance(context);
                ArrayList<Song> downloadedSongs = downloadManager.getDownloadedSongs();

                if (downloadedSongs.isEmpty()) {
                    Toast.makeText(context, "No downloaded songs available", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Update ListView in FragmentLibraries
                fragment.updateDownloadedSongs(downloadedSongs);
            }
        });
    }

    @Override
    public int getItemCount() {
        if (libraries != null) return libraries.size();
        return 0;
    }

    public class ItemLibrariesViewHolder extends RecyclerView.ViewHolder {
        TextView tv1, tv2;
        ImageView img;

        public ItemLibrariesViewHolder(@NonNull View itemView) {
            super(itemView);
            tv1 = itemView.findViewById(R.id.nameItem_libraries);
            tv2 = itemView.findViewById(R.id.numSong_libraries);
            img = itemView.findViewById(R.id.icon_libraries);
        }
    }
}
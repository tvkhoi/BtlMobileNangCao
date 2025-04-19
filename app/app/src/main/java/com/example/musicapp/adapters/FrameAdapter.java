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
import com.example.musicapp.IMyOnClickListener;
import com.example.musicapp.R;
import com.example.musicapp.models.Song;

import java.util.ArrayList;


public class FrameAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    private ArrayList<Song> listSong;
    private Context context;
    private int layoutType;
    private IMyOnClickListener myOnClick;
    public FrameAdapter(Context context, ArrayList<Song> listSong, int layoutType) {
        this.context=context;
        this.listSong = listSong;
        this.layoutType=layoutType;
    }

    public void setMyOnClick(IMyOnClickListener myOnClick) {
        this.myOnClick = myOnClick;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(layoutType == 0){
            View view = LayoutInflater.from(context).inflate(R.layout.item_featuring
                    ,parent,false);
            return new FrameViewHolder3(view);
        }
        else if(layoutType == 1){
            View view = LayoutInflater.from(context).inflate(R.layout.item_recently_song
                    ,parent,false);
            return new FrameViewHolder(view);
        }
        else{
            View view = LayoutInflater.from(context).inflate(R.layout.item_song_play_list
                    ,parent,false);
            return new FrameViewHolder2(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Song song = listSong.get(position);
        if(holder instanceof  FrameViewHolder3){
            Glide.with(context).load(song.getImageUrl()).into(((FrameViewHolder3) holder).imgSong);
            ((FrameViewHolder3) holder).tvSong.setText(song.getName());
        }
        else if(holder instanceof FrameViewHolder){
            Glide.with(context).load(song.getImageUrl()).into(((FrameViewHolder) holder).imgSong);
            ((FrameViewHolder) holder).tvSong.setText(song.getName());
        }else{
            Glide.with(context).load(song.getImageUrl()).into(((FrameViewHolder2) holder).imgSong);
            ((FrameViewHolder2) holder).tvSong.setText(song.getName());
        }
    }

    @Override
    public int getItemCount() {
        if (listSong != null)
            return listSong.size();
        return 0;
    }

    // viewholder cho item bài hát
    public class FrameViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        ImageView imgSong;
        TextView tvSong;

        public FrameViewHolder(@NonNull View itemView) {
            super(itemView);
            imgSong = itemView.findViewById(R.id.imgSong);
            tvSong = itemView.findViewById(R.id.nameSong);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if(myOnClick!=null){
                myOnClick.myClickToSendArrayList(getAdapterPosition(),listSong);
            }

        }
    }
    // viewholder cho item nghệ sĩ, danh sách bài hát,.....
    public class FrameViewHolder2 extends RecyclerView.ViewHolder {
        ImageView imgSong;
        TextView tvSong;

        public FrameViewHolder2(@NonNull View itemView) {
            super(itemView);
            imgSong = itemView.findViewById(R.id.imgSongPlayList);
            tvSong = itemView.findViewById(R.id.nameSongPlayList);
        }
    }
    // viewholder cho item tiêu biểu hôm nay
    public class FrameViewHolder3 extends RecyclerView.ViewHolder {
        ImageView imgSong;
        TextView tvSong;

        public FrameViewHolder3(@NonNull View itemView) {
            super(itemView);
            imgSong = itemView.findViewById(R.id.imgFeaturing);
            tvSong = itemView.findViewById(R.id.tvNameFeaturing);
        }
    }

}

package com.example.musicapp.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.musicapp.fragments.FragmentForYou;

public class ViewPagerAdapterScrollBar extends FragmentStateAdapter {
    private static final int NUM_OF_FRAGMENT = 1;
    public ViewPagerAdapterScrollBar(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position){
            case 0:
                return new FragmentForYou();
            default:
                return new FragmentForYou();
        }
    }

    @Override
    public int getItemCount() {
        return NUM_OF_FRAGMENT;
    }

}

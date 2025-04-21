package com.example.musicapp.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager2.widget.ViewPager2;

import com.example.musicapp.R;
import com.example.musicapp.adapters.ActivityMainAdapter;
import com.example.musicapp.fragments.MiniPlayerFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView menuBottom;
    private ViewPager2 viewPager2;
    private FrameLayout frameMiniPlayer;
    private ActivityMainAdapter adapterVG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        menuBottom = findViewById(R.id.bottomBar);
        viewPager2 = findViewById(R.id.vg_fragMain);
        frameMiniPlayer = findViewById(R.id.frameMiniPlayer);

        //tắt hiệu ứng vuốt của fragment (hiệu ứng chuyển qua lại giữa home-search-library)
        viewPager2.setUserInputEnabled(false);
        adapterVG = new ActivityMainAdapter(this);
        viewPager2.setAdapter(adapterVG);

        initializeMiniPlayer();
        setupNavigation();
    }

    private void initializeMiniPlayer() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.frameMiniPlayer, MiniPlayerFragment.newInstance());
        transaction.commit();
    }

    private void setupNavigation() {
        menuBottom.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.homeIcon) {
                viewPager2.setCurrentItem(0);
            } else if (itemId == R.id.searchIcon) {
                viewPager2.setCurrentItem(1);
            } else if (itemId == R.id.libraryIcon) {
                viewPager2.setCurrentItem(2);
            }
            return true;
        });
    }
}
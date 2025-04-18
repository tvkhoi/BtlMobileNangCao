package com.example.musicapp.fragments;


import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.musicapp.R;
import com.example.musicapp.activities.RegisterActivity;
import com.example.musicapp.adapters.ViewPagerAdapterScrollBar;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import de.hdodenhof.circleimageview.CircleImageView;


public class FragmentMain extends Fragment{
    private TabLayout tabLayoutScrollBar;
    private ViewPagerAdapterScrollBar VPAdapterScrollBar;
    private ViewPager2 viewPagerFragmenMain;
    private TextView hiUser;
    private FirebaseAuth mAuth;
    CircleImageView circleImageView;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main,container,false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewPagerFragmenMain= view.findViewById(R.id.viewPagerFragmentMain);
        tabLayoutScrollBar= view.findViewById(R.id.tabLayoutScrollBar);
        hiUser = view.findViewById(R.id.hiUser);
        circleImageView = view.findViewById(R.id.imageUser);
        setHelloUserName();
        circleImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent =new Intent(getActivity(), RegisterActivity.class);
                startActivity(intent);
            }
        });

        VPAdapterScrollBar = new ViewPagerAdapterScrollBar(getActivity());
        viewPagerFragmenMain.setAdapter(VPAdapterScrollBar);

        new TabLayoutMediator(tabLayoutScrollBar, viewPagerFragmenMain, (tab, position) -> {
            switch (position){
                case 0:
                    tab.setText(R.string.bar_foryou);
                    break;
                default:
                    tab.setText(R.string.bar_foryou);
                    break;
            }
        }).attach();

        tabLayoutScrollBar.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                tab.view.setBackgroundResource(R.drawable.rounded_background_solid);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                tab.view.setBackgroundResource(R.drawable.rounded_background);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        TabLayout.Tab firstTab = tabLayoutScrollBar.getTabAt(0);
        firstTab.view.setBackgroundResource(R.drawable.rounded_background_solid);
    }

    private void setHelloUserName(){
        mAuth=FirebaseAuth.getInstance();
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        //lấy người dùng hiện tại
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser!=null){
            //lấy id
            String userID = currentUser.getUid();
            //tìm trong collection có document chứa userID
            database.collection("user").document(userID)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            //nếu tìm thấy
                            if(task.isSuccessful()){
                                String userName = task.getResult().getString("userName");
                                hiUser.setText(getString(R.string.hi_username)+" "+userName+",");
                            }else{
                                Toast.makeText(getContext(),task.getException().toString()
                                        ,Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }
    }

}

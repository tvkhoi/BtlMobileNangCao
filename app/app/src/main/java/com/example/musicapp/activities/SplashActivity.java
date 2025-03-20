package com.example.musicapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.example.musicapp.R;
import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {
    private Handler handler ;
    private FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        handler = new Handler();
        mAuth = FirebaseAuth.getInstance();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = null;
                if(mAuth.getCurrentUser() == null) {
                    intent = new Intent(SplashActivity.this, RegisterActivity.class); // Chưa đăng nhập -> Đăng ký
                }else {
                    intent = new Intent(SplashActivity.this, MainActivity.class); // Đã đăng nhập -> Vào app chính
                }
                // Intent intent = new Intent(SplashActivity.this, RegisterActivity.class);
                startActivity(intent);
                finish();
            }
        },2000);
    }
}
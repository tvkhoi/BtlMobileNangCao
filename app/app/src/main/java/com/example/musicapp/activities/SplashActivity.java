package com.example.musicapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.musicapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {
    private Handler handler;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        handler = new Handler();
        mAuth = FirebaseAuth.getInstance();

        handler.postDelayed(() -> checkUserStatus(), 2000);
    }

    private void checkUserStatus() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            // Tải lại thông tin từ Firebase để kiểm tra xem tài khoản có bị xóa không
            user.reload().addOnCompleteListener(task -> {
                FirebaseUser updatedUser = mAuth.getCurrentUser();
                if (updatedUser == null) {
                    // Tài khoản không còn tồn tại -> Đăng xuất
                    handleAccountDeleted();
                } else {
                    // Tài khoản hợp lệ -> Chuyển vào MainActivity
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    finish();
                }
            });
        } else {
            // Người dùng chưa đăng nhập -> Chuyển sang màn hình đăng ký
            startActivity(new Intent(SplashActivity.this, RegisterActivity.class));
            finish();
        }
    }

    private void handleAccountDeleted() {
        mAuth.signOut(); // Đăng xuất Firebase
        Toast.makeText(this, "Tài khoản của bạn đã bị xóa!", Toast.LENGTH_LONG).show();
        startActivity(new Intent(SplashActivity.this, RegisterActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}

package com.example.musicapp.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.musicapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {
    private Handler handler;
    private FirebaseAuth mAuth;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 100;
    private boolean isPermissionChecked = false;

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
            // Chưa đăng nhập -> Yêu cầu quyền trước khi vào RegisterActivity
            requestNotificationPermission();
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                );
            } else {
                proceedToRegister();
            }
        } else {
            proceedToRegister();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Đã bật thông báo", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Bạn có thể bật quyền sau trong Cài đặt", Toast.LENGTH_LONG).show();
            }
            proceedToRegister();
        }
    }

    private void proceedToRegister() {
        startActivity(new Intent(SplashActivity.this, RegisterActivity.class));
        finish();
    }

    private void handleAccountDeleted() {
        mAuth.signOut(); // Đăng xuất Firebase
        Toast.makeText(this, "Tài khoản của bạn đã bị xóa!", Toast.LENGTH_LONG).show();
        startActivity(new Intent(SplashActivity.this, RegisterActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}

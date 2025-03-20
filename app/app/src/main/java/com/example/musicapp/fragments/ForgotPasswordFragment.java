package com.example.musicapp.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;


import com.example.musicapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordFragment extends Fragment {
    private TextView goBack;
    private FrameLayout frameLayout;

    private EditText email;
    private TextView responseMessage;
    private Button resetPasswordButton;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_forgot_password, container, false);
        goBack = view.findViewById(R.id.go_back);
        frameLayout = getActivity().findViewById(R.id.register_frame_layout);

        email = view.findViewById(R.id.email);
        responseMessage = view.findViewById(R.id.responseMessage);
        resetPasswordButton = view.findViewById(R.id.resetPasswordButton);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        goBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setFragment(new SignInFragment());
            }
        });

        email.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                checkInput();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        resetPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetPassword();
            }
        });
    }

    private void resetPassword() {
        if(email.getText().toString().matches("[a-zA-Z0-9._-]+@[a-z]+\\.[a-z]+")){
            mAuth.sendPasswordResetEmail(email.getText().toString()).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        responseMessage.setText("Kiểm tra Email của bạn.");
                        responseMessage.setTextColor(getResources().getColor(R.color.green));
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Không xác định được lỗi.";
                        responseMessage.setText("Có sự cố khi gửi Email: " + error);
                        responseMessage.setTextColor(getResources().getColor(R.color.red));
                    }
                    responseMessage.setVisibility(View.VISIBLE);
                }
            });
        }else {
            email.setError("Email không hợp lệ");
            resetPasswordButton.setEnabled(true);
            resetPasswordButton.setTextColor(getResources().getColor(R.color.white));
        }
    }

    private void checkInput() {
        if(!email.getText().toString().isEmpty()){
            resetPasswordButton.setEnabled(true);
            resetPasswordButton.setTextColor(getResources().getColor(R.color.white));

        }else {
            resetPasswordButton.setEnabled(false);
            resetPasswordButton.setTextColor(getResources().getColor(R.color.color_text_artist));
        }
    }

    private void setFragment(Fragment fragment){
        FragmentTransaction fragmentTransaction =getActivity().getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.from_left , R.anim.out_from_right);
        fragmentTransaction.replace(frameLayout.getId(),fragment);
        fragmentTransaction.commit();
    }
}
package com.piddlepops.awardl;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import com.piddlepops.awardl.databinding.ActivityLoginBinding;

import java.text.MessageFormat;
import java.util.Random;
import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity {

    ActivityLoginBinding binding;
    String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.ivCustomLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://piddlepops.com"));
                startActivity(intent);
            }
        });

        binding.btnLogin.setOnClickListener(view -> {
            if (binding.etName.getText().toString().isEmpty() || !binding.etName.getText().toString().matches("[a-zA-Z]{1,}\s[a-zA-Z]{1,}")) {
                binding.etName.setError("Enter Valid First & Last name");
                return;
            }
            if (binding.etEmail.getText().toString().isEmpty() || !binding.etEmail.getText().toString().matches(emailPattern)) {
                binding.etEmail.setError("Enter Valid Email Address");
                return;
            }

            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra("name", binding.etName.getText().toString());
            intent.putExtra("email", binding.etEmail.getText().toString());
            finish();
            startActivity(intent);
        });
    }
}
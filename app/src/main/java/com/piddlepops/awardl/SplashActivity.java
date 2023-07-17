package com.piddlepops.awardl;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;
import com.piddlepops.awardl.databinding.IpSettingDialogBinding;

public class SplashActivity extends AppCompatActivity {

    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences("AWARDL", Context.MODE_PRIVATE);

        setContentView(R.layout.activity_splash);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        ImageView iv = findViewById(R.id.ivSplash);

        Glide.with(this).load(R.drawable.loading).into(iv);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (sharedPreferences.getString("IP", "").length() == 0) {
                    Dialog dialog = new Dialog(SplashActivity.this);

                    IpSettingDialogBinding ipSettingDialogBinding = IpSettingDialogBinding.inflate(getLayoutInflater());

                    dialog.setCancelable(false);

                    ViewGroup.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

                    dialog.setContentView(ipSettingDialogBinding.getRoot(), layoutParams);

                    ipSettingDialogBinding.etIPAddress.setText(APIInterface.BASE_URL);

                    ipSettingDialogBinding.btnSubmit.setOnClickListener(view -> {
                        if (ipSettingDialogBinding.etIPAddress.getText().toString().length() > 0 && ipSettingDialogBinding.etIPAddress.getText().toString().contains("http://")) {
                            sharedPreferences.edit().putString("IP", ipSettingDialogBinding.etIPAddress.getText().toString()).commit();
                            finish();
                            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                        }
                    });

                    dialog.show();
                    Window window = dialog.getWindow();
                    window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
                } else {
                    finish();
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                }
            }
        }, 2800);
    }
}
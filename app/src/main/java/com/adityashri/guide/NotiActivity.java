package com.adityashri.guide;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class NotiActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_noti);
        finish();
    }
}
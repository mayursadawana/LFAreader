package com.example.lfareader;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Toast;

public class CameraActivity extends AppCompatActivity {

    private String imageName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        imageName = getIntent().getStringExtra("fileName");
        Toast.makeText(this, imageName, Toast.LENGTH_SHORT).show();

    }
}
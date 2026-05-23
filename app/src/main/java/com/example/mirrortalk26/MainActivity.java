package com.example.mirrortalk26;

import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fix system bar colors — prevents content going behind status/nav bars
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        getWindow().setStatusBarColor(Color.parseColor("#0D0D1A"));
        getWindow().setNavigationBarColor(Color.parseColor("#0D0D1A"));

        setContentView(R.layout.activity_main);
    }
}
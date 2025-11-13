package com.code.wlu.cp470.wellnest;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.code.wlu.cp470.wellnest.utils.MusicService;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host);
        NavController navController = navHostFragment.getNavController();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startService(new Intent(this, MusicService.class));
    }

    @Override
    protected void onPause() {
        super.onPause();
        Intent i = new Intent(this, MusicService.class);
        i.setAction(MusicService.ACTION_PAUSE);
        startService(i);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent i = new Intent(this, MusicService.class);
        i.setAction(MusicService.ACTION_RESUME);
        startService(i);
    }


    @Override
    protected void onStop() {
        super.onStop();
        stopService(new Intent(this, MusicService.class));
    }
}

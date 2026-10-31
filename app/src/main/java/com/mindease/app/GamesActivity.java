package com.mindease.app;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.mindease.app.databinding.ActivityGamesBinding;

// Game hub screen providing navigation to all available mindfulness
// mini-games: Bubble Pop, Color Sort, Zen Doodle, and Star Catcher.
public class GamesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityGamesBinding binding = ActivityGamesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnBubblePop.setOnClickListener(v ->
                startActivity(new Intent(this, BubblePopGameActivity.class)));
        binding.btnColorSort.setOnClickListener(v ->
                startActivity(new Intent(this, ColorSortGameActivity.class)));
        binding.btnZenDoodle.setOnClickListener(v ->
                startActivity(new Intent(this, ZenDoodleActivity.class)));
        binding.btnStarCatcher.setOnClickListener(v ->
                startActivity(new Intent(this, StarCatcherActivity.class)));
    }
}

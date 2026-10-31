package com.mindease.app;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.mindease.app.databinding.ActivityColorSortBinding;

// Color sorting puzzle mini-game. The player swaps adjacent color tiles
// to arrange them in the correct order (Red, Green, Blue, Yellow).
// Provides calming focus-based gameplay with a completion celebration.
public class ColorSortGameActivity extends AppCompatActivity {

    private ActivityColorSortBinding binding;
    private final int[] targetOrder = {0, 1, 2, 3}; // R G B Y
    private int[] currentOrder = {2, 0, 3, 1};
    private int selected = -1;

    private final String[] colorNames = {"🔴 Red", "🟢 Green", "🔵 Blue", "🟡 Yellow"};
    private final String[] colorHex = {"#ED4545", "#2E9E8F", "#4A90E2", "#FCBA4A"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityColorSortBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        binding.tvInstruction.setText("Sort the colors: Red → Green → Blue → Yellow");

        setupTiles();
    }

    private void setupTiles() {
        android.widget.Button[] tiles = {
            binding.tile1, binding.tile2, binding.tile3, binding.tile4
        };
        for (int i = 0; i < tiles.length; i++) {
            final int idx = i;
            tiles[i].setText(colorNames[currentOrder[i]]);
            tiles[i].setBackgroundColor(android.graphics.Color.parseColor(colorHex[currentOrder[i]]));
            tiles[i].setOnClickListener(v -> onTileClick(idx, tiles));
        }
    }

    private void onTileClick(int idx, android.widget.Button[] tiles) {
        if (selected == -1) {
            selected = idx;
            tiles[idx].setAlpha(0.5f);
        } else {
            // Swap
            int tmp = currentOrder[selected];
            currentOrder[selected] = currentOrder[idx];
            currentOrder[idx] = tmp;
            tiles[selected].setAlpha(1.0f);
            selected = -1;
            setupTiles();
            checkWin();
        }
    }

    private void checkWin() {
        for (int i = 0; i < currentOrder.length; i++) {
            if (currentOrder[i] != targetOrder[i]) return;
        }
        Toast.makeText(this, "🎉 Perfect! Colors sorted!", Toast.LENGTH_LONG).show();
    }
}

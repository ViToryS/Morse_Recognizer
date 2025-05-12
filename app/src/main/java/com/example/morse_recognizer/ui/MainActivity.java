package com.example.morse_recognizer.ui;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.example.morse_recognizer.R;
import com.example.morse_recognizer.viewmodel.MorseViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private TextView headerTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        headerTitle = findViewById(R.id.header_title);
        MorseViewModel viewModel = new ViewModelProvider(this).get(MorseViewModel.class);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        NavHostFragment navHostFragment = (NavHostFragment)getSupportFragmentManager().findFragmentById(
                R.id.nav_host_fragment);
        assert navHostFragment != null;
        NavController navController = navHostFragment.getNavController();

        NavigationUI.setupWithNavController(bottomNavigationView, navController);
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
        int destId = destination.getId();

        if (destId == R.id.sendingFragment) {
            headerTitle.setText("Передача сообщений");
        } else if (destId == R.id.recognizingFragment) {
            headerTitle.setText("Распознавание ");
        } else {
            headerTitle.setText("Морзе");
        }
        });
    }
}
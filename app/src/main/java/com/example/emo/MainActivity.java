package com.example.emo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.emo.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private NavController navController;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        }

        try {
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            Log.d(TAG, "Инициализация MainActivity");

            setSupportActionBar(binding.toolbar);

            navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
            
            // Настройка нижней навигации
            BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
            if (bottomNavigationView != null) {
                bottomNavigationView.setOnItemSelectedListener(item -> {
                    return NavigationUI.onNavDestinationSelected(item, navController);
                });
            }

            // Настройка AppBarConfiguration без drawer
            appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.HomeFragment, R.id.FirstFragment, R.id.TestsFragment, 
                    R.id.AboutActivity, R.id.ProfileActivity)
                    .build();

            // Настройка NavigationUI
            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                Log.d(TAG, "Навигация к: " + destination.getLabel() + ", id: " + destination.getId());
                if (destination.getId() == R.id.HomeFragment) {
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().hide();
                    }
                } else {
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().show();
                    }
                    if (destination.getId() == R.id.FirstFragment) {
                        if (getSupportActionBar() != null) {
                            getSupportActionBar().setTitle("Оцените свое состояние");
                        }
                    }
                    // Скрываем кнопку назад для ProfileFragment и AboutFragment
                    if (destination.getId() == R.id.ProfileFragment || destination.getId() == R.id.AboutFragment) {
                        if (getSupportActionBar() != null) {
                            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                        }
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Ошибка в onCreate: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Дополнительная проверка аутентификации при возвращении к активности
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.w(TAG, "Пользователь не авторизован в onStart, перенаправление на LoginActivity");
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish(); // Закрываем MainActivity, чтобы пользователь не мог вернуться назад
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
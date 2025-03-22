package com.example.emo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.emo.databinding.ActivityMainBinding;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private NavController navController;
    private DrawerLayout drawer;
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

            drawer = binding.getRoot().findViewById(R.id.drawer_layout);
            NavigationView navigationView = findViewById(R.id.nav_view);
            navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

            // Настройка AppBarConfiguration с учетом drawer
            appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.FirstFragment, R.id.TestsFragment, R.id.AiPsychologistFragment, R.id.ChartsFragment)
                    .setOpenableLayout(drawer)
                    .build();

            // Настройка NavigationUI
            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
            NavigationUI.setupWithNavController(navigationView, navController);

            // Обработчик выбора пунктов меню
            navigationView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                Log.d(TAG, "Нажат элемент меню с id: " + id);

                try {
                    if (id == R.id.FirstFragment) {
                        Log.d(TAG, "Переход к FirstFragment");
                        navController.navigate(R.id.FirstFragment);
                    } else if (id == R.id.TestsFragment) {
                        Log.d(TAG, "Переход к TestsFragment");
                        navController.navigate(R.id.TestsFragment);
                    } else if (id == R.id.AiPsychologistFragment) {
                        Log.d(TAG, "Переход к AiPsychologistFragment");
                        navController.navigate(R.id.AiPsychologistFragment);
                    } else if (id == R.id.ChartsFragment) {
                        Log.d(TAG, "Переход к ChartsFragment");
                        navController.navigate(R.id.ChartsFragment);
                    } else if (id == R.id.action_logout) {
                        FirebaseAuth.getInstance().signOut();
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        finish();
                        return true;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при навигации: " + e.getMessage());
                    Toast.makeText(MainActivity.this, "Ошибка навигации: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    return false;
                }

                drawer.closeDrawer(GravityCompat.START);
                return true;
            });

            // Логирование навигации
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                Log.d(TAG, "Навигация к: " + destination.getLabel() + ", id: " + destination.getId());
            });

        } catch (Exception e) {
            Log.e(TAG, "Ошибка в onCreate: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return NavigationUI.onNavDestinationSelected(item, navController)
                || super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
package com.example.emo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.emo.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private NavController navController;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (FirebaseAuth.getInstance().getCurrentUser()==null){
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        }

        try {
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            
            Log.d(TAG, "Инициализация MainActivity");
            
            navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
            
            // Заменяем setupActionBarWithNavController на простую настройку AppBarConfiguration
            AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.FirstFragment, R.id.TestsFragment, R.id.AiPsychologistFragment)
                    .build();
            
            // Настраиваем нижнюю навигацию
            BottomNavigationView bottomNavigationView = binding.bottomNavigation;
            
            // Проверяем, что bottomNavigationView не null
            if (bottomNavigationView == null) {
                Log.e(TAG, "bottomNavigationView is null!");
            } else {
                Log.d(TAG, "bottomNavigationView найден, настраиваем навигацию");
                
                // Добавляем явный обработчик нажатий
                bottomNavigationView.setOnItemSelectedListener(item -> {
                    int id = item.getItemId();
                    Log.d(TAG, "Нажат элемент меню с id: " + id);
                    
                    try {
                        if (id == R.id.FirstFragment) {
                            Log.d(TAG, "Переход к FirstFragment");
                            navController.navigate(R.id.FirstFragment);
                            return true;
                        } else if (id == R.id.TestsFragment) {
                            Log.d(TAG, "Переход к TestsFragment");
                            navController.navigate(R.id.TestsFragment);
                            return true;
                        } else if (id == R.id.AiPsychologistFragment) {
                            Log.d(TAG, "Переход к AiPsychologistFragment");
                            navController.navigate(R.id.AiPsychologistFragment);
                            return true;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при навигации: " + e.getMessage());
                        Toast.makeText(MainActivity.this, "Ошибка навигации: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    
                    return false;
                });
            }
            
            // Добавляем логирование для отслеживания навигации
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
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
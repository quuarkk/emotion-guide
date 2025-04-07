package com.example.emo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class AboutActivity extends AppCompatActivity {
    private DrawerLayout drawer;
    private static final String TAG = "AboutActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // Проверка аутентификации
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(AboutActivity.this, LoginActivity.class));
            finish();
            return;
        }

        try {
            // Настройка тулбара
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }

            // Настройка бокового меню
            drawer = findViewById(R.id.drawer_layout);
            NavigationView navigationView = findViewById(R.id.nav_view);

            // Обработчик выбора пунктов меню
            navigationView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                Log.d(TAG, "Нажат элемент меню с id: " + id);

                try {
                    if (id == R.id.ProfileActivity) {
                        startActivity(new Intent(AboutActivity.this, ProfileActivity.class));
                    } else if (id == R.id.FirstFragment ||
                            id == R.id.TestsFragment ||
                            id == R.id.AiPsychologistFragment ||
                            id == R.id.ChartsFragment) {
                        Intent intent = new Intent(AboutActivity.this, MainActivity.class);
                        intent.putExtra("fragment_id", id);
                        startActivity(intent);
                    } else if (id == R.id.AboutActivity) {
                        // Уже находимся в AboutActivity, ничего не делаем
                    } else if (id == R.id.action_logout) {
                        FirebaseAuth.getInstance().signOut();
                        startActivity(new Intent(AboutActivity.this, LoginActivity.class));
                        finish();
                        return true;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при навигации: " + e.getMessage());
                    return false;
                }

                drawer.closeDrawer(GravityCompat.START);
                return true;
            });

            // Настройка WebView
            WebView webView = findViewById(R.id.feedbackForm);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setDomStorageEnabled(true);
            webView.setWebViewClient(new WebViewClient());
            webView.loadUrl("https://forms.yandex.ru/u/67f309bc90fa7b089baaf8dd/?iframe=1");

        } catch (Exception e) {
            Log.e(TAG, "Ошибка в onCreate: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            drawer.openDrawer(GravityCompat.START);
        }
        return true;
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
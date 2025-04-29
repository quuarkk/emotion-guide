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
import android.content.res.Configuration;

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
                getSupportActionBar().setTitle("О нас");
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
                        finish();
                        return true;
                    } else if (id == R.id.FirstFragment ||
                            id == R.id.TestsFragment ||
                            id == R.id.AiPsychologistFragment ||
                            id == R.id.RelaxationFragment ||
                            id == R.id.ChartsFragment) {
                        Intent intent = new Intent(AboutActivity.this, MainActivity.class);
                        intent.putExtra("fragment_id", id);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                        return true;
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
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    // Проверяем текущую тему
                    int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                    boolean isDarkTheme = nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
                    
                    // JavaScript для изменения цветов формы
                    String jsCode = "javascript:(function() { " +
                        "var style = document.createElement('style'); " +
                        "style.type = 'text/css'; " +
                        "style.innerHTML = '" +
                        (isDarkTheme ? 
                            "body, input, textarea, select, .text-black { background-color: #121212 !important; color: #FFFFFF !important; } " +
                            "input, textarea, select { background-color: #1E1E1E !important; color: #FFFFFF !important; border-color: #333333 !important; } " +
                            "button { background-color: #BB86FC !important; color: #FFFFFF !important; } " +
                            "* { color: #FFFFFF !important; } " +
                            ".footer__container { background: transparent !important; } " +
                            ".footer { background: transparent !important; }" :
                            "body { background-color: #FFFFFF !important; color: #000000 !important; } " +
                            "input, textarea, select { background-color: #F5F5F5 !important; color: #000000 !important; border-color: #CCCCCC !important; } " +
                            "button { background-color: #6200EE !important; color: #FFFFFF !important; } " +
                            ".footer__container { background: transparent !important; } " +
                            ".footer { background: transparent !important; }") +
                        "'; " +
                        "document.head.appendChild(style);" +
                        "})()";
                    view.evaluateJavascript(jsCode, null);
                }
            });
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
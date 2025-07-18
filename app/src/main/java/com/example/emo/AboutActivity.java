package com.example.emo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.firebase.auth.FirebaseAuth;
import android.content.res.Configuration;

public class AboutActivity extends AppCompatActivity {
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

            // Настройка WebView
            WebView webView = findViewById(R.id.feedbackForm);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setDomStorageEnabled(true);
            webView.loadUrl("https://forms.yandex.ru/u/67f309bc90fa7b089baaf8dd/?iframe=1");

        } catch (Exception e) {
            Log.e(TAG, "Ошибка в onCreate: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
package com.example.emo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.animation.DecelerateInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.example.emo.firebase.FirebaseDataManager; // Добавляем импорт

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 800; // 3 секунды для основного экрана
    private static final int LOGO_ANIMATION_DURATION = 500; // 0.8 секунды для анимации лого
    private static final int AUTO_PROCEED_DELAY = 800; // 3 секунды до автоматического перехода
    
    private ImageView logoImageView;
    private TextView appTitleView;
    private View rootView;
    
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean hasNavigated = false; // Флаг для предотвращения повторных переходов

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Инициализируем FirebaseDataManager здесь, чтобы гарантировать его готовность
        FirebaseDataManager.setApplicationContext(getApplicationContext());
        FirebaseDataManager.initialize();

        // Устанавливаем полноэкранный режим
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
        
        setContentView(R.layout.activity_splash);
        
        logoImageView = findViewById(R.id.splash_image);
        appTitleView = findViewById(R.id.app_title);
        rootView = findViewById(android.R.id.content);
        logoImageView.setImageResource(R.drawable.icon_apps);
        
        // Запускаем анимацию заставки
        startSplashAnimation();
    }
    
    private void startSplashAnimation() {
        // Сначала устанавливаем текст и лого полностью прозрачными
        appTitleView.setAlpha(0f);
        logoImageView.setAlpha(0f);
        
        // Анимация появления логотипа
        ObjectAnimator logoFadeIn = ObjectAnimator.ofFloat(logoImageView, "alpha", 0f, 1f);
        logoFadeIn.setDuration(LOGO_ANIMATION_DURATION);
        logoFadeIn.start();
        
        // После паузы запускаем анимацию появления текста
        handler.postDelayed(() -> {
            // Одновременно с появлением логотипа начинаем медленно проявлять текст
            ValueAnimator textFadeIn = ValueAnimator.ofFloat(0f, 1f);
            textFadeIn.setDuration(LOGO_ANIMATION_DURATION);
            textFadeIn.addUpdateListener(animation -> {
                float alpha = (float) animation.getAnimatedValue();
                appTitleView.setAlpha(alpha);
            });
            textFadeIn.start();
            
            // Добавляем возможность перейти дальше по клику после завершения анимации
            handler.postDelayed(() -> {
                rootView.setOnClickListener(v -> navigateToNextScreen());
                
                // Автоматический переход через заданное время
                handler.postDelayed(() -> {
                    if (!isFinishing()) {
                        navigateToNextScreen();
                    }
                }, AUTO_PROCEED_DELAY);
                
            }, LOGO_ANIMATION_DURATION);
            
        }, LOGO_ANIMATION_DURATION + 300); // Небольшая пауза после появления логотипа
    }
    
    // Вспомогательный метод для конвертации dp в пиксели
    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
    
    private void navigateToNextScreen() {
        // Предотвращаем повторный вызов, если уже осуществлен переход
        if (hasNavigated) {
            return;
        }
        hasNavigated = true;
        
        // Проверяем авторизацию пользователя
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            // Если пользователь авторизован, переходим на главный экран
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
        } else {
            // Если пользователь не авторизован, показываем онбординг независимо от того, видел ли он его ранее
            startActivity(new Intent(SplashActivity.this, OnboardingActivity.class));
        }
        
        finish();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Очищаем обработчик при уничтожении активности
        handler.removeCallbacksAndMessages(null);
    }
} 
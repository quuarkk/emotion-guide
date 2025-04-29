package com.example.emo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;

public class OnboardingActivity extends AppCompatActivity {

    private OnboardingAdapter onboardingAdapter;
    private LinearLayout layoutOnboardingIndicators;
    private MaterialButton buttonOnboardingAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Проверяем, авторизован ли пользователь
        if (FirebaseAuth.getInstance().getCurrentUser() != null && restorePrefData()) {
            // Если пользователь авторизован и уже видел онбординг, переходим на главный экран
            startActivity(new Intent(OnboardingActivity.this, MainActivity.class));
            finish();
            return;
        }
        
        setContentView(R.layout.activity_onboarding);

        layoutOnboardingIndicators = findViewById(R.id.layoutOnboardingIndicators);
        buttonOnboardingAction = findViewById(R.id.buttonOnboardingAction);

        setupOnboardingItems();

        ViewPager2 onboardingViewPager = findViewById(R.id.onboardingViewPager);
        onboardingViewPager.setAdapter(onboardingAdapter);

        setupOnboardingIndicators();
        setCurrentOnboardingIndicator(0);

        onboardingViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                setCurrentOnboardingIndicator(position);
            }
        });

        // Вызываем метод для добавления эффекта свечения
        addShineEffect();
        
        // Добавляем анимацию нажатия кнопки
        buttonOnboardingAction.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    buttonOnboardingAction.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).start();
                } else if (event.getAction() == MotionEvent.ACTION_UP || 
                           event.getAction() == MotionEvent.ACTION_CANCEL) {
                    buttonOnboardingAction.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                }
                return false;
            }
        });

        buttonOnboardingAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onboardingViewPager.getCurrentItem() + 1 < onboardingAdapter.getItemCount()) {
                    onboardingViewPager.setCurrentItem(onboardingViewPager.getCurrentItem() + 1);
                } else {
                    // Сохраняем, что онбординг был показан
                    savePrefsData();
                    // Переходим на экран авторизации
                    startActivity(new Intent(OnboardingActivity.this, AuthActivity.class));
                    finish();
                }
            }
        });
    }

    private void setupOnboardingItems() {
        List<OnboardingItem> onboardingItems = new ArrayList<>();

        OnboardingItem moodTracking = new OnboardingItem();
        moodTracking.setTitle("Отслеживайте свое состояние");
        moodTracking.setSubtitle("Узнай себя лучше отслеживая свое ежедневное состояние. Это приложение поможет следить за своим ментальным здоровьем и улучшать его в случае ухудшения");
        moodTracking.setImage(R.drawable.girl);

        OnboardingItem meditation = new OnboardingItem();
        meditation.setTitle("Индивидуальные рекомендации для каждого");
        meditation.setSubtitle("На основе предоставленных ответов из тестов, наш ИИ-психолог анализирует показатели, показывает динамику изменения состояния, дает рекомендации");
        meditation.setImage(R.drawable.man);

        onboardingItems.add(moodTracking);
        onboardingItems.add(meditation);

        onboardingAdapter = new OnboardingAdapter(onboardingItems);
    }

    private void setupOnboardingIndicators() {
        ImageView[] indicators = new ImageView[onboardingAdapter.getItemCount()];
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(8, 0, 8, 0);

        for (int i = 0; i < indicators.length; i++) {
            indicators[i] = new ImageView(getApplicationContext());
            indicators[i].setImageDrawable(ContextCompat.getDrawable(
                    getApplicationContext(),
                    R.drawable.onboarding_indicator_inactive
            ));
            indicators[i].setLayoutParams(layoutParams);
            layoutOnboardingIndicators.addView(indicators[i]);
        }
    }

    private void setCurrentOnboardingIndicator(int index) {
        int childCount = layoutOnboardingIndicators.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ImageView imageView = (ImageView) layoutOnboardingIndicators.getChildAt(i);
            if (i == index) {
                imageView.setImageDrawable(
                        ContextCompat.getDrawable(getApplicationContext(), R.drawable.onboarding_indicator_active)
                );
            } else {
                imageView.setImageDrawable(
                        ContextCompat.getDrawable(getApplicationContext(), R.drawable.onboarding_indicator_inactive)
                );
            }
        }

        // Для последнего экрана меняем иконку на кнопке
        if (index == onboardingAdapter.getItemCount() - 1) {
            buttonOnboardingAction.setIconResource(android.R.drawable.ic_media_play);
        } else {
            buttonOnboardingAction.setIconResource(android.R.drawable.ic_media_play);
        }
    }
    
    private boolean restorePrefData() {
        SharedPreferences pref = getApplicationContext().getSharedPreferences("onboardingPrefs", MODE_PRIVATE);
        return pref.getBoolean("isOnboardingShown", false);
    }

    private void savePrefsData() {
        SharedPreferences pref = getApplicationContext().getSharedPreferences("onboardingPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("isOnboardingShown", true);
        editor.apply();
    }

    private void addShineEffect() {
        // Получаем размеры кнопки после отрисовки
        buttonOnboardingAction.post(new Runnable() {
            @Override
            public void run() {
                // Создаем анимацию пульсации для кнопки
                View shineEffect = new View(OnboardingActivity.this);
                shineEffect.setId(View.generateViewId());
                ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                        buttonOnboardingAction.getWidth() + 40,
                        buttonOnboardingAction.getHeight() + 40
                );
                
                shineEffect.setLayoutParams(params);
                shineEffect.setBackground(ContextCompat.getDrawable(OnboardingActivity.this, R.drawable.onboarding_button_shine));
                shineEffect.setAlpha(0.7f);
                
                ConstraintLayout parent = findViewById(R.id.onboarding_container);
                parent.addView(shineEffect, 0);
                
                // Позиционируем эффект свечения под кнопкой
                ConstraintSet constraintSet = new ConstraintSet();
                constraintSet.clone(parent);
                constraintSet.connect(shineEffect.getId(), ConstraintSet.START, buttonOnboardingAction.getId(), ConstraintSet.START);
                constraintSet.connect(shineEffect.getId(), ConstraintSet.END, buttonOnboardingAction.getId(), ConstraintSet.END);
                constraintSet.connect(shineEffect.getId(), ConstraintSet.TOP, buttonOnboardingAction.getId(), ConstraintSet.TOP);
                constraintSet.connect(shineEffect.getId(), ConstraintSet.BOTTOM, buttonOnboardingAction.getId(), ConstraintSet.BOTTOM);
                constraintSet.applyTo(parent);
                
                // Анимация пульсации
                ObjectAnimator scaleX = ObjectAnimator.ofFloat(shineEffect, "scaleX", 1f, 1.2f, 1f);
                scaleX.setRepeatMode(ValueAnimator.RESTART);
                scaleX.setRepeatCount(ValueAnimator.INFINITE);
                
                ObjectAnimator scaleY = ObjectAnimator.ofFloat(shineEffect, "scaleY", 1f, 1.2f, 1f);
                scaleY.setRepeatMode(ValueAnimator.RESTART);
                scaleY.setRepeatCount(ValueAnimator.INFINITE);
                
                ObjectAnimator alpha = ObjectAnimator.ofFloat(shineEffect, "alpha", 0.7f, 0.2f, 0.7f);
                alpha.setRepeatMode(ValueAnimator.RESTART);
                alpha.setRepeatCount(ValueAnimator.INFINITE);
                
                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playTogether(scaleX, scaleY, alpha);
                animatorSet.setDuration(2000);
                animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
                animatorSet.start();
            }
        });
    }
} 
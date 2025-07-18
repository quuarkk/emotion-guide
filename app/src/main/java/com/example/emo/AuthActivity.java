package com.example.emo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.view.ViewGroup;
import android.os.Build;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.emo.databinding.ActivityAuthBinding;
import com.example.emo.fragments.LoginFragment;
import com.example.emo.fragments.RegisterFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;

public class AuthActivity extends AppCompatActivity {
    
    private static final String TAG = "AuthActivity";
    private ActivityAuthBinding binding;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Если пользователь уже авторизован, перенаправляем на главный экран
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            startActivity(new Intent(AuthActivity.this, MainActivity.class));
            finish();
            return;
        }
        
        binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        viewPager = binding.viewPager;
        tabLayout = binding.tabLayout;
        
        // Настраиваем ViewPager с адаптером
        setupViewPager();
        
        // Связываем TabLayout с ViewPager
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Вход");
                    break;
                case 1:
                    tab.setText("Регистрация");
                    break;
            }
        }).attach();
        
        // Добавляем слушатель для изменения высоты ViewPager2 при смене страниц
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                setDynamicHeight(position);
            }
        });

        // Устанавливаем начальную высоту
        viewPager.post(() -> setDynamicHeight(viewPager.getCurrentItem()));
        
        // Настраиваем слушатель состояния авторизации
        FirebaseAuth.getInstance().addAuthStateListener(firebaseAuth -> {
            if (firebaseAuth.getCurrentUser() != null) {
                // Если пользователь авторизовался, перенаправляем на главный экран
                startActivity(new Intent(AuthActivity.this, MainActivity.class));
                finish();
            }
        });
    }
    
    private void setupViewPager() {
        AuthPagerAdapter pagerAdapter = new AuthPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        
        // Запрещаем свайп между фрагментами (навигация только через табы)
        // viewPager.setUserInputEnabled(false);
    }

    private void setDynamicHeight(int position) {
        // Получаем текущий фрагмент
        Fragment fragment = getSupportFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());
        if (fragment != null && fragment.getView() != null) {
            fragment.getView().post(() -> {
                int height = fragment.getView().getHeight();
                if (height == 0) {
                    // Если высота 0, ждем, пока View будет отрисован
                    fragment.getView().getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            // Удаляем слушатель, чтобы избежать повторных вызовов
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                fragment.getView().getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            } else {
                                fragment.getView().getViewTreeObserver().removeGlobalOnLayoutListener(this);
                            }
                            // Устанавливаем высоту ViewPager2
                            ViewGroup.LayoutParams layoutParams = viewPager.getLayoutParams();
                            layoutParams.height = fragment.getView().getHeight();
                            viewPager.setLayoutParams(layoutParams);
                            Log.d(TAG, "Dynamic height set (onGlobalLayout): " + layoutParams.height);
                        }
                    });
                } else {
                    // Если высота уже есть, устанавливаем ее сразу
                    ViewGroup.LayoutParams layoutParams = viewPager.getLayoutParams();
                    layoutParams.height = height;
                    viewPager.setLayoutParams(layoutParams);
                    Log.d(TAG, "Dynamic height set (initial): " + layoutParams.height);
                }
            });
        } else {
            Log.e(TAG, "Fragment or fragment view is null for position: " + position);
        }
    }
    
    private static class AuthPagerAdapter extends FragmentStateAdapter {
        
        public AuthPagerAdapter(FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }
        
        @Override
        public int getItemCount() {
            return 2; // Два фрагмента: вход и регистрация
        }
        
        @Override
        public Fragment createFragment(int position) {
            // Возвращаем нужный фрагмент в зависимости от позиции
            switch (position) {
                case 0:
                    return new LoginFragment();
                case 1:
                    return new RegisterFragment();
                default:
                    return new LoginFragment(); // По умолчанию возвращаем фрагмент входа
            }
        }
    }
} 
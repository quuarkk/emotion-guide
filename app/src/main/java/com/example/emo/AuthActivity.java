package com.example.emo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

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
package com.example.emo;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.emo.databinding.ActivityLoginBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Устанавливаем фильтр для всех полей EditText, чтобы запретить перенос строки
        InputFilter noNewLineFilter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                // Проверяем каждый вводимый символ
                for (int i = start; i < end; i++) {
                    if (source.charAt(i) == '\n') {
                        // Если найден символ переноса строки, возвращаем пустую строку (игнорируем его)
                        return "";
                    }
                }
                return null; // Разрешаем ввод
            }
        };

        // Применяем фильтр ко всем полям
        binding.emailEt.setFilters(new InputFilter[]{noNewLineFilter});
        binding.passwordEt.setFilters(new InputFilter[]{noNewLineFilter});

        // Добавляем обработчик для кнопки "Готово" на клавиатуре (для поля пароля)
        binding.passwordEt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.loginBtn.performClick(); // Симулируем клик по кнопке входа
                return true;
            }
            return false;
        });

        binding.loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = binding.emailEt.getText().toString().trim();
                String password = binding.passwordEt.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Поля не могут быть пустыми", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(getApplicationContext(), "Введите корректный email", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.length() < 6) {
                    Toast.makeText(getApplicationContext(), "Пароль должен содержать минимум 6 символов", Toast.LENGTH_SHORT).show();
                    return;
                }

                binding.progressBar.setVisibility(View.VISIBLE);
                binding.loginBtn.setEnabled(false);

                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                binding.progressBar.setVisibility(View.GONE);
                                binding.loginBtn.setEnabled(true);
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "Вход успешен, переход в MainActivity");
                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    finish();
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            binding.progressBar.setVisibility(View.GONE);
                            binding.loginBtn.setEnabled(true);
                            Log.e(TAG, "Ошибка входа: " + e.getMessage());
                            Toast.makeText(getApplicationContext(), "Ошибка входа: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            }
        });

        binding.forgotPasswordTv.setOnClickListener(v -> {
            String email = binding.emailEt.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(getApplicationContext(), "Введите ваш email", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(getApplicationContext(), "Введите корректный email", Toast.LENGTH_SHORT).show();
                return;
            }

            binding.progressBar.setVisibility(View.VISIBLE);
            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        binding.progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Письмо для сброса пароля отправлено");
                            Toast.makeText(getApplicationContext(), "Письмо для сброса пароля отправлено", Toast.LENGTH_LONG).show();
                        } else {
                            Log.e(TAG, "Ошибка отправки письма: " + task.getException().getMessage());
                            Toast.makeText(getApplicationContext(), "Ошибка отправки письма: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        binding.goToRegisterActivityTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });
    }
}
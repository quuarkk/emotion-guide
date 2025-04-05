package com.example.emo;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.emo.databinding.ActivityRegisterBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private static final String TAG = "RegisterActivity";
    private boolean isRegistering = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Проверка, если пользователь уже авторизован
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            Log.d(TAG, "Пользователь уже авторизован, перенаправление на MainActivity");
            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
            finish(); // Закрываем RegisterActivity
            return; // Прекращаем выполнение onCreate
        }

        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ImageButton backButton = findViewById(R.id.back_btn);
        CheckBox termsCheckbox = findViewById(R.id.terms_checkbox);

        // Настройка кликабельного текста "Пользовательское соглашение"
        TextView termsText = findViewById(R.id.terms_text);
        if (termsText == null) {
            Log.e(TAG, "terms_text не найден в макете!");
            Toast.makeText(this, "Ошибка: terms_text не найден", Toast.LENGTH_LONG).show();
            return;
        }

        // Используем SpannableString для кликабельной ссылки
        String fullText = getString(R.string.terms_accept).replaceAll("<[^>]+>", ""); // Удаляем HTML-теги
        SpannableString spannableString = new SpannableString(fullText);

        String linkText = "Пользовательского соглашения";
        int start = fullText.indexOf(linkText);
        int end = start + linkText.length();

        if (start == -1) {
            Log.e(TAG, "Текст 'Пользовательского соглашения' не найден в строке: " + fullText);
            Toast.makeText(this, "Ошибка отображения условий", Toast.LENGTH_SHORT).show();
            return;
        }

        // Делаем текст кликабельным
        spannableString.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                try {
                    Log.d(TAG, "Попытка открыть TermsActivity");
                    startActivity(new Intent(RegisterActivity.this, TermsActivity.class));
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при открытии TermsActivity: " + e.getMessage(), e);
                    Toast.makeText(RegisterActivity.this, "Не удалось открыть соглашение: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Добавляем подчеркивание
        spannableString.setSpan(new UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // (Опционально) Изменяем цвет ссылки
        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(android.R.color.holo_blue_light)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        termsText.setText(spannableString);
        termsText.setMovementMethod(LinkMovementMethod.getInstance());
        termsText.setHighlightColor(android.graphics.Color.TRANSPARENT);

        // Устанавливаем фильтр для всех полей EditText
        InputFilter noNewLineFilter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    if (source.charAt(i) == '\n') {
                        return "";
                    }
                }
                return null;
            }
        };

        binding.emailEt.setFilters(new InputFilter[]{noNewLineFilter});
        binding.passwordEt.setFilters(new InputFilter[]{noNewLineFilter});
        binding.confirmPasswordEt.setFilters(new InputFilter[]{noNewLineFilter});
        binding.usernameEt.setFilters(new InputFilter[]{noNewLineFilter});

        backButton.setOnClickListener(v -> finish());

        binding.signUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRegistering) {
                    Log.d(TAG, "Регистрация уже выполняется, игнорируем повторный клик");
                    return;
                }

                String email = binding.emailEt.getText().toString().trim();
                String password = binding.passwordEt.getText().toString().trim();
                String confirmPassword = binding.confirmPasswordEt.getText().toString().trim();
                String username = binding.usernameEt.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty() || username.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Поля не могут быть пустыми", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(getApplicationContext(), "Введите корректный email", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.length() < 6) {
                    Toast.makeText(getApplicationContext(), " Пароль должен содержать минимум 6 символов", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    Toast.makeText(getApplicationContext(), "Пароли не совпадают", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (username.length() < 3) {
                    Toast.makeText(getApplicationContext(), "Имя пользователя должно содержать минимум 3 символа", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!isNetworkAvailable()) {
                    Toast.makeText(getApplicationContext(), "Нет подключения к интернету", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!termsCheckbox.isChecked()) {
                    Toast.makeText(getApplicationContext(), "Вы должны принять условия Пользовательского соглашения", Toast.LENGTH_SHORT).show();
                    return;
                }

                Log.d(TAG, "Начало процесса регистрации");
                isRegistering = true;
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.signUpBtn.setEnabled(false);

                Handler handler = new Handler(Looper.getMainLooper());
                Runnable timeoutRunnable = () -> {
                    Log.w(TAG, "Тайм-аут: запрос к Firebase не завершился вовремя");
                    isRegistering = false;
                    binding.progressBar.setVisibility(View.GONE);
                    binding.signUpBtn.setEnabled(true);
                    Toast.makeText(getApplicationContext(), "Время ожидания истекло. Проверьте подключение и попробуйте снова.", Toast.LENGTH_LONG).show();
                };
                handler.postDelayed(timeoutRunnable, 10000);

                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                Log.d(TAG, "createUserWithEmailAndPassword completed: " + task.isSuccessful());
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "Регистрация успешна, сохранение данных в FirebaseDatabase");
                                    HashMap<String, Object> userInfo = new HashMap<>(); // Изменяем тип на Object для гибкости
                                    userInfo.put("email", email);
                                    userInfo.put("username", username);
                                    userInfo.put("termsAccepted", termsCheckbox.isChecked()); // Добавляем состояние чекбокса

                                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                    Log.d(TAG, "User ID: " + userId);

                                    FirebaseDatabase database = FirebaseDatabase.getInstance("https://emotions-guide-c173c-default-rtdb.europe-west1.firebasedatabase.app/");
                                    DatabaseReference userRef = database.getReference().child("Users").child(userId);

                                    userRef.setValue(userInfo)
                                            .addOnSuccessListener(aVoid -> {
                                                handler.removeCallbacks(timeoutRunnable);
                                                Log.d(TAG, "setValue: данные успешно сохранены");
                                                binding.progressBar.setVisibility(View.GONE);
                                                binding.signUpBtn.setEnabled(true);
                                                isRegistering = false;
                                                Toast.makeText(getApplicationContext(), "Регистрация успешна!", Toast.LENGTH_SHORT).show();
                                                startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                handler.removeCallbacks(timeoutRunnable);
                                                Log.e(TAG, "setValue failed: " + e.getMessage());
                                                binding.progressBar.setVisibility(View.GONE);
                                                binding.signUpBtn.setEnabled(true);
                                                isRegistering = false;
                                                Toast.makeText(getApplicationContext(), "Ошибка сохранения данных: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                            });
                                } else {
                                    handler.removeCallbacks(timeoutRunnable);
                                    Log.e(TAG, "Регистрация не удалась: " + task.getException().getMessage());
                                    binding.progressBar.setVisibility(View.GONE);
                                    binding.signUpBtn.setEnabled(true);
                                    isRegistering = false;
                                    Toast.makeText(getApplicationContext(), "Ошибка регистрации: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            handler.removeCallbacks(timeoutRunnable);
                            Log.e(TAG, "createUserWithEmailAndPassword failed: " + e.getMessage());
                            binding.progressBar.setVisibility(View.GONE);
                            binding.signUpBtn.setEnabled(true);
                            isRegistering = false;
                            Toast.makeText(getApplicationContext(), "Ошибка регистрации: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            }
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
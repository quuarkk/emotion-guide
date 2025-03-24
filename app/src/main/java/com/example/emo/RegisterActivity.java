package com.example.emo;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
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
    private boolean isRegistering = false; // Флаг для предотвращения повторных вызовов

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ImageButton backButton = findViewById(R.id.back_btn);

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
        binding.confirmPasswordEt.setFilters(new InputFilter[]{noNewLineFilter});
        binding.usernameEt.setFilters(new InputFilter[]{noNewLineFilter});

        // Устанавливаем обработчик клика для кнопки "Назад"
        backButton.setOnClickListener(v -> {
            finish(); // Закрывает текущую активность и возвращает на предыдущий экран
        });

        binding.signUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Проверяем, не выполняется ли уже регистрация
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
                    Toast.makeText(getApplicationContext(), "Пароль должен содержать минимум 6 символов", Toast.LENGTH_SHORT).show();
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

                Log.d(TAG, "Начало процесса регистрации");
                isRegistering = true; // Устанавливаем флаг
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.signUpBtn.setEnabled(false);

                // Устанавливаем тайм-аут на 10 секунд
                Handler handler = new Handler(Looper.getMainLooper());
                Runnable timeoutRunnable = () -> {
                    Log.w(TAG, "Тайм-аут: запрос к Firebase не завершился вовремя");
                    isRegistering = false; // Сбрасываем флаг
                    binding.progressBar.setVisibility(View.GONE);
                    binding.signUpBtn.setEnabled(true);
                    Toast.makeText(getApplicationContext(), "Время ожидания истекло. Проверьте подключение и попробуйте снова.", Toast.LENGTH_LONG).show();
                };
                handler.postDelayed(timeoutRunnable, 10000); // 10 секунд

                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                Log.d(TAG, "createUserWithEmailAndPassword completed: " + task.isSuccessful());
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "Регистрация успешна, сохранение данных в FirebaseDatabase");
                                    HashMap<String, String> userInfo = new HashMap<>();
                                    userInfo.put("email", email);
                                    userInfo.put("username", username);
//                                    userInfo.put("profileImage", "");
//                                    userInfo.put("chats", "");

                                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                    Log.d(TAG, "User ID: " + userId);

                                    FirebaseDatabase database = FirebaseDatabase.getInstance("https://emotions-guide-c173c-default-rtdb.europe-west1.firebasedatabase.app/");
                                    Log.d(TAG, "Database instance created: " + database.toString());

                                    DatabaseReference userRef = database.getReference().child("Users").child(userId);
                                    Log.d(TAG, "Database reference: " + userRef.toString());

                                    Log.d(TAG, "User info to save: " + userInfo.toString());
                                    Log.d(TAG, "Attempting to save data to FirebaseDatabase");

                                    userRef.setValue(userInfo)
                                            .addOnSuccessListener(aVoid -> {
                                                handler.removeCallbacks(timeoutRunnable); // Отменяем тайм-аут
                                                Log.d(TAG, "setValue: данные успешно сохранены");
                                                binding.progressBar.setVisibility(View.GONE);
                                                binding.signUpBtn.setEnabled(true);
                                                isRegistering = false; // Сбрасываем флаг
                                                Toast.makeText(getApplicationContext(), "Регистрация успешна!", Toast.LENGTH_SHORT).show();
                                                startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                handler.removeCallbacks(timeoutRunnable); // Отменяем тайм-аут
                                                Log.e(TAG, "setValue failed: " + e.getMessage());
                                                binding.progressBar.setVisibility(View.GONE);
                                                binding.signUpBtn.setEnabled(true);
                                                isRegistering = false; // Сбрасываем флаг
                                                Toast.makeText(getApplicationContext(), "Ошибка сохранения данных: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                            })
                                            .addOnCompleteListener(task1 -> {
                                                Log.d(TAG, "setValue completed: " + task1.isSuccessful());
                                                if (!task1.isSuccessful()) {
                                                    Log.e(TAG, "setValue failed with exception: " + task1.getException());
                                                }
                                            });
                                } else {
                                    handler.removeCallbacks(timeoutRunnable); // Отменяем тайм-аут
                                    Log.e(TAG, "Регистрация не удалась: " + task.getException().getMessage());
                                    binding.progressBar.setVisibility(View.GONE);
                                    binding.signUpBtn.setEnabled(true);
                                    isRegistering = false; // Сбрасываем флаг
                                    Toast.makeText(getApplicationContext(), "Ошибка регистрации: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            handler.removeCallbacks(timeoutRunnable); // Отменяем тайм-аут
                            Log.e(TAG, "createUserWithEmailAndPassword failed: " + e.getMessage());
                            binding.progressBar.setVisibility(View.GONE);
                            binding.signUpBtn.setEnabled(true);
                            isRegistering = false; // Сбрасываем флаг
                            Toast.makeText(getApplicationContext(), "Ошибка регистрации: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            }
        });
    }

    // Метод для проверки подключения к интернету
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
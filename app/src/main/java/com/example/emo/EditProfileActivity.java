package com.example.emo;

import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.example.emo.firebase.FirebaseDataManager;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";
    private EditText usernameEt, emailEt, passwordEt;
    private Button saveUsernameBtn, saveEmailBtn, changePasswordBtn;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Настройка Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Инициализация Firebase
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userRef = FirebaseDatabase.getInstance("https://emotions-guide-c173c-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("Users")
                .child(currentUser.getUid());

        // Инициализация UI элементов
        usernameEt = findViewById(R.id.username_et);
        emailEt = findViewById(R.id.email_et);
        passwordEt = findViewById(R.id.password_et);
        saveUsernameBtn = findViewById(R.id.save_username_btn);
        saveEmailBtn = findViewById(R.id.save_email_btn);
        changePasswordBtn = findViewById(R.id.change_password_btn);
        progressBar = findViewById(R.id.progressBar);

        // Фильтр для запрета переноса строки
        InputFilter noNewLineFilter = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                if (source.charAt(i) == '\n') {
                    return "";
                }
            }
            return null;
        };
        usernameEt.setFilters(new InputFilter[]{noNewLineFilter});
        emailEt.setFilters(new InputFilter[]{noNewLineFilter});
        passwordEt.setFilters(new InputFilter[]{noNewLineFilter});

        // Загрузка текущих данных
        loadUserData();

        // Обработчики кнопок
        saveUsernameBtn.setOnClickListener(v -> saveUsername());
        saveEmailBtn.setOnClickListener(v -> saveEmail());
        changePasswordBtn.setOnClickListener(v -> changePassword());
    }

    private void loadUserData() {
        showProgress(true);

        // Получение никнейма
        FirebaseDataManager.getUserName().thenAccept(username -> {
            usernameEt.setText(username);

            // Получение email
            emailEt.setText(currentUser.getEmail());

            showProgress(false);
        }).exceptionally(throwable -> {
            Log.e(TAG, "Ошибка загрузки данных: " + throwable.getMessage());
            Toast.makeText(this, "Ошибка загрузки данных: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
            showProgress(false);
            return null;
        });
    }

    private void saveUsername() {
        String newUsername = usernameEt.getText().toString().trim();
        if (newUsername.length() < 3) {
            Toast.makeText(this, "Никнейм должен содержать минимум 3 символа", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress(true);
        userRef.child("username").setValue(newUsername)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Никнейм успешно обновлен", Toast.LENGTH_SHORT).show();
                    showProgress(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Ошибка обновления никнейма: " + e.getMessage());
                    Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    showProgress(false);
                });
    }

    private void saveEmail() {
        String newEmail = emailEt.getText().toString().trim();
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            Toast.makeText(this, "Введите корректный email", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress(true);
        currentUser.verifyBeforeUpdateEmail(newEmail)
                .addOnSuccessListener(aVoid -> {
                    userRef.child("email").setValue(newEmail)
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(this, "Проверьте почту для подтверждения нового email", Toast.LENGTH_LONG).show();
                                showProgress(false);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Ошибка обновления email в базе данных: " + e.getMessage());
                                Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                showProgress(false);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Ошибка обновления email: " + e.getMessage());
                    Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    showProgress(false);
                });
    }

    private void changePassword() {
        String newPassword = passwordEt.getText().toString().trim();
        if (newPassword.length() < 6) {
            Toast.makeText(this, "Пароль должен содержать минимум 6 символов", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress(true);
        currentUser.updatePassword(newPassword)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Пароль успешно обновлен", Toast.LENGTH_SHORT).show();
                    passwordEt.setText("");
                    showProgress(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Ошибка обновления пароля: " + e.getMessage());
                    Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    showProgress(false);
                });
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        saveUsernameBtn.setEnabled(!show);
        saveEmailBtn.setEnabled(!show);
        changePasswordBtn.setEnabled(!show);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
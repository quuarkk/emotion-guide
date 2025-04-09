package com.example.emo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.example.emo.firebase.FirebaseDataManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private static final String PREFS_NAME = "user_prefs";
    private static final String KEY_USERNAME = "KEY_USERNAME";
    private TextView usernameTv, emailTv, registrationDateTv;
    private ImageButton editProfileBtn;
    private Button resetTestResultsBtn;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private DatabaseReference userRef;
    private boolean isLoading = false;
    private SharedPreferences sharedPreferences;
    private int retryCount = 0;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000; // 2 секунды задержки между попытками

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Инициализация SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Передаем контекст в FirebaseDataManager
        FirebaseDataManager.setApplicationContext(getApplicationContext());

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

        // Запускаем тестирование Firebase при инициализации
        FirebaseDataManager.testFirebaseConfiguration();

        userRef = FirebaseDatabase.getInstance("https://emotions-guide-c173c-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("Users")
                .child(currentUser.getUid());

        // Инициализация UI элементов
        usernameTv = findViewById(R.id.username_tv);
        emailTv = findViewById(R.id.email_tv);
        registrationDateTv = findViewById(R.id.registration_date_tv);
        editProfileBtn = findViewById(R.id.edit_profile_btn);
        resetTestResultsBtn = findViewById(R.id.reset_test_results_btn);
        progressBar = findViewById(R.id.progressBar);

        // Установка кэшированных данных (если есть)
        String cachedUsername = sharedPreferences.getString(KEY_USERNAME, null);
        if (cachedUsername != null) {
            usernameTv.setText(cachedUsername);
        }

        // Обработчики кнопок
        editProfileBtn.setOnClickListener(v -> {
            Log.d(TAG, "Нажата кнопка редактирования профиля");
            startActivity(new Intent(ProfileActivity.this, EditProfileActivity.class));
        });

        resetTestResultsBtn.setOnClickListener(v -> {
            Log.d(TAG, "Нажата кнопка сброса результатов теста");
            resetTestResults();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadUserData();
    }

    private void loadUserData() {
        if (isLoading) {
            Log.d(TAG, "loadUserData: загрузка уже в процессе, пропускаем");
            return;
        }
        
        isLoading = true;
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        Log.d(TAG, "Начата загрузка данных пользователя");
        
        // Сначала загружаем кэшированные данные для быстрого отображения
        loadCachedData();
        
        // Проверяем соединение с интернетом
        if (!isNetworkAvailable()) {
            Log.w(TAG, "Нет подключения к интернету");
            Toast.makeText(this, "Нет подключения к интернету", Toast.LENGTH_SHORT).show();
            isLoading = false;
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            return;
        }
        
        // Проверяем авторизацию
        if (currentUser == null) {
            Log.w(TAG, "Пользователь не авторизован");
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            isLoading = false;
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            finish();
            return;
        }
        
        try {
            // Получаем дату регистрации
            try {
                if (currentUser.getMetadata() != null) {
                    long creationTimestamp = currentUser.getMetadata().getCreationTimestamp();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                    String registrationDate = sdf.format(new Date(creationTimestamp));
                    if (registrationDateTv != null) registrationDateTv.setText(registrationDate);
                    Log.d(TAG, "Дата регистрации: " + registrationDate);
                } else {
                    Log.w(TAG, "Метаданные пользователя не доступны");
                    if (registrationDateTv != null) registrationDateTv.setText("Не указана");
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка получения даты регистрации", e);
                if (registrationDateTv != null) registrationDateTv.setText("Не указана");
            }
            
            // Получаем email
            String email = currentUser.getEmail() != null ? currentUser.getEmail() : "Не указан";
            if (emailTv != null) emailTv.setText(email);
            Log.d(TAG, "Email пользователя: " + email);
            
            // Используем оба метода получения имени пользователя для надежности
            // 1. Прямое чтение из Firebase
            String userId = currentUser.getUid();
            FirebaseDataManager.readUsernameDirectly(userId, new FirebaseDataManager.OnUsernameLoadedListener() {
                @Override
                public void onUsernameLoaded(String username, boolean success) {
                    if (success) {
                        Log.d(TAG, "Имя пользователя получено напрямую: " + username);
                        runOnUiThread(() -> {
                            if (usernameTv != null) usernameTv.setText(username);
                            
                            // Сохраняем имя пользователя в SharedPreferences
                            sharedPreferences.edit().putString(KEY_USERNAME, username).apply();
                            
                            isLoading = false;
                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                        });
                    } else {
                        Log.w(TAG, "Не удалось получить имя пользователя напрямую, используем запасной метод");
                        // При неудаче используем стандартный метод
                        loadUsernameWithFallback();
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка загрузки данных пользователя", e);
            Toast.makeText(this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
            isLoading = false;
            if (progressBar != null) progressBar.setVisibility(View.GONE);
        }
    }

    private void loadCachedData() {
        String cachedUsername = sharedPreferences.getString(KEY_USERNAME, "Пользователь");
        usernameTv.setText(cachedUsername);
        Log.d(TAG, "Загружено кэшированное имя пользователя: " + cachedUsername);

        String email = currentUser != null ? currentUser.getEmail() : "Не указан";
        emailTv.setText(email);

        try {
            long creationTimestamp = currentUser != null ? currentUser.getMetadata().getCreationTimestamp() : 0;
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            String registrationDate = creationTimestamp != 0 ? sdf.format(new Date(creationTimestamp)) : "Не указана";
            registrationDateTv.setText(registrationDate);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка загрузки кэшированной даты регистрации", e);
            registrationDateTv.setText("Не указана");
        }
    }

    private void resetTestResults() {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Нет подключения к интернету", Toast.LENGTH_LONG).show();
            return;
        }

        showProgress(true);
        Log.d(TAG, "Начало сброса результатов теста");

        userRef.child("TestResults").removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Результаты теста успешно сброшены");
                    Toast.makeText(this, "Результаты теста САН успешно сброшены", Toast.LENGTH_SHORT).show();
                    showProgress(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Ошибка сброса результатов теста: " + e.getMessage());
                    Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    showProgress(false);
                });
    }

    private void showProgress(boolean show) {
        Log.d(TAG, "showProgress: " + show);
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (editProfileBtn != null) {
            editProfileBtn.setEnabled(!show);
        }
        if (resetTestResultsBtn != null) {
            resetTestResultsBtn.setEnabled(!show);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }
        
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        boolean isAvailable = activeNetworkInfo != null && activeNetworkInfo.isConnected();
        Log.d(TAG, "Проверка сети: " + (isAvailable ? "доступна" : "недоступна"));
        return isAvailable;
    }

    // Запасной метод загрузки имени пользователя
    private void loadUsernameWithFallback() {
        Log.d(TAG, "Использование запасного метода загрузки имени пользователя");
        // Загружаем имя пользователя из Firebase
        FirebaseDataManager.getUserName()
                .thenAccept(username -> {
                    runOnUiThread(() -> {
                        if (usernameTv != null) usernameTv.setText(username);
                        Log.d(TAG, "Имя пользователя через запасной метод: " + username);
                        
                        // Сохраняем имя пользователя в SharedPreferences
                        sharedPreferences.edit().putString(KEY_USERNAME, username).apply();
                        
                        isLoading = false;
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                    });
                })
                .exceptionally(e -> {
                    Log.e(TAG, "Ошибка загрузки имени пользователя через запасной метод", e);
                    
                    // Если все методы не сработали, проверяем есть ли смысл повторить попытку
                    runOnUiThread(() -> {
                        if (retryCount < MAX_RETRIES) {
                            retryCount++;
                            Log.d(TAG, "Повторная попытка загрузки имени пользователя (" + retryCount + "/" + MAX_RETRIES + ")");
                            
                            // Делаем паузу перед повторной попыткой
                            new Handler().postDelayed(() -> {
                                tryLoadUsernameDirectly();
                            }, RETRY_DELAY_MS);
                        } else {
                            Log.w(TAG, "Достигнуто максимальное количество попыток, используем кэшированное значение");
                            String cachedUsername = sharedPreferences.getString(KEY_USERNAME, "Пользователь");
                            if (usernameTv != null) usernameTv.setText(cachedUsername);
                            isLoading = false;
                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                        }
                    });
                    return null;
                });
    }

    // Метод для прямого чтения из базы данных при повторных попытках
    private void tryLoadUsernameDirectly() {
        if (currentUser == null) {
            Log.w(TAG, "Пользователь не авторизован при повторной попытке");
            isLoading = false;
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            return;
        }
        
        // Пробуем читать напрямую из Firebase
        try {
            Log.d(TAG, "Прямое чтение из FirebaseDatabase для UID: " + currentUser.getUid());
            
            // Получаем ссылку на базу данных напрямую
            FirebaseDatabase database = FirebaseDatabase.getInstance("https://emotions-guide-c173c-default-rtdb.europe-west1.firebasedatabase.app");
            DatabaseReference userRef = database.getReference("Users").child(currentUser.getUid()).child("username");
            
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String username = dataSnapshot.getValue(String.class);
                    Log.d(TAG, "Прямое чтение: получено имя пользователя: " + username);
                    
                    if (username == null || username.trim().isEmpty()) {
                        username = "Пользователь";
                    }
                    
                    final String finalUsername = username;
                    runOnUiThread(() -> {
                        if (usernameTv != null) usernameTv.setText(finalUsername);
                        sharedPreferences.edit().putString(KEY_USERNAME, finalUsername).apply();
                        isLoading = false;
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                    });
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Ошибка при прямом чтении: " + databaseError.getMessage());
                    runOnUiThread(() -> {
                        String cachedUsername = sharedPreferences.getString(KEY_USERNAME, "Пользователь");
                        if (usernameTv != null) usernameTv.setText(cachedUsername);
                        isLoading = false;
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при прямом чтении из Firebase", e);
            String cachedUsername = sharedPreferences.getString(KEY_USERNAME, "Пользователь");
            if (usernameTv != null) usernameTv.setText(cachedUsername);
            isLoading = false;
            if (progressBar != null) progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.FirstFragment ||
                id == R.id.TestsFragment ||
                id == R.id.AiPsychologistFragment ||
                id == R.id.RelaxationFragment ||
                id == R.id.ChartsFragment) {
            Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
            intent.putExtra("fragment_id", id);
            startActivity(intent);
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
}
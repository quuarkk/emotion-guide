package com.example.emo.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.emo.EditProfileActivity;
import com.example.emo.R;
import com.example.emo.firebase.FirebaseDataManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private static final String PREFS_NAME = "user_prefs";
    private static final String KEY_USERNAME = "KEY_USERNAME";
    private static final String KEY_AVATAR = "KEY_AVATAR";
    
    private TextView usernameTv, emailTv, registrationDateTv;
    private ImageButton editProfileBtn;
    private Button resetTestResultsBtn;
    private ImageView avatarIv;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private DatabaseReference userRef;
    private boolean isLoading = false;
    private SharedPreferences sharedPreferences;
    private int retryCount = 0;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000; // 2 секунды задержки между попытками

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        // Инициализация SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Передаем контекст в FirebaseDataManager
        FirebaseDataManager.setApplicationContext(requireActivity().getApplicationContext());

        // Инициализация Firebase
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            return view;
        }

        // Запускаем тестирование Firebase при инициализации
        FirebaseDataManager.testFirebaseConfiguration();

        userRef = FirebaseDatabase.getInstance("https://emotions-guide-c173c-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("Users")
                .child(currentUser.getUid());

        // Инициализация UI элементов
        usernameTv = view.findViewById(R.id.username_tv);
        emailTv = view.findViewById(R.id.email_tv);
        registrationDateTv = view.findViewById(R.id.registration_date_tv);
        editProfileBtn = view.findViewById(R.id.edit_profile_btn);
        resetTestResultsBtn = view.findViewById(R.id.reset_test_results_btn);
        avatarIv = view.findViewById(R.id.avatar_iv);
        progressBar = view.findViewById(R.id.progressBar);

        // Загрузка сохраненного аватара
        loadAvatarFromPreferences();

        // Установка кэшированных данных (если есть)
        String cachedUsername = sharedPreferences.getString(KEY_USERNAME, null);
        if (cachedUsername != null) {
            usernameTv.setText(cachedUsername);
        }

        // Обработчики кнопок
        editProfileBtn.setOnClickListener(v -> {
            Log.d(TAG, "Нажата кнопка редактирования профиля");
            startActivity(new Intent(requireActivity(), EditProfileActivity.class));
        });

        resetTestResultsBtn.setOnClickListener(v -> {
            Log.d(TAG, "Нажата кнопка сброса результатов теста");
            resetTestResults();
        });

        return view;
    }

    @Override
    public void onStart() {
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
            Toast.makeText(requireContext(), "Нет подключения к интернету", Toast.LENGTH_SHORT).show();
            isLoading = false;
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            return;
        }
        
        // Проверяем авторизацию
        if (currentUser == null) {
            Log.w(TAG, "Пользователь не авторизован");
            Toast.makeText(requireContext(), "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            isLoading = false;
            if (progressBar != null) progressBar.setVisibility(View.GONE);
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
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (usernameTv != null) usernameTv.setText(username);
                                
                                // Сохраняем имя пользователя в SharedPreferences
                                sharedPreferences.edit().putString(KEY_USERNAME, username).apply();
                                
                                isLoading = false;
                                if (progressBar != null) progressBar.setVisibility(View.GONE);
                            });
                        }
                    } else {
                        Log.w(TAG, "Не удалось получить имя пользователя напрямую, используем запасной метод");
                        // При неудаче используем стандартный метод
                        loadUsernameWithFallback();
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка загрузки данных пользователя", e);
            Toast.makeText(requireContext(), "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(requireContext(), "Нет подключения к интернету", Toast.LENGTH_LONG).show();
            return;
        }

        showProgress(true);
        Log.d(TAG, "Начало сброса результатов теста");

        userRef.child("TestResults").removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Результаты теста успешно сброшены");
                    Toast.makeText(requireContext(), "Результаты теста САН успешно сброшены", Toast.LENGTH_SHORT).show();
                    showProgress(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Ошибка сброса результатов теста: " + e.getMessage());
                    Toast.makeText(requireContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
        ConnectivityManager connectivityManager = (ConnectivityManager) requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
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
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (usernameTv != null) usernameTv.setText(username);
                            Log.d(TAG, "Имя пользователя через запасной метод: " + username);
                            
                            // Сохраняем имя пользователя в SharedPreferences
                            sharedPreferences.edit().putString(KEY_USERNAME, username).apply();
                            
                            isLoading = false;
                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                        });
                    }
                })
                .exceptionally(e -> {
                    Log.e(TAG, "Ошибка загрузки имени пользователя через запасной метод", e);
                    
                    // Если все методы не сработали, проверяем есть ли смысл повторить попытку
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
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
                    }
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

        String userId = currentUser.getUid();
        FirebaseDataManager.readUsernameDirectly(userId, new FirebaseDataManager.OnUsernameLoadedListener() {
            @Override
            public void onUsernameLoaded(String username, boolean success) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (success) {
                            Log.d(TAG, "Имя пользователя получено при повторной попытке: " + username);
                            if (usernameTv != null) usernameTv.setText(username);
                            sharedPreferences.edit().putString(KEY_USERNAME, username).apply();
                        } else {
                            Log.w(TAG, "Не удалось получить имя пользователя при повторной попытке");
                            String cachedUsername = sharedPreferences.getString(KEY_USERNAME, "Пользователь");
                            if (usernameTv != null) usernameTv.setText(cachedUsername);
                        }
                        isLoading = false;
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                    });
                }
            }
        });
    }

    private void loadAvatarFromPreferences() {
        try {
            String avatarString = sharedPreferences.getString(KEY_AVATAR, null);
            if (avatarString != null && !avatarString.isEmpty()) {
                byte[] avatarBytes = Base64.decode(avatarString, Base64.DEFAULT);
                Bitmap avatarBitmap = BitmapFactory.decodeByteArray(avatarBytes, 0, avatarBytes.length);
                if (avatarBitmap != null && avatarIv != null) {
                    avatarIv.setImageBitmap(avatarBitmap);
                    Log.d(TAG, "Аватар успешно загружен из SharedPreferences");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка загрузки аватара из SharedPreferences", e);
        }
    }
} 
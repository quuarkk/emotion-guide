package com.example.emo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.emo.TestResultsManager;
import com.example.emo.utils.SharedPreferencesManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private TextView greetingText;
    private ImageView profileImage;
    private CardView statsCard;
    private TextView wellbeingValue;
    private TextView activityValue;
    private TextView moodValue;
    private Button openChatButton;
    private CardView relaxationCard;
    private CardView breathingCard;
    
    private FirebaseUser currentUser;
    private DatabaseReference userRef;
    private TestResultsManager testResultsManager;
    private SharedPreferencesManager sharedPreferencesManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Инициализация Firebase
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
            return;
        }
        
        userRef = FirebaseDatabase.getInstance("https://emotions-guide-c173c-default-rtdb.europe-west1.firebasedatabase.app/")
                .getReference()
                .child("Users")
                .child(currentUser.getUid());
                
        testResultsManager = new TestResultsManager();
        sharedPreferencesManager = new SharedPreferencesManager(requireContext());
        
        // Инициализация UI элементов
        initUI(view);
        
        // Установка приветствия
        setGreeting();
        
        // Загрузка последних результатов тестов
        loadLatestTestResults();
        
        // Настройка обработчиков событий
        setupClickListeners();
    }
    
    private void initUI(View view) {
        greetingText = view.findViewById(R.id.greeting_text);
        profileImage = view.findViewById(R.id.profile_image);
        statsCard = view.findViewById(R.id.stats_card);
        wellbeingValue = view.findViewById(R.id.wellbeing_value);
        activityValue = view.findViewById(R.id.activity_value);
        moodValue = view.findViewById(R.id.mood_value);
        openChatButton = view.findViewById(R.id.open_chat_button);
        relaxationCard = view.findViewById(R.id.relaxation_card);
        breathingCard = view.findViewById(R.id.breathing_card);
    }
    
    private void setGreeting() {
        // Загружаем имя пользователя из Firebase
        userRef.child("username").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String username = dataSnapshot.getValue(String.class);
                if (username != null && !username.isEmpty()) {
                    greetingText.setText("Привет, " + username + "!");
                } else {
                    // Если имя не найдено в базе, используем displayName из FirebaseUser
                    String displayName = currentUser.getDisplayName();
                    if (displayName != null && !displayName.isEmpty()) {
                        greetingText.setText("Привет, " + displayName + "!");
                    } else {
                        greetingText.setText("Привет!");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Ошибка при загрузке имени пользователя: " + databaseError.getMessage());
                // Используем displayName как запасной вариант
                String displayName = currentUser.getDisplayName();
                if (displayName != null && !displayName.isEmpty()) {
                    greetingText.setText("Привет, " + displayName + "!");
                } else {
                    greetingText.setText("Привет!");
                }
            }
        });
    }
    
    private void loadLatestTestResults() {
        Log.d(TAG, "Начало загрузки последних результатов теста");
        
        // Сначала пытаемся загрузить данные из кэша
        String cachedResults = sharedPreferencesManager.getTestResults();
        if (cachedResults != null) {
            Log.d(TAG, "Загружены кэшированные результаты: " + cachedResults);
            try {
                JSONObject jsonObject = new JSONObject(cachedResults);
                float wellbeing = (float) jsonObject.getDouble("wellbeingScore");
                float activity = (float) jsonObject.getDouble("activityScore");
                float mood = (float) jsonObject.getDouble("moodScore");
                updateUIWithTestResults(wellbeing, activity, mood);
            } catch (JSONException e) {
                Log.e(TAG, "Ошибка парсинга кэшированных результатов: " + e.getMessage());
            }
        } else {
            // Устанавливаем значения по умолчанию, если кэша нет
            wellbeingValue.setText("0,0");
            activityValue.setText("0,0");
            moodValue.setText("0,0");
        }
        
        // Проверяем, что пользователь авторизован
        if (currentUser == null) {
            Log.w(TAG, "Пользователь не авторизован");
            return;
        }
        
        Log.d(TAG, "Загружаем результаты для пользователя: " + currentUser.getUid());
        
        // Загружаем последние результаты теста из Firebase
        userRef.child("TestResults").orderByChild("timestamp").limitToLast(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Log.d(TAG, "Получены данные из Firebase. Существует: " + dataSnapshot.exists() + ", Количество детей: " + dataSnapshot.getChildrenCount());
                        
                        if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                Log.d(TAG, "Обрабатываем результат теста с ключом: " + snapshot.getKey());
                                
                                try {
                                    Float wellbeing = snapshot.child("wellbeingScore").getValue(Float.class);
                                    Float activity = snapshot.child("activityScore").getValue(Float.class);
                                    Float mood = snapshot.child("moodScore").getValue(Float.class);
                                    
                                    Log.d(TAG, "Извлеченные значения: wellbeing=" + wellbeing + ", activity=" + activity + ", mood=" + mood);
                                    
                                    if (wellbeing != null && activity != null && mood != null) {
                                        // Обновляем UI в основном потоке и кэшируем
                                        updateUIWithTestResults(wellbeing, activity, mood);
                                        JSONObject jsonToCache = new JSONObject();
                                        jsonToCache.put("wellbeingScore", wellbeing);
                                        jsonToCache.put("activityScore", activity);
                                        jsonToCache.put("moodScore", mood);
                                        sharedPreferencesManager.saveTestResults(jsonToCache.toString());
                                        Log.d(TAG, "Результаты теста САН загружены и кэшированы: С=" + wellbeing + ", А=" + activity + ", Н=" + mood);
                                    } else {
                                        Log.w(TAG, "Неполные данные теста: wellbeing=" + wellbeing + ", activity=" + activity + ", mood=" + mood);
                                        
                                        // Попробуем получить данные как Double
                                        Double wellbeingDouble = snapshot.child("wellbeingScore").getValue(Double.class);
                                        Double activityDouble = snapshot.child("activityScore").getValue(Double.class);
                                        Double moodDouble = snapshot.child("moodScore").getValue(Double.class);
                                        
                                        Log.d(TAG, "Попытка получить как Double: wellbeing=" + wellbeingDouble + ", activity=" + activityDouble + ", mood=" + moodDouble);
                                        
                                        if (wellbeingDouble != null && activityDouble != null && moodDouble != null) {
                                            updateUIWithTestResults(wellbeingDouble.floatValue(), activityDouble.floatValue(), moodDouble.floatValue());
                                            JSONObject jsonToCache = new JSONObject();
                                            jsonToCache.put("wellbeingScore", wellbeingDouble);
                                            jsonToCache.put("activityScore", activityDouble);
                                            jsonToCache.put("moodScore", moodDouble);
                                            sharedPreferencesManager.saveTestResults(jsonToCache.toString());
                                            Log.d(TAG, "Результаты теста САН загружены и кэшированы (Double): С=" + wellbeingDouble + ", А=" + activityDouble + ", Н=" + moodDouble);
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Ошибка при обработке данных теста: " + e.getMessage(), e);
                                }
                            }
                        } else {
                            Log.d(TAG, "Результаты тестов не найдены в Firebase");
                            // Если нет результатов в Firebase, очищаем кэш
                            sharedPreferencesManager.clearTestResults();
                            // Устанавливаем значения по умолчанию
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    wellbeingValue.setText("0,0");
                                    activityValue.setText("0,0");
                                    moodValue.setText("0,0");
                                });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Ошибка при загрузке результатов теста: " + databaseError.getMessage());
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Не удалось загрузить результаты", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Новый метод для обновления UI
    private void updateUIWithTestResults(float wellbeing, float activity, float mood) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                wellbeingValue.setText(String.format(Locale.getDefault(), "%.1f", wellbeing));
                activityValue.setText(String.format(Locale.getDefault(), "%.1f", activity));
                moodValue.setText(String.format(Locale.getDefault(), "%.1f", mood));
            });
        }
    }
    
    private void setupClickListeners() {
        statsCard.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.ChartsFragment);
        });
        
        openChatButton.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.AiPsychologistFragment);
        });
        
        relaxationCard.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.RelaxationFragment);
        });
        
        breathingCard.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.BreathingFragment);
        });
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Обновляем данные при возвращении к фрагменту
        loadLatestTestResults();
    }
} 
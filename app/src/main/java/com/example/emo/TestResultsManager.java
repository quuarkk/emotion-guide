package com.example.emo;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestResultsManager {
    
    private final FirebaseDatabase database;
    
    public TestResultsManager() {
        database = FirebaseDatabase.getInstance();
    }
    
    // Метод для сохранения результатов теста в Firebase
    public void saveTestResult(TestResult result, String testType, OnTestResultSavedListener listener) {
        // Получаем текущего пользователя
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        
        if (currentUser != null) {
            // Создаем путь для сохранения результатов тестов пользователя
            DatabaseReference userTestsRef = database.getReference("users")
                    .child(currentUser.getUid())
                    .child("test_results")
                    .child(testType);
            
            // Генерируем уникальный ключ для результата теста
            String testResultKey = userTestsRef.push().getKey();
            
            if (testResultKey != null) {
                // Сохраняем результат теста
                userTestsRef.child(testResultKey).setValue(result)
                        .addOnSuccessListener(aVoid -> {
                            // Успешно сохранено
                            if (listener != null) {
                                listener.onTestResultSaved();
                            }
                        })
                        .addOnFailureListener(e -> {
                            // Ошибка при сохранении
                            if (listener != null) {
                                listener.onTestResultError(e.getMessage());
                            }
                        });
            }
        } else {
            // Пользователь не авторизован
            if (listener != null) {
                listener.onTestResultError("Пользователь не авторизован");
            }
        }
    }
    
    // Метод для получения результатов тестов пользователя
    public void fetchUserTestResults(OnTestResultsLoadedListener listener) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        
        if (currentUser == null) {
            if (listener != null) {
                listener.onTestResultsError("Пользователь не авторизован");
            }
            return;
        }
        
        String userId = currentUser.getUid();
        DatabaseReference userTestsRef = database.getReference("users")
                .child(userId)
                .child("test_results");
        
        userTestsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, List<TestResult>> testResults = new HashMap<>();
                
                for (DataSnapshot testTypeSnapshot : dataSnapshot.getChildren()) {
                    String testType = testTypeSnapshot.getKey();
                    List<TestResult> results = new ArrayList<>();
                    
                    for (DataSnapshot resultSnapshot : testTypeSnapshot.getChildren()) {
                        TestResult result = resultSnapshot.getValue(TestResult.class);
                        if (result != null) {
                            results.add(result);
                        }
                    }
                    
                    testResults.put(testType, results);
                }
                
                if (listener != null) {
                    listener.onTestResultsLoaded(testResults);
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (listener != null) {
                    listener.onTestResultsError(databaseError.getMessage());
                }
            }
        });
    }
    
    // Метод для получения статистики по тестам
    public void getTestStatistics(String testType, OnTestStatisticsLoadedListener listener) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        
        if (currentUser == null) {
            if (listener != null) {
                listener.onTestStatisticsError("Пользователь не авторизован");
            }
            return;
        }
        
        String userId = currentUser.getUid();
        DatabaseReference testResultsRef = database.getReference("users")
                .child(userId)
                .child("test_results")
                .child(testType);
        
        testResultsRef.orderByChild("timestamp").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<TestResult> results = new ArrayList<>();
                
                for (DataSnapshot resultSnapshot : dataSnapshot.getChildren()) {
                    TestResult result = resultSnapshot.getValue(TestResult.class);
                    if (result != null) {
                        results.add(result);
                    }
                }
                
                // Вычисляем статистику
                float avgWellbeing = 0;
                float avgActivity = 0;
                float avgMood = 0;
                
                if (!results.isEmpty()) {
                    for (TestResult result : results) {
                        avgWellbeing += result.getWellbeingScore();
                        avgActivity += result.getActivityScore();
                        avgMood += result.getMoodScore();
                    }
                    
                    avgWellbeing /= results.size();
                    avgActivity /= results.size();
                    avgMood /= results.size();
                }
                
                if (listener != null) {
                    listener.onTestStatisticsLoaded(results, avgWellbeing, avgActivity, avgMood);
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (listener != null) {
                    listener.onTestStatisticsError(databaseError.getMessage());
                }
            }
        });
    }
    
    // Интерфейсы для обратных вызовов
    public interface OnTestResultSavedListener {
        void onTestResultSaved();
        void onTestResultError(String errorMessage);
    }
    
    public interface OnTestResultsLoadedListener {
        void onTestResultsLoaded(Map<String, List<TestResult>> testResults);
        void onTestResultsError(String errorMessage);
    }
    
    public interface OnTestStatisticsLoadedListener {
        void onTestStatisticsLoaded(List<TestResult> results, float avgWellbeing, float avgActivity, float avgMood);
        void onTestStatisticsError(String errorMessage);
    }
} 
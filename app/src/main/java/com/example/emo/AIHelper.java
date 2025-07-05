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

public class AIHelper {
    
    private final FirebaseDatabase database;
    
    public AIHelper() {
        database = FirebaseDatabase.getInstance();
    }
    
    // Метод для получения результатов тестов пользователя
    public void fetchUserTestResults(String userId, OnTestResultsLoadedListener listener) {
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
                
                listener.onTestResultsLoaded(testResults);
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                listener.onTestResultsError(databaseError.getMessage());
            }
        });
    }

    // Интерфейс для обратного вызова при загрузке результатов тестов
    public interface OnTestResultsLoadedListener {
        void onTestResultsLoaded(Map<String, List<TestResult>> testResults);
        void onTestResultsError(String errorMessage);
    }

    // Метод для получения статистики по тестам
    public void getTestStatistics(String userId, String testType, OnTestStatisticsLoadedListener listener) {
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
                
                listener.onTestStatisticsLoaded(results, avgWellbeing, avgActivity, avgMood);
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                listener.onTestStatisticsError(databaseError.getMessage());
            }
        });
    }

    // Интерфейс для обратного вызова при загрузке статистики тестов
    public interface OnTestStatisticsLoadedListener {
        void onTestStatisticsLoaded(List<TestResult> results, float avgWellbeing, float avgActivity, float avgMood);
        void onTestStatisticsError(String errorMessage);
    }

    // Метод для анализа результатов тестов
    public void analyzeTestResults(Map<String, List<TestResult>> testResults, OnAnalysisCompletedListener listener) {
        StringBuilder analysis = new StringBuilder();
        
        // Проверяем результаты теста на стресс
        if (testResults.containsKey("stress_test")) {
            List<TestResult> stressResults = testResults.get("stress_test");
            if (stressResults != null && !stressResults.isEmpty()) {
                // Берем последний результат теста
                TestResult latestResult = stressResults.get(stressResults.size() - 1);
                float stressLevel = latestResult.getWellbeingScore();
                
                analysis.append("Анализ уровня стресса: ");
                if (stressLevel < 5) {
                    analysis.append("У вас низкий уровень стресса. Продолжайте практиковать техники релаксации.\n\n");
                } else if (stressLevel < 7) {
                    analysis.append("У вас средний уровень стресса. Рекомендую больше отдыхать и практиковать медитацию.\n\n");
                } else {
                    analysis.append("У вас высокий уровень стресса. Рекомендую обратиться к специалисту и уделить время релаксации.\n\n");
                }
            }
        }
        
        // Проверяем результаты теста на эмоциональный интеллект
        if (testResults.containsKey("emotional_intelligence_test")) {
            List<TestResult> eiResults = testResults.get("emotional_intelligence_test");
            if (eiResults != null && !eiResults.isEmpty()) {
                TestResult latestResult = eiResults.get(eiResults.size() - 1);
                float eiScore = latestResult.getWellbeingScore();
                
                analysis.append("Анализ эмоционального интеллекта: ");
                if (eiScore < 4) {
                    analysis.append("Ваш эмоциональный интеллект ниже среднего. Рекомендую больше практиковать осознанность и развивать эмпатию.\n\n");
                } else if (eiScore < 7) {
                    analysis.append("У вас средний уровень эмоционального интеллекта. Продолжайте развивать навыки понимания эмоций.\n\n");
                } else {
                    analysis.append("У вас высокий уровень эмоционального интеллекта. Вы хорошо понимаете свои и чужие эмоции.\n\n");
                }
            }
        }
        
        // Проверяем результаты теста Бойко
        if (testResults.containsKey("boyko_test")) {
            List<TestResult> boykoResults = testResults.get("boyko_test");
            if (boykoResults != null && !boykoResults.isEmpty()) {
                TestResult latestResult = boykoResults.get(boykoResults.size() - 1);
                float burnoutLevel = 10 - latestResult.getWellbeingScore(); // Инвертируем шкалу
                
                analysis.append("Анализ эмоционального выгорания (Бойко): ");
                if (burnoutLevel < 3) {
                    analysis.append("У вас низкий уровень эмоционального выгорания. Продолжайте заботиться о своем психологическом здоровье.\n\n");
                } else if (burnoutLevel < 7) {
                    analysis.append("У вас средний уровень эмоционального выгорания. Рекомендую обратить внимание на отдых и восстановление.\n\n");
                } else {
                    analysis.append("У вас высокий уровень эмоционального выгорания. Рекомендую обратиться к специалисту и пересмотреть баланс работы и отдыха.\n\n");
                }
            }
        }
        
        // Проверяем результаты теста Маслач
        if (testResults.containsKey("maslach_test")) {
            List<TestResult> maslachResults = testResults.get("maslach_test");
            if (maslachResults != null && !maslachResults.isEmpty()) {
                TestResult latestResult = maslachResults.get(maslachResults.size() - 1);
                float burnoutLevel = 10 - latestResult.getWellbeingScore(); // Инвертируем шкалу
                
                analysis.append("Анализ профессионального выгорания (Маслач): ");
                if (burnoutLevel < 3) {
                    analysis.append("У вас низкий уровень профессионального выгорания. Вы эффективно справляетесь с рабочими стрессами.\n\n");
                } else if (burnoutLevel < 7) {
                    analysis.append("У вас средний уровень профессионального выгорания. Обратите внимание на свое эмоциональное состояние на работе.\n\n");
                } else {
                    analysis.append("У вас высокий уровень профессионального выгорания. Рекомендую пересмотреть свое отношение к работе и обратиться к специалисту.\n\n");
                }
            }
        }
        
        // Проверяем результаты теста самооценки
        if (testResults.containsKey("self_esteem_test")) {
            List<TestResult> selfEsteemResults = testResults.get("self_esteem_test");
            if (selfEsteemResults != null && !selfEsteemResults.isEmpty()) {
                TestResult latestResult = selfEsteemResults.get(selfEsteemResults.size() - 1);
                float selfEsteemLevel = latestResult.getWellbeingScore();
                
                analysis.append("Анализ самооценки: ");
                if (selfEsteemLevel < 4) {
                    analysis.append("У вас низкий уровень самооценки. Рекомендую работать над повышением уверенности в себе и позитивным мышлением.\n\n");
                } else if (selfEsteemLevel < 7) {
                    analysis.append("У вас средний уровень самооценки. Продолжайте развивать уверенность в себе и своих способностях.\n\n");
                } else {
                    analysis.append("У вас высокий уровень самооценки. Это помогает вам эффективно справляться с жизненными вызовами.\n\n");
                }
            }
        }
        
        // Добавляем анализ других тестов...
        
        if (listener != null) {
            listener.onAnalysisCompleted(analysis.toString());
        }
    }
    
    // Метод для получения и анализа результатов тестов
    public void getAndAnalyzeTestResults(OnAnalysisCompletedListener listener) {
        TestResultsManager testResultsManager = new TestResultsManager();
        testResultsManager.fetchUserTestResults(new TestResultsManager.OnTestResultsLoadedListener() {
            @Override
            public void onTestResultsLoaded(Map<String, List<TestResult>> testResults) {
                analyzeTestResults(testResults, listener);
            }
            
            @Override
            public void onTestResultsError(String errorMessage) {
                if (listener != null) {
                    listener.onAnalysisCompleted("Не удалось получить результаты тестов: " + errorMessage);
                }
            }
        });
    }
    
    // Интерфейс для обратного вызова при завершении анализа
    public interface OnAnalysisCompletedListener {
        void onAnalysisCompleted(String analysisText);
    }
} 
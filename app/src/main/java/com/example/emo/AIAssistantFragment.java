package com.example.emo;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.emo.databinding.FragmentAiAssistantBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.Map;

public class AIAssistantFragment extends Fragment {

    private FragmentAiAssistantBinding binding;
    private AIHelper aiHelper;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentAiAssistantBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        aiHelper = new AIHelper();
        
        // Загружаем анализ тестов при создании фрагмента
        loadTestAnalysis();
    }

    private void loadTestAnalysis() {
        AIHelper aiHelper = new AIHelper();
        aiHelper.getAndAnalyzeTestResults(analysisText -> {
            // Отображаем результаты анализа
            TextView analysisTextView = binding.aiAnalysisText;
            analysisTextView.setText(analysisText);
        });
    }
    
    private void analyzeUserData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            
            // Получаем результаты тестов
            aiHelper.fetchUserTestResults(userId, new AIHelper.OnTestResultsLoadedListener() {
                @Override
                public void onTestResultsLoaded(Map<String, List<TestResult>> testResults) {
                    // Анализируем результаты тестов
                    analyzeTestResults(testResults);
                }
                
                @Override
                public void onTestResultsError(String errorMessage) {
                    // Обработка ошибки
                    Log.e("AIAssistant", "Error loading test results: " + errorMessage);
                }
            });
        }
    }

    private void analyzeTestResults(Map<String, List<TestResult>> testResults) {
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
        
        // Добавляем анализ других тестов...
        
        // Отображаем результаты анализа
        displayAIAnalysis(analysis.toString());
    }

    private void displayAIAnalysis(String analysisText) {
        // Отображение анализа в интерфейсе
        TextView analysisTextView = binding.aiAnalysisText;
        analysisTextView.setText(analysisText);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 
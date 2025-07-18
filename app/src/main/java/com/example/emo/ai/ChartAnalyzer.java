package com.example.emo.ai;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.example.emo.TestResult;
import com.example.emo.openai.ApiClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ChartAnalyzer {
    private static final String MODEL = "meta-llama/Llama-3.3-70B-Instruct";
    private static Context context;

    public static void init(Context appContext) {
        context = appContext.getApplicationContext();
    }

    private static boolean isInternetAvailable() {
        if (context == null) {
            return false;
        }
        
        ConnectivityManager connectivityManager = (ConnectivityManager) 
            context.getSystemService(Context.CONNECTIVITY_SERVICE);
            
        if (connectivityManager == null) {
            return false;
        }

        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    public static CompletableFuture<String> analyzeChart(List<TestResult> results, String metricName) {
        if (!isInternetAvailable()) {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.complete("Для анализа графика требуется подключение к интернету");
            return future;
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("Ты - эксперт по анализу эмоционального состояния. Оценка состояния происходит по шкале от 1 до 7, где 1 - минимально худшее состояние, 4 - средне нормальное, а 7 - максимально хорошее. ");
        prompt.append("Проанализируй следующие значения для метрики '").append(metricName).append("' за последние записи:\n");
        
        for (TestResult result : results) {
            float value = 0;
            switch (metricName.toLowerCase()) {
                case "самочувствие":
                    value = result.getWellbeingScore();
                    break;
                case "активность":
                    value = result.getActivityScore();
                    break;
                case "настроение":
                    value = result.getMoodScore();
                    break;
            }
            prompt.append(value).append(", ");
        }
        
        prompt.append("\nДай краткий анализ тренда (не более 150 символов) на русском языке. ");
        prompt.append("Не используй фразы вроде 'Анализ показывает' или 'Можно заметить'. ");
        prompt.append("Начни сразу с сути.");

        return ApiClient.getCompletion(prompt.toString(), MODEL)
                .thenApply(response -> {
                    if (response == null || response.isEmpty()) {
                        return "Анализ недоступен";
                    }
                    return response;
                })
                .exceptionally(throwable -> {
                    if (!isInternetAvailable()) {
                        return "Для анализа графика требуется подключение к интернету";
                    }
                    return "Анализ недоступен: " + throwable.getMessage();
                });
    }
} 
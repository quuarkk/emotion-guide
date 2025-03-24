package com.example.emo.openai;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ApiClient {
    private static final String TAG = "ApiClient";
    private static final String API_URL = "https://api.intelligence.io.solutions/api/v1/chat/completions";
    private static final String API_KEY = "Bearer io-v2-eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJvd25lciI6IjI1MzZjZGIzLTFjY2QtNDA2Zi04NjE4LTU0OTVmZDdmMzg4OCIsImV4cCI6NDg5NjM1MzkwMX0.Mdl2yTIgRHMw03Vkm9CqWgHzC4JkJF6Ta7JkRxaFCXWHW_eI7JWmqnnKttyiWB7f4rkVfL6sgCN-beEiftl5YA";
    private static final String MODEL = "deepseek-ai/DeepSeek-R1";
    private static final Executor executor = Executors.newCachedThreadPool();
    
    // Максимальное количество повторных попыток при ошибке 504
    private static final int MAX_RETRIES = 3;
    
    // Создаем OkHttpClient без таймаутов для неограниченного ожидания
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(0, TimeUnit.SECONDS) // 0 означает неограниченное время
            .writeTimeout(0, TimeUnit.SECONDS)   // 0 означает неограниченное время
            .readTimeout(0, TimeUnit.SECONDS)    // 0 означает неограниченное время
            .retryOnConnectionFailure(true)
            .build();
    
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public static CompletableFuture<String> sendChatRequest(String systemPrompt, String userMessage) {
        // Создаем счетчик попыток
        AtomicInteger retryCount = new AtomicInteger(0);
        
        // Создаем CompletableFuture для результата
        CompletableFuture<String> future = new CompletableFuture<>();
        
        // Вызываем метод с учетом повторных попыток
        sendChatRequestWithRetry(systemPrompt, userMessage, future, retryCount);
        
        return future;
    }
    
    private static void sendChatRequestWithRetry(String systemPrompt, String userMessage, 
                                               CompletableFuture<String> future, AtomicInteger retryCount) {
        try {
            // Формируем тело запроса
            JSONObject data = new JSONObject();
            data.put("model", MODEL);

            JSONArray messages = new JSONArray();

            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);

            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);

            messages.put(systemMessage);
            messages.put(userMsg);

            data.put("messages", messages);
            
            int currentRetry = retryCount.get();
            Log.d(TAG, "Подготовлен запрос к API (попытка " + (currentRetry + 1) + "/" + (MAX_RETRIES + 1) + "): " 
                    + data.toString().substring(0, Math.min(100, data.toString().length())) + "...");
            
            // Создаем запрос
            RequestBody body = RequestBody.create(data.toString(), JSON);
            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", API_KEY)
                    .post(body)
                    .build();
            
            // Добавляем сообщение в лог перед отправкой запроса
            Log.d(TAG, "Отправка запроса к API...");
            
            // Отправляем асинхронный запрос
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Ошибка при отправке запроса", e);
                    
                    // Проверяем, можно ли повторить запрос
                    if (retryCount.incrementAndGet() <= MAX_RETRIES) {
                        Log.d(TAG, "Повторная попытка " + retryCount.get() + "/" + MAX_RETRIES + " после ошибки соединения");
                        // Небольшая задержка перед повторной попыткой
                        executor.execute(() -> {
                            try {
                                Thread.sleep(1000 * retryCount.get()); // Увеличиваем задержку с каждой попыткой
                                sendChatRequestWithRetry(systemPrompt, userMessage, future, retryCount);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                future.completeExceptionally(e);
                            }
                        });
                    } else {
                        future.completeExceptionally(new Exception("Ошибка соединения после " + MAX_RETRIES + 
                                " попыток: " + e.getMessage() + "\n\nПроверьте подключение к интернету и попробуйте снова."));
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (ResponseBody responseBody = response.body()) {
                        if (!response.isSuccessful()) {
                            String errorBody = responseBody != null ? responseBody.string() : "Неизвестная ошибка";
                            Log.e(TAG, "Ошибка API: " + response.code() + " - " + errorBody);
                            
                            // Специальная обработка для ошибки 504
                            if (response.code() == 504) {
                                // Проверяем, можно ли повторить запрос
                                if (retryCount.incrementAndGet() <= MAX_RETRIES) {
                                    Log.d(TAG, "Повторная попытка " + retryCount.get() + "/" + MAX_RETRIES + " после ошибки 504");
                                    // Небольшая задержка перед повторной попыткой
                                    executor.execute(() -> {
                                        try {
                                            Thread.sleep(2000 * retryCount.get()); // Увеличиваем задержку с каждой попыткой
                                            sendChatRequestWithRetry(systemPrompt, userMessage, future, retryCount);
                                        } catch (InterruptedException ie) {
                                            Thread.currentThread().interrupt();
                                            future.completeExceptionally(new Exception("Ошибка API: " + response.code() + 
                                                    (errorBody.length() > 0 ? " - " + errorBody : "")));
                                        }
                                    });
                                } else {
                                    future.completeExceptionally(new Exception(
                                            "Сервер не отвечает (ошибка 504) после " + MAX_RETRIES + " попыток. Это может быть вызвано:\n" +
                                            "1. Перегрузкой сервера\n" +
                                            "2. Медленным интернет-соединением\n" +
                                            "3. Временными проблемами с API\n\n" +
                                            "Пожалуйста, попробуйте позже или используйте другое подключение к интернету."));
                                }
                            } else {
                                future.completeExceptionally(new Exception("Ошибка API: " + response.code() + 
                                        (errorBody.length() > 0 ? " - " + errorBody : "")));
                            }
                            return;
                        }
                        
                        if (responseBody == null) {
                            future.completeExceptionally(new Exception("Пустой ответ от сервера"));
                            return;
                        }
                        
                        String responseString = responseBody.string();
                        Log.d(TAG, "Получен ответ от API: " + responseString.substring(0, Math.min(100, responseString.length())) + "...");
                        
                        JSONObject responseJson = new JSONObject(responseString);
                        String content = responseJson.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");

                        // Обработка формата ответа (если есть тег </think>)
                        String[] parts = content.split("</think>\n\n");
                        if (parts.length > 1) {
                            future.complete(parts[1]); // Возвращаем только видимую часть ответа
                        } else {
                            future.complete(content);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при обработке ответа", e);
                        
                        // Проверяем, можно ли повторить запрос
                        if (retryCount.incrementAndGet() <= MAX_RETRIES) {
                            Log.d(TAG, "Повторная попытка " + retryCount.get() + "/" + MAX_RETRIES + " после ошибки обработки");
                            // Небольшая задержка перед повторной попыткой
                            executor.execute(() -> {
                                try {
                                    Thread.sleep(1000 * retryCount.get()); // Увеличиваем задержку с каждой попыткой
                                    sendChatRequestWithRetry(systemPrompt, userMessage, future, retryCount);
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                    future.completeExceptionally(e);
                                }
                            });
                        } else {
                            future.completeExceptionally(new Exception("Ошибка при обработке ответа после " + 
                                    MAX_RETRIES + " попыток: " + e.getMessage()));
                        }
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при подготовке запроса", e);
            future.completeExceptionally(e);
        }
    }
    
    // Метод для отмены всех активных запросов при выходе из приложения
    public static void cancelAllRequests() {
        client.dispatcher().cancelAll();
    }
} 
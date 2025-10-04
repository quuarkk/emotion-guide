package com.example.emo.openai;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
    private static final String API_KEY = "Bearer io-v2-eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJvd25lciI6IjBhNzdmYzViLWNlODQtNDNmNi04Mjc2LWJjYzZmOTQ3MTcyMSIsImV4cCI6NDkxMTk5NDc5OH0.En6STI6gS_sBOH9fqzJtudqfrA6B4EWnNIOFEMtLtfJZfC8m0_mihKyykuBqh0xeFZbkXI4isKdbTkzN3uH2mA ";
    private static final String MODEL = "openai/gpt-oss-120b";
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

    // Интерфейс для обновления частичного ответа
    public interface StreamListener {
        void onPartialResponse(String partialResponse);
    }
    
    private static StreamListener streamListener;
    
    public static void setStreamListener(StreamListener listener) {
        streamListener = listener;
    }
    
    // Для анимации "думающей" нейросети
    private static int dotCount = 0;
    private static final String[] thinkingAnimation = {".", "..", "..."};
    private static long lastAnimationUpdateTime = 0;
    private static final long ANIMATION_DELAY_MS = 300; // Задержка между сменой анимации (800 мс)
    
    private static String getThinkingAnimation() {
        // Теперь возвращаем пустую строку, чтобы не показывать процесс "думания"
        return "";
    }
    
    private static void updatePartialResponse(String partialResponse) {
        // Проверяем, содержит ли ответ теги <think>
        if (partialResponse != null && partialResponse.contains("<think>") && !partialResponse.contains("</think>")) {
            // Если содержит только открывающий тег <think>, не отправляем обновление
            return;
        }
        
        // Если есть streamListener и ответ не пустой, отправляем обновление
        if (streamListener != null && partialResponse != null && !partialResponse.isEmpty()) {
            streamListener.onPartialResponse(partialResponse);
        }
    }

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
            data.put("max_tokens", 5000); // Ограничение на количество токенов для ответа
            data.put("stream", true); // Включаем потоковую генерацию

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
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "Неизвестная ошибка";
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
                    
                    ResponseBody responseBody = response.body();
                    if (responseBody == null) {
                        future.completeExceptionally(new Exception("Пустой ответ от сервера"));
                        return;
                    }
                    
                    // Для потоковой обработки
                    StringBuilder fullResponse = new StringBuilder();
                    StringBuilder currentChunk = new StringBuilder();
                    
                    try {
                        // Получаем поток данных
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(responseBody.byteStream()));
                        String line;
                        
                        while ((line = reader.readLine()) != null) {
                            if (line.isEmpty()) continue;
                            
                            // Пропускаем префикс "data: "
                            if (line.startsWith("data: ")) {
                                line = line.substring(6);
                            }
                            
                            // Проверяем, не является ли это сообщением о завершении
                            if (line.equals("[DONE]")) {
                                break;
                            }
                            
                            try {
                                JSONObject chunk = new JSONObject(line);
                                if (chunk.has("choices")) {
                                    JSONArray choices = chunk.getJSONArray("choices");
                                    if (choices.length() > 0) {
                                        JSONObject choice = choices.getJSONObject(0);
                                        if (choice.has("delta") && choice.getJSONObject("delta").has("content")) {
                                            String content = choice.getJSONObject("delta").getString("content");
                                            fullResponse.append(content);
                                            currentChunk.append(content);
                                            
                                            // Отправляем обновление, когда накопилось достаточно текста
                                            // или встретился знак пунктуации
                                            if (currentChunk.length() > 10 || 
                                                    content.matches(".*[.!?]\\s*$")) {
                                                // Создаем промежуточный результат
                                                String partialResult = fullResponse.toString();
                                                
                                                // Удаляем всё от начала до </think> если есть
                                                int thinkEndIndex = partialResult.indexOf("</think>");
                                                if (thinkEndIndex != -1) {
                                                    partialResult = partialResult.substring(thinkEndIndex + "</think>".length()).trim();
                                                    // Только если после удаления тегов есть контент, отправляем обновление
                                                    if (!partialResult.isEmpty()) {
                                                        // Отправляем промежуточное обновление через интерфейс
                                                        updatePartialResponse(partialResult);
                                                    }
                                                } else {
                                                    // Если тег </think> не найден, проверяем наличие <think>
                                                    int thinkStartIndex = partialResult.indexOf("<think>");
                                                    if (thinkStartIndex != -1) {
                                                        // Если есть только открывающий тег, не показываем ничего
                                                        // Не вызываем updatePartialResponse
                                                    } else {
                                                        // Если нет тегов <think>, показываем обычный ответ
                                                        updatePartialResponse(partialResult);
                                                    }
                                                }
                                                
                                                currentChunk.setLength(0); // Сбрасываем текущий чанк
                                            }
                                        }
                                    }
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "Ошибка при разборе JSON чанка: " + line, e);
                            }
                        }
                        
                        // Обрабатываем полный ответ
                        String finalResponse = fullResponse.toString();
                        
                        // Удаляем рассуждения нейросети в финальном ответе
                        int thinkEndIndex = finalResponse.indexOf("</think>");
                        if (thinkEndIndex != -1) {
                            finalResponse = finalResponse.substring(thinkEndIndex + "</think>".length()).trim();
                        } else {
                            // Если тег </think> не найден, проверяем наличие <think>
                            int thinkStartIndex = finalResponse.indexOf("<think>");
                            if (thinkStartIndex != -1) {
                                // Если есть только открывающий тег, удаляем всё до конца текста
                                finalResponse = finalResponse.substring(0, thinkStartIndex).trim();
                                if (finalResponse.isEmpty()) {
                                    // Если после удаления ничего не осталось, возвращаем сообщение
                                    finalResponse = "Извините, не удалось получить ответ. Пожалуйста, попробуйте еще раз.";
                                }
                            }
                        }
                        
                        // Отправляем финальное обновление через интерфейс
                        if (!finalResponse.isEmpty()) {
                            updatePartialResponse(finalResponse);
                        }
                        
                        future.complete(finalResponse);
                        
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при обработке потокового ответа", e);
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
                    } finally {
                        responseBody.close();
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

    public static CompletableFuture<String> getCompletion(String prompt, String model) {
        CompletableFuture<String> future = new CompletableFuture<>();

        new Thread(() -> {
            try {
                // Ограничиваем размер промпта
                String optimizedPrompt = prompt;
                if (prompt.length() > 15000) {
                    optimizedPrompt = prompt.substring(0, 15000) + "...";
                    Log.d(TAG, "Промпт был сокращен с " + prompt.length() + " до 15000 символов");
                }
                
                JSONObject json = new JSONObject();
                json.put("model", model);
                
                JSONArray messages = new JSONArray();
                JSONObject message = new JSONObject();
                message.put("role", "user");
                message.put("content", optimizedPrompt);
                messages.put(message);
                
                json.put("messages", messages);
                json.put("temperature", 0.7);
                json.put("max_tokens", 150);

                RequestBody body = RequestBody.create(json.toString(), JSON);
                Request request = new Request.Builder()
                        .url(API_URL)
                        .addHeader("Authorization", API_KEY)
                        .post(body)
                        .build();

                Log.d(TAG, "Отправка запроса: " + json.toString());

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "";
                        Log.e(TAG, "Ошибка API: " + response.code() + " - " + errorBody);
                        throw new IOException("Ошибка API: " + response.code() + (errorBody.isEmpty() ? "" : " - " + errorBody));
                    }

                    String responseBody = response.body().string();
                    Log.d(TAG, "Получен ответ: " + responseBody);
                    
                    JSONObject responseJson = new JSONObject(responseBody);
                    String completion = responseJson.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                            .trim();

                    future.complete(completion);
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при получении completion: " + e.getMessage());
                future.completeExceptionally(e);
            }
        }).start();

        return future;
    }
} 
package com.example.emo;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emo.databinding.FragmentAiPsychologistBinding;
import com.example.emo.firebase.FirebaseDataManager;
import com.example.emo.openai.ApiClient;

import org.json.JSONObject;

import io.noties.markwon.Markwon;
import io.noties.markwon.core.CorePlugin;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.html.HtmlPlugin;
import io.noties.markwon.image.ImagesPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;

public class AiPsychologistFragment extends Fragment {

    private FragmentAiPsychologistBinding binding;
    private RecyclerView chatRecyclerView;
    private Button analyzeButton;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private static final String TAG = "AiPsychologistFragment";
    private ProgressBar progressBar;
    private Markwon markwon;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentAiPsychologistBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        Log.d(TAG, "onViewCreated вызван");

        // Инициализация Markwon для отображения Markdown
        markwon = Markwon.builder(requireContext())
                .usePlugin(CorePlugin.create())
                .usePlugin(ImagesPlugin.create())
                .usePlugin(TablePlugin.create(requireContext()))
                .usePlugin(HtmlPlugin.create())
                .usePlugin(StrikethroughPlugin.create())
                .build();

        chatRecyclerView = binding.chatRecyclerView;
        analyzeButton = binding.analyzeButton;
        progressBar = view.findViewById(R.id.progressBar);

        // Инициализация списка сообщений
        chatMessages = new ArrayList<>();
        chatMessages.add(new ChatMessage("Здравствуйте! Я ваш ИИ-психолог. Нажмите на кнопку \"Анализировать результаты\", чтобы я проанализировал ваши тесты.", false));

        // Настройка RecyclerView
        chatAdapter = new ChatAdapter(chatMessages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);

        // Обработчик нажатия на кнопку анализа
        analyzeButton.setOnClickListener(v -> analyzeTestResults());
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void analyzeTestResults() {
        // Проверяем подключение к интернету
        if (!isNetworkAvailable()) {
            chatMessages.add(new ChatMessage("Отсутствует подключение к интернету. Пожалуйста, проверьте ваше соединение и попробуйте снова.", false));
            chatAdapter.notifyItemInserted(chatMessages.size() - 1);
            chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
            return;
        }

        // Показываем индикатор загрузки
        progressBar.setVisibility(View.VISIBLE);
        
        // Добавляем сообщение о начале анализа
        chatMessages.add(new ChatMessage("Анализирую ваши результаты...", false));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
        
        // Получаем имя пользователя
        FirebaseDataManager.getUserName()
            .thenCompose(username -> {
                Log.d(TAG, "Получено имя пользователя: " + username);
                // Получаем результаты тестов
                return FirebaseDataManager.getUserTestResults()
                    .thenApply(testResults -> {
                        Log.d(TAG, "Получено результатов тестов: " + testResults.size());
                        // Подготавливаем данные для ИИ
                        JSONObject data = FirebaseDataManager.prepareTestDataForAI(testResults, username);
                        return new Pair<>(testResults, data.toString(), username);
                    });
            })
            .thenAccept(triple -> {
                List<FirebaseDataManager.TestResult> testResults = triple.first;
                String jsonData = triple.second;
                String username = triple.third;
                
                // Формируем системный промпт в зависимости от наличия тестов
                String systemPrompt;
                if (testResults.isEmpty()) {
                    // Промпт для мотивации на прохождение тестов САН
                    systemPrompt = "Ты психолог, который объясняет важность тестов САН. " +
                            "Объясни пользователю " + username + ", почему регулярное прохождение тестов помогает: " +
                            "1) Отслеживать эмоциональные паттерны " +
                            "2) Улучшать самосознание " +
                            "3) Создавать основу для персонализированных рекомендаций. " +
                            "Используй поддерживающий тон и приведи примеры из практики.";
                } else {
                    // Промпт для анализа результатов + травма-ориентированная поддержка
                    systemPrompt = "Ты trauma-informed психолог, анализирующий тесты САН. " +
                            "Для пользователя " + username + " выполнить: " +

                            // Анализ тестов
                            "1. Сравнить показатели (норма 5-7) Самочувствия/Активности/Настроения " +
                            "2. Оценить динамику изменений при наличии нескольких тестов " +
                            "3. Выявить показатели ниже нормы и предложить: " +
                            "- Соматические практики (дыхание, расслабление) " +
                            "- Психообразовательные материалы " +
                            "- План поэтапного улучшения " +

                            // Травма-ориентированная работа
                            "4. Интегрировать результаты с эмоциональным состоянием: " +
                            "- Использовать grounding-техники при признаках дистресса " +
                            "- Применять inner child work для показателей, связанных с детской травмой " +
                            "- Работать с негативными убеждениями через когнитивные реструктуризации " +

                            // Формат ответа
                            "Ответ структурировать как: " +
                            "А) Краткий анализ тестов с визуализацией прогресса (эмодзи/шкалы) " +
                            "Б) 1-2 конкретные рекомендации с привязкой к показателям ";
                }

                Log.d(TAG, "Отправка запроса к API с системным промптом");
                
                // Отправляем запрос к API
                ApiClient.sendChatRequest(systemPrompt, jsonData)
                        .thenAccept(response -> {
                            Log.d(TAG, "Получен ответ от API");
                            requireActivity().runOnUiThread(() -> {
                                // Скрываем индикатор загрузки
                                progressBar.setVisibility(View.GONE);

                                // Удаляем сообщение о статусе анализа
                                for (int i = chatMessages.size() - 1; i >= 0; i--) {
                                    ChatMessage chatMessage = chatMessages.get(i);
                                    if (!chatMessage.isUser() && chatMessage.getText().contains("Анализирую")) {
                                        chatMessages.remove(i);
                                        chatAdapter.notifyItemRemoved(i);
                                        break;
                                    }
                                }

                                // Добавляем ответ ИИ в чат
                                chatMessages.add(new ChatMessage(response, false));
                                chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                                // Прокручиваем к последнему сообщению
                                chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
                            });
                        })
                        .exceptionally(e -> {
                            Log.e(TAG, "Ошибка при получении ответа от API", e);
                            requireActivity().runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                
                                // Получаем основное сообщение об ошибке
                                String errorMessage = e.getMessage();
                                if (e instanceof CompletionException && e.getCause() != null) {
                                    errorMessage = e.getCause().getMessage();
                                }
                                
                                // Если это не ошибка 504 (которая обрабатывается автоматически с повторными попытками)
                                if (!errorMessage.contains("504")) {
                                    // Добавляем сообщение об ошибке в чат с форматированием Markdown
                                    String formattedError = "### Ошибка при получении ответа\n\n" + 
                                            errorMessage + "\n\n" +
                                            "Попробуйте повторить запрос позже или проверить подключение к интернету.";
                                    
                                    // Удаляем сообщение о статусе анализа
                                    for (int i = chatMessages.size() - 1; i >= 0; i--) {
                                        ChatMessage chatMessage = chatMessages.get(i);
                                        if (!chatMessage.isUser() && chatMessage.getText().contains("Анализирую")) {
                                            chatMessages.set(i, new ChatMessage(formattedError, false));
                                            chatAdapter.notifyItemChanged(i);
                                            break;
                                        }
                                    }
                                }
                            });
                            return null;
                        });
            })
            .exceptionally(e -> {
                Log.e(TAG, "Error analyzing test results", e);
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    chatMessages.add(new ChatMessage("Произошла ошибка при получении данных: " + e.getMessage() + 
                            "\n\nПопробуйте перезапустить приложение или проверить подключение к интернету.", false));
                    chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                    chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
                });
                return null;
            });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    
    // Вспомогательный класс для тройки значений
    private static class Pair<F, S, T> {
        public final F first;
        public final S second;
        public final T third;

        public Pair(F first, S second, T third) {
            this.first = first;
            this.second = second;
            this.third = third;
        }
    }
    
    // Класс для представления сообщения в чате
    public static class ChatMessage {
        private final String text;
        private final boolean isUser;

        public ChatMessage(String text, boolean isUser) {
            this.text = text;
            this.isUser = isUser;
        }

        public String getText() {
            return text;
        }

        public boolean isUser() {
            return isUser;
        }
    }
    
    // Адаптер для RecyclerView
    private class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
        private final List<ChatMessage> messages;

        public ChatAdapter(List<ChatMessage> messages) {
            this.messages = messages;
        }

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_message, parent, false);
            return new ChatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            ChatMessage message = messages.get(position);
            
            // Применяем Markdown форматирование к тексту сообщения
            if (!message.isUser()) {
                markwon.setMarkdown(holder.messageText, message.getText());
                // Включаем поддержку кликабельных ссылок
                holder.messageText.setMovementMethod(LinkMovementMethod.getInstance());
            } else {
            holder.messageText.setText(message.getText());
            }
            
            // Настройка внешнего вида в зависимости от отправителя
            if (message.isUser()) {
                holder.messageText.setBackgroundResource(R.drawable.bg_user_message);
                holder.messageText.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
            } else {
                holder.messageText.setBackgroundResource(R.drawable.bg_ai_message);
                holder.messageText.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            }
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        class ChatViewHolder extends RecyclerView.ViewHolder {
            TextView messageText;

            ChatViewHolder(View itemView) {
                super(itemView);
                messageText = itemView.findViewById(R.id.message_text);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        // Отменяем все активные запросы при выходе из фрагмента
        ApiClient.cancelAllRequests();
    }

    // Добавьте этот метод в класс AiPsychologistFragment для обновления сообщения о статусе
    private void updateStatusMessage(String message) {
        requireActivity().runOnUiThread(() -> {
            // Находим последнее сообщение от ИИ
            for (int i = chatMessages.size() - 1; i >= 0; i--) {
                ChatMessage chatMessage = chatMessages.get(i);
                if (!chatMessage.isUser() && chatMessage.getText().contains("Анализирую")) {
                    // Обновляем сообщение
                    chatMessages.set(i, new ChatMessage(message, false));
                    chatAdapter.notifyItemChanged(i);
                    chatRecyclerView.smoothScrollToPosition(i);
                    return;
                }
            }
            
            // Если не нашли сообщение для обновления, добавляем новое
            chatMessages.add(new ChatMessage(message, false));
            chatAdapter.notifyItemInserted(chatMessages.size() - 1);
            chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
        });
    }
} 
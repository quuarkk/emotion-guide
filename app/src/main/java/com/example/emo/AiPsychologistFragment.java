package com.example.emo;

import static com.example.emo.R.id.message_progress;

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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

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
            chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
            return;
        }

        // Не показываем индикатор загрузки, так как у нас есть анимация "Анализирую..."
        // progressBar.setVisibility(View.VISIBLE);
        
        // Проверяем, есть ли уже сообщение "Анализирую" и удаляем его
        for (int i = chatMessages.size() - 1; i >= 0; i--) {
            ChatMessage message = chatMessages.get(i);
            if (!message.isUser() && message.getText().startsWith("Анализирую")) {
                chatMessages.remove(i);
                chatAdapter.notifyItemRemoved(i);
                break;
            }
        }
        
        // Добавляем сообщение о начале анализа
        chatMessages.add(new ChatMessage("Анализирую ваши результаты", false));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
        
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
                            "- План поэтапного улучшения " +

                            // Травма-ориентированная работа
                            "4. Интегрировать результаты с эмоциональным состоянием: " +
                            "- Использовать grounding-техники при признаках дистресса " +
                            "- Применять inner child work для показателей, связанных с детской травмой " +
                            "- Работать с негативными убеждениями через когнитивные реструктуризации " +

                            // Формат ответа
                            "Ответ структурировать как очень дружелюбный психолог, который помогает пользователю понять свои результаты тестов: " +
                            "А) Краткий анализ тестов с визуализацией прогресса эмодзи " +
                            "Б) 1-2 конкретные рекомендации с привязкой к показателям ";
                }

                Log.d(TAG, "Отправка запроса к API с системным промптом");
                
                // Устанавливаем слушатель для потоковых обновлений
                ApiClient.setStreamListener(partialResponse -> {
                    requireActivity().runOnUiThread(() -> {
                        // Обновляем сообщение о статусе с частичным ответом
                        updateStatusMessage(partialResponse);
                    });
                });
                
                // Отправляем запрос к API
                ApiClient.sendChatRequest(systemPrompt, jsonData)
                        .thenAccept(response -> {
                            Log.d(TAG, "Получен ответ от API");
                            requireActivity().runOnUiThread(() -> {
                                // Не нужно скрывать индикатор загрузки
                                // progressBar.setVisibility(View.GONE);
                                
                                // Финальное обновление уже произошло через StreamListener,
                                // поэтому не нужно добавлять новое сообщение
                                
                                // Прокручиваем к последнему сообщению без анимации
                                chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                            });
                        })
                        .exceptionally(e -> {
                            Log.e(TAG, "Ошибка при получении ответа от API", e);
                            requireActivity().runOnUiThread(() -> {
                                // Не нужно скрывать индикатор загрузки
                                // progressBar.setVisibility(View.GONE);
                                
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
                    // Не нужно скрывать индикатор загрузки
                    // progressBar.setVisibility(View.GONE);
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
        private String text;
        private final boolean isUser;

        public ChatMessage(String text, boolean isUser) {
            this.text = text;
            this.isUser = isUser;
        }

        public String getText() {
            return text;
        }
        
        public void setText(String text) {
            this.text = text;
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
                // Установка текста без перерисовки всего элемента
                if (holder.messageText.getTag() == null || !holder.messageText.getTag().equals(message.getText())) {
                    markwon.setMarkdown(holder.messageText, message.getText());
                    // Сохраняем текущий текст как тег, чтобы предотвратить ненужные обновления
                    holder.messageText.setTag(message.getText());
                    // Включаем поддержку кликабельных ссылок
                    holder.messageText.setMovementMethod(LinkMovementMethod.getInstance());
                }
                
                // Отображаем полосу загрузки только для сообщения "Анализирую результаты..."
                if (message.getText().startsWith("Анализирую")) {
                    holder.messageProgress.setVisibility(View.VISIBLE);
                } else {
                    holder.messageProgress.setVisibility(View.GONE);
                }
            } else {
                if (holder.messageText.getTag() == null || !holder.messageText.getTag().equals(message.getText())) {
                    holder.messageText.setText(message.getText());
                    holder.messageText.setTag(message.getText());
                }
                // Для сообщений пользователя всегда скрываем прогресс-бар
                holder.messageProgress.setVisibility(View.GONE);
            }
            
            // Настройка внешнего вида в зависимости от отправителя
            // Выполняем только один раз при создании ViewHolder
            if (holder.isStyleApplied == null || !holder.isStyleApplied) {
                // Получаем родительский контейнер (LinearLayout)
                View parentContainer = (View) holder.messageText.getParent();
                
                if (message.isUser()) {
                    // Применяем стиль к родительскому контейнеру
                    parentContainer.setBackgroundResource(R.drawable.bg_user_message);
                    holder.messageText.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
                    parentContainer.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                } else {
                    // Применяем стиль к родительскому контейнеру
                    parentContainer.setBackgroundResource(R.drawable.bg_ai_message);
                    holder.messageText.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
                    parentContainer.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
                }
                holder.isStyleApplied = true;
            }
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position, @NonNull List<Object> payloads) {
            if (payloads.isEmpty()) {
                // Если нет специальных данных, вызываем стандартный метод
                super.onBindViewHolder(holder, position, payloads);
            } else {
                // Обновляем только текст без перерисовки фона и других элементов
                for (Object payload : payloads) {
                    if (payload instanceof String) {
                        String newText = (String) payload;
                        if (holder.messageText.getTag() == null || !holder.messageText.getTag().equals(newText)) {
                            markwon.setMarkdown(holder.messageText, newText);
                            holder.messageText.setTag(newText);
                        }
                        
                        // Обновляем видимость прогресс-бара в зависимости от текста сообщения
                        if (newText.startsWith("Анализирую")) {
                            holder.messageProgress.setVisibility(View.VISIBLE);
                        } else {
                            holder.messageProgress.setVisibility(View.GONE);
                        }
                    }
                }
            }
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        class ChatViewHolder extends RecyclerView.ViewHolder {
            TextView messageText;
            ProgressBar messageProgress;
            Boolean isStyleApplied = false;

            ChatViewHolder(View itemView) {
                super(itemView);
                messageText = itemView.findViewById(R.id.message_text);
                messageProgress = itemView.findViewById(message_progress);
            }
        }
        
        // Метод для эффективного обновления списка сообщений
        public void updateMessages(List<ChatMessage> newMessages) {
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ChatMessageDiffCallback(messages, newMessages));
            messages.clear();
            messages.addAll(newMessages);
            diffResult.dispatchUpdatesTo(this);
        }
    }

    // DiffUtil для эффективного обновления RecyclerView
    private class ChatMessageDiffCallback extends DiffUtil.Callback {
        private final List<ChatMessage> oldMessages;
        private final List<ChatMessage> newMessages;

        public ChatMessageDiffCallback(List<ChatMessage> oldMessages, List<ChatMessage> newMessages) {
            this.oldMessages = oldMessages;
            this.newMessages = newMessages;
        }

        @Override
        public int getOldListSize() {
            return oldMessages.size();
        }

        @Override
        public int getNewListSize() {
            return newMessages.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            // Для упрощения - считаем элементы одинаковыми если они имеют одинаковый индекс
            // В более сложных случаях здесь может быть ID или другой уникальный идентификатор
            return oldItemPosition == newItemPosition;
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            ChatMessage oldMessage = oldMessages.get(oldItemPosition);
            ChatMessage newMessage = newMessages.get(newItemPosition);
            
            return oldMessage.isUser() == newMessage.isUser() && 
                   oldMessage.getText().equals(newMessage.getText());
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        // Отменяем все активные запросы и удаляем слушатель
        ApiClient.setStreamListener(null);
        ApiClient.cancelAllRequests();
    }

    // Обновляем метод для правильной работы с потоковыми ответами
    private void updateStatusMessage(String message) {
        requireActivity().runOnUiThread(() -> {
            // Находим последнее сообщение от ИИ
            boolean messageUpdated = false;
            for (int i = chatMessages.size() - 1; i >= 0; i--) {
                ChatMessage chatMessage = chatMessages.get(i);
                if (!chatMessage.isUser()) {
                    // Обновляем только текст сообщения без создания нового объекта
                    chatMessage.setText(message);
                    
                    // Обновляем только текст без полной перерисовки элемента
                    RecyclerView.ViewHolder holder = chatRecyclerView.findViewHolderForAdapterPosition(i);
                    if (holder != null && holder instanceof ChatAdapter.ChatViewHolder) {
                        ChatAdapter.ChatViewHolder chatViewHolder = (ChatAdapter.ChatViewHolder) holder;
                        if (chatViewHolder.messageText.getTag() == null || !chatViewHolder.messageText.getTag().equals(message)) {
                            markwon.setMarkdown(chatViewHolder.messageText, message);
                            chatViewHolder.messageText.setTag(message);
                        }
                        
                        // Обновляем видимость прогресс-бара
                        boolean isAnalyzing = message.startsWith("Анализирую");
                        chatViewHolder.messageProgress.setVisibility(isAnalyzing ? View.VISIBLE : View.GONE);
                    } else {
                        // Используем более эффективный частичный способ обновления
                        chatAdapter.notifyItemChanged(i, message);
                    }
                    
                    messageUpdated = true;
                    break;
                }
            }
            
            // Если не нашли сообщение для обновления, добавляем новое
            if (!messageUpdated) {
                chatMessages.add(new ChatMessage(message, false));
                chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                // Прокручиваем к новому сообщению без анимации
                chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
            }
        });
    }
} 
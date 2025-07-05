package com.example.emo;

import static com.example.emo.R.id.message_progress;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.example.emo.databinding.FragmentAiPsychologistBinding;
import com.example.emo.firebase.FirebaseDataManager;
import com.example.emo.openai.ApiClient;
import com.example.emo.db.AppDatabase;
import com.example.emo.db.ChatMessageDao;
import com.example.emo.db.ChatMessageEntity;

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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class AiPsychologistFragment extends Fragment {

    private FragmentAiPsychologistBinding binding;
    private RecyclerView chatRecyclerView;
    private Button analyzeButton;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private static final String TAG = "AiPsychologistFragment";
    private ProgressBar progressBar;
    private Markwon markwon;
    private static final int MAX_SAVED_MESSAGES = 5; // Максимальное количество сохраняемых анализов
    private AppDatabase db;
    private ChatMessageDao chatMessageDao;
    private static final long ANALYSIS_COOLDOWN = 30000; // 30 секунд
    private long lastAnalysisTime = 0;
    private android.os.CountDownTimer cooldownTimer;
    private boolean isAnalysisInProgress = false; // Флаг для отслеживания состояния анализа

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentAiPsychologistBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Инициализация базы данных
        db = AppDatabase.getInstance(requireContext());
        chatMessageDao = db.chatMessageDao();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        Log.d(TAG, "onViewCreated вызван");

        // Включаем обработку меню
        setHasOptionsMenu(true);

        // Инициализация Markwon для отображения Markdown
        markwon = Markwon.builder(requireContext())
                .usePlugin(CorePlugin.create())
                .usePlugin(ImagesPlugin.create())
                .usePlugin(TablePlugin.create(requireContext()))
                .usePlugin(HtmlPlugin.create())
                .usePlugin(StrikethroughPlugin.create())
                .build();

        // Инициализация views
        chatRecyclerView = binding.chatRecyclerView;
        if (chatRecyclerView == null) {
            Log.e(TAG, "chatRecyclerView is null!");
            return;
        }
        Log.d(TAG, "chatRecyclerView успешно инициализирован");

        analyzeButton = binding.analyzeButton;
        progressBar = binding.progressBar;

        // Инициализация списка сообщений
        chatMessages = new ArrayList<>();
        chatMessages.add(new ChatMessage("Здравствуйте! Я ваш ИИ-психолог. Нажмите на кнопку \"Анализировать результаты\", чтобы я проанализировал ваши тесты.", false));
        Log.d(TAG, "Добавлено первое сообщение в список");

        // Настройка RecyclerView
        chatAdapter = new ChatAdapter(chatMessages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);
        Log.d(TAG, "RecyclerView настроен с адаптером");

        // Проверяем, что все настроено правильно
        if (chatAdapter.getItemCount() > 0) {
            Log.d(TAG, "В адаптере есть " + chatAdapter.getItemCount() + " элементов");
        } else {
            Log.e(TAG, "Адаптер пуст!");
        }

        // Обработчик нажатия на кнопку анализа
        analyzeButton.setOnClickListener(v -> {
            if (isAnalysisInProgress) {
                Toast.makeText(requireContext(), 
                    "Пожалуйста, дождитесь завершения текущего анализа", 
                    Toast.LENGTH_SHORT).show();
                return;
            }

            long currentTime = System.currentTimeMillis();
            long timeElapsed = currentTime - lastAnalysisTime;

            if (timeElapsed < ANALYSIS_COOLDOWN) {
                long remainingSeconds = (ANALYSIS_COOLDOWN - timeElapsed) / 1000;
                Toast.makeText(requireContext(), 
                    "Подождите " + remainingSeconds + " секунд перед следующим анализом", 
                    Toast.LENGTH_SHORT).show();
                return;
            }

            // Запускаем анализ
            lastAnalysisTime = currentTime;
            isAnalysisInProgress = true;
            updateButtonState();
            
            // Запускаем таймер для обратного отсчета
            if (cooldownTimer != null) {
                cooldownTimer.cancel();
            }
            
            cooldownTimer = new android.os.CountDownTimer(ANALYSIS_COOLDOWN, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    if (analyzeButton != null && !isAnalysisInProgress) {
                        updateButtonState();
                    }
                }

                @Override
                public void onFinish() {
                    if (analyzeButton != null && !isAnalysisInProgress) {
                        updateButtonState();
                    }
                }
            }.start();

            analyzeTestResults();
        });
        
        // Принудительно обновляем список
        chatAdapter.notifyDataSetChanged();
        chatRecyclerView.post(() -> {
            chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
            Log.d(TAG, "Выполнен скролл к последнему сообщению");
        });

        // Загружаем сохраненные сообщения
        loadSavedMessages();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.ai_psychologist_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_clear_history) {
            showClearHistoryDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showClearHistoryDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Очистить историю")
                .setMessage("Вы уверены, что хотите удалить всю историю сообщений?")
                .setPositiveButton("Да", (dialog, which) -> clearHistory())
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void clearHistory() {
        new Thread(() -> {
            // Очищаем базу данных
            chatMessageDao.deleteAll();
            
            // Обновляем UI в главном потоке
            requireActivity().runOnUiThread(() -> {
                // Очищаем список сообщений, оставляя только приветственное
                chatMessages.clear();
                chatMessages.add(new ChatMessage("Здравствуйте! Я ваш ИИ-психолог. Нажмите на кнопку \"Анализировать результаты\", чтобы я проанализировал ваши тесты.", false));
                chatAdapter.notifyDataSetChanged();
                // Прокручиваем к началу
                chatRecyclerView.scrollToPosition(0);
            });
        }).start();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void analyzeTestResults() {
        Log.d(TAG, "Запуск метода analyzeTestResults");

        // Проверяем подключение к интернету
        if (!isNetworkAvailable()) {
            Log.d(TAG, "Отсутствует подключение к интернету");
            chatMessages.add(new ChatMessage("Отсутствует подключение к интернету. Пожалуйста, проверьте ваше соединение и попробуйте снова.", false));
            chatAdapter.notifyItemInserted(chatMessages.size() - 1);
            chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
            
            // Сбрасываем состояние
            isAnalysisInProgress = false;
            lastAnalysisTime = 0;
            if (cooldownTimer != null) {
                cooldownTimer.cancel();
            }
            updateButtonState();
            return;
        }

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

                    Log.d(TAG, "Данные для анализа готовы. Количество результатов: " + testResults.size());

                    // Формируем системный промпт в зависимости от наличия тестов
                    String systemPrompt;
                    if (testResults.isEmpty()) {
                        Log.d(TAG, "Результаты тестов отсутствуют, формируем промпт для мотивации");
                        // Промпт для мотивации на прохождение тестов САН
                        systemPrompt = "Ты психолог, который объясняет важность тестов САН. " +
                                "Объясни пользователю " + username + ", почему регулярное прохождение тестов помогает: " +
                                "1) Отслеживать эмоциональные паттерны " +
                                "2) Улучшать самосознание " +
                                "3) Создавать основу для персонализированных рекомендаций. " +
                                "Используй поддерживающий тон и приведи примеры из практики.";
                    } else {
                        Log.d(TAG, "Результаты тестов есть, формируем промпт для анализа");
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
                        Log.d(TAG, "Получено потоковое обновление: " + partialResponse);
                        requireActivity().runOnUiThread(() -> {
                            updateStatusMessage(partialResponse);
                        });
                    });

                    // Отправляем запрос к API
                    ApiClient.sendChatRequest(systemPrompt, jsonData)
                            .thenAccept(response -> {
                                Log.d(TAG, "Получен финальный ответ от API: " + response);
                                requireActivity().runOnUiThread(() -> {
                                    // Обновляем состояние после получения ответа
                                    isAnalysisInProgress = false;
                                    updateButtonState();
                                    chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                                });
                            })
                            .exceptionally(e -> {
                                Log.e(TAG, "Ошибка при получении ответа от API", e);
                                requireActivity().runOnUiThread(() -> {
                                    String errorMessage = e.getMessage();
                                    if (e instanceof CompletionException && e.getCause() != null) {
                                        errorMessage = e.getCause().getMessage();
                                    }

                                    String formattedError = "### Ошибка при получении ответа\n\n" +
                                            errorMessage + "\n\n" +
                                            "Попробуйте повторить запрос позже или проверить подключение к интернету.";

                                    // Обновляем сообщение о статусе анализа
                                    for (int i = chatMessages.size() - 1; i >= 0; i--) {
                                        ChatMessage chatMessage = chatMessages.get(i);
                                        if (!chatMessage.isUser() && chatMessage.getText().contains("Анализирую")) {
                                            chatMessages.set(i, new ChatMessage(formattedError, false));
                                            chatAdapter.notifyItemChanged(i);
                                            break;
                                        }
                                    }

                                    // Сбрасываем состояние при ошибке
                                    isAnalysisInProgress = false;
                                    lastAnalysisTime = 0;
                                    if (cooldownTimer != null) {
                                        cooldownTimer.cancel();
                                    }
                                    updateButtonState();
                                });
                                return null;
                            });
                })
                .exceptionally(e -> {
                    Log.e(TAG, "Ошибка при получении данных", e);
                    requireActivity().runOnUiThread(() -> {
                        chatMessages.add(new ChatMessage("Произошла ошибка при получении данных: " + e.getMessage() +
                                "\n\nПопробуйте перезапустить приложение или проверить подключение к интернету.", false));
                        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                        chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
                    });
                    return null;
                });
    }

    private void loadSavedMessages() {
        new Thread(() -> {
            List<ChatMessageEntity> savedMessages = chatMessageDao.getLastMessages(MAX_SAVED_MESSAGES);
            // Преобразуем и добавляем сообщения в обратном порядке (от старых к новым)
            List<ChatMessage> messages = new ArrayList<>();
            for (int i = savedMessages.size() - 1; i >= 0; i--) {
                ChatMessageEntity entity = savedMessages.get(i);
                messages.add(new ChatMessage(entity.getText(), entity.isUser()));
            }
            
            requireActivity().runOnUiThread(() -> {
                chatMessages.clear();
                // Добавляем приветственное сообщение, если нет сохраненных
                if (messages.isEmpty()) {
                    chatMessages.add(new ChatMessage("Здравствуйте! Я ваш ИИ-психолог. Нажмите на кнопку \"Анализировать результаты\", чтобы я проанализировал ваши тесты.", false));
                } else {
                    chatMessages.addAll(messages);
                }
                chatAdapter.notifyDataSetChanged();
                chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
            });
        }).start();
    }

    private void saveMessage(ChatMessage message) {
        new Thread(() -> {
            ChatMessageEntity entity = new ChatMessageEntity(message.getText(), message.isUser());
            chatMessageDao.insertAndMaintainLimit(entity, MAX_SAVED_MESSAGES);
        }).start();
    }

    private void updateStatusMessage(String message) {
        requireActivity().runOnUiThread(() -> {
            boolean messageFound = false;
            for (int i = chatMessages.size() - 1; i >= 0; i--) {
                ChatMessage chatMessage = chatMessages.get(i);
                if (!chatMessage.isUser()) {
                    if (!chatMessage.getText().equals(message)) {
                        boolean oldIsAnalyzing = chatMessage.getText().startsWith("Анализирую");
                        boolean newIsAnalyzing = message.startsWith("Анализирую");
                        
                        if (!(oldIsAnalyzing && newIsAnalyzing)) {
                            chatMessage.setText(message);
                            chatAdapter.notifyItemChanged(i, message);
                            
                            // Сохраняем сообщение только если это не промежуточное "Анализирую"
                            if (!newIsAnalyzing) {
                                saveMessage(chatMessage);
                            }
                        }
                    }
                    messageFound = true;
                    break;
                }
            }

            if (!messageFound) {
                ChatMessage newMessage = new ChatMessage(message, false);
                chatMessages.add(newMessage);
                chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                
                // Сохраняем новое сообщение, если это не "Анализирую"
                if (!message.startsWith("Анализирую")) {
                    saveMessage(newMessage);
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        if (cooldownTimer != null) {
            cooldownTimer.cancel();
            cooldownTimer = null;
        }
        isAnalysisInProgress = false;
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
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position, @NonNull List<Object> payloads) {
            if (!payloads.isEmpty()) {
                // Если есть payload, обновляем только текст
                String newText = (String) payloads.get(0);
                if (holder.messageText.getTag() == null || !holder.messageText.getTag().equals(newText)) {
                    markwon.setMarkdown(holder.messageText, newText);
                    holder.messageText.setTag(newText);
                }
                
                // Обновляем progressBar только если это действительно необходимо
                boolean shouldShowProgress = newText.startsWith("Анализирую");
                if ((shouldShowProgress && holder.messageProgress.getVisibility() != View.VISIBLE) ||
                    (!shouldShowProgress && holder.messageProgress.getVisibility() != View.GONE)) {
                    holder.messageProgress.setVisibility(shouldShowProgress ? View.VISIBLE : View.GONE);
                }
                return;
            }
            
            // Если нет payload, выполняем полное обновление
            onBindViewHolder(holder, position);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            ChatMessage message = messages.get(position);
            
            // Применяем Markdown форматирование к тексту сообщения
            if (!message.isUser()) {
                if (holder.messageText.getTag() == null || !holder.messageText.getTag().equals(message.getText())) {
                    markwon.setMarkdown(holder.messageText, message.getText());
                    holder.messageText.setTag(message.getText());
                    holder.messageText.setMovementMethod(LinkMovementMethod.getInstance());
                }
                
                boolean shouldShowProgress = message.getText().startsWith("Анализирую");
                if ((shouldShowProgress && holder.messageProgress.getVisibility() != View.VISIBLE) ||
                    (!shouldShowProgress && holder.messageProgress.getVisibility() != View.GONE)) {
                    holder.messageProgress.setVisibility(shouldShowProgress ? View.VISIBLE : View.GONE);
                }
            } else {
                if (holder.messageText.getTag() == null || !holder.messageText.getTag().equals(message.getText())) {
                    holder.messageText.setText(message.getText());
                    holder.messageText.setTag(message.getText());
                }
                if (holder.messageProgress.getVisibility() != View.GONE) {
                    holder.messageProgress.setVisibility(View.GONE);
                }
            }
            
            // Настройка внешнего вида только если не применялись стили
            if (holder.isStyleApplied == null || !holder.isStyleApplied) {
                View parentContainer = (View) holder.messageText.getParent();
                
                if (message.isUser()) {
                    parentContainer.setBackgroundResource(R.drawable.bg_user_message);
                    holder.messageText.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
                    parentContainer.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                } else {
                    parentContainer.setBackgroundResource(R.drawable.bg_ai_message);
                    holder.messageText.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
                    parentContainer.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
                }
                holder.isStyleApplied = true;
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
                messageProgress = itemView.findViewById(R.id.message_progress);
            }
        }

        public void updateMessages(List<ChatMessage> newMessages) {
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ChatMessageDiffCallback(messages, newMessages));
            messages.clear();
            messages.addAll(newMessages);
            diffResult.dispatchUpdatesTo(this);
        }
    }

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

    private void updateButtonState() {
        if (analyzeButton == null) return;
        
        long currentTime = System.currentTimeMillis();
        long timeElapsed = currentTime - lastAnalysisTime;
        boolean isInCooldown = timeElapsed < ANALYSIS_COOLDOWN;
        
        analyzeButton.setEnabled(!isAnalysisInProgress && !isInCooldown);
        
        if (isAnalysisInProgress) {
            analyzeButton.setText("ИДЕТ АНАЛИЗ...");
        } else if (isInCooldown) {
            long remainingSeconds = (ANALYSIS_COOLDOWN - timeElapsed) / 1000;
            analyzeButton.setText("ПОДОЖДИТЕ " + remainingSeconds + " СЕК");
        } else {
            analyzeButton.setText("АНАЛИЗИРОВАТЬ МОИ РЕЗУЛЬТАТЫ");
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        ApiClient.setStreamListener(null);
        ApiClient.cancelAllRequests();
    }
}
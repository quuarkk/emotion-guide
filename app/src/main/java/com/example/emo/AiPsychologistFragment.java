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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
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

import com.example.emo.models.ChatMessage;
import com.example.emo.utils.SharedPreferencesManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

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
import java.util.concurrent.CompletableFuture;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.lifecycle.ViewModelProvider;
import com.example.emo.viewmodels.AiPsychologistViewModel;

import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Locale;

// Вспомогательный класс для хранения информации о времени и сезоне
class TimeAndSeasonData {
    public final String timeOfDay;
    public final String season;
    public final String timestamp;

    public TimeAndSeasonData(String timeOfDay, String season, String timestamp) {
        this.timeOfDay = timeOfDay;
        this.season = season;
        this.timestamp = timestamp;
    }
}

public class AiPsychologistFragment extends Fragment {

    private FragmentAiPsychologistBinding binding;
    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private Button analyzeButton;
    private ProgressBar progressBar;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messages = new ArrayList<>();
    private static final String TAG = "AiPsychologistFragment";
    private Markwon markwon;
    private static final int MAX_SAVED_MESSAGES = 50; // Максимальное количество сохраняемых сообщений
    private AppDatabase db;
    private ChatMessageDao chatMessageDao;
    private static final long ANALYSIS_COOLDOWN = 30000; // 30 секунд
    private long lastAnalysisTime = 0;
    private android.os.CountDownTimer cooldownTimer;
    private boolean isAnalysisInProgress = false; // Флаг для отслеживания состояния анализа
    private CompletableFuture<String> currentRequest;
    private AiPsychologistViewModel viewModel;

    private TimeAndSeasonData getTimeAndSeasonData() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int month = calendar.get(Calendar.MONTH);

        // Определение времени суток
        String timeOfDay;
        if (hour >= 6 && hour < 12) {
            timeOfDay = "утро";
        } else if (hour >= 12 && hour < 18) {
            timeOfDay = "день";
        } else if (hour >= 18 && hour < 23) {
            timeOfDay = "вечер";
        } else {
            timeOfDay = "ночь";
        }

        // Определение сезона
        String season;
        if (month >= Calendar.DECEMBER || month <= Calendar.FEBRUARY) {
            season = "зима";
        } else if (month >= Calendar.MARCH && month <= Calendar.MAY) {
            season = "весна";
        } else if (month >= Calendar.JUNE && month <= Calendar.AUGUST) {
            season = "лето";
        } else {
            season = "осень";
        }

        // Форматирование временной метки
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String timestamp = sdf.format(calendar.getTime());

        return new TimeAndSeasonData(timeOfDay, season, timestamp);
    }

    private void updateStatusMessage(String message) {
        // Обрабатываем теги <think> в ответе
        String processedMessage = message;
        int thinkEndIndex = processedMessage.indexOf("</think>");
        if (thinkEndIndex != -1) {
            processedMessage = processedMessage.substring(thinkEndIndex + "</think>".length()).trim();
            // Если после удаления тегов ничего не осталось, не обновляем сообщение
            if (processedMessage.isEmpty()) {
                return;
            }
        } else {
            int thinkStartIndex = processedMessage.indexOf("<think>");
            if (thinkStartIndex != -1) {
                // Если есть только открывающий тег, не показываем ничего
                return;
            }
        }
        
        // Если сообщение содержит "Анализирую" и это не финальный ответ, не обновляем
        if (processedMessage.contains("Анализирую") && !processedMessage.trim().endsWith(".")) {
            return;
        }
        
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage chatMessage = messages.get(i);
            if (chatMessage.getType() == ChatMessage.TYPE_AI && 
                    (chatMessage.getContent().contains("Анализирую") || chatMessage.isLoading())) {
                messages.set(i, new ChatMessage(processedMessage, ChatMessage.TYPE_AI));
                chatAdapter.notifyItemChanged(i);
                break;
            }
        }
    }

    private void analyzeTestResults() {
        Log.d(TAG, "Запуск метода analyzeTestResults");

        if (!isNetworkAvailable()) {
            Log.d(TAG, "Отсутствует подключение к интернету");
            messages.add(new ChatMessage("Отсутствует подключение к интернету. Пожалуйста, проверьте ваше соединение и попробуйте снова.", ChatMessage.TYPE_AI));
            chatAdapter.notifyItemInserted(messages.size() - 1);
            chatRecyclerView.scrollToPosition(messages.size() - 1);

            isAnalysisInProgress = false;
            lastAnalysisTime = 0;
            if (cooldownTimer != null) {
                cooldownTimer.cancel();
            }
            updateButtonState();
            return;
        }

        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage message = messages.get(i);
            if (message.getType() == ChatMessage.TYPE_AI && message.getContent().startsWith("Анализирую")) {
                messages.remove(i);
                chatAdapter.notifyItemRemoved(i);
                break;
            }
        }

        messages.add(new ChatMessage("⌛ Пожалуйста, подождите...", ChatMessage.TYPE_AI));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        chatRecyclerView.scrollToPosition(messages.size() - 1);

        // Получаем данные о времени и сезоне
        TimeAndSeasonData timeData = getTimeAndSeasonData();

        FirebaseDataManager.getUserName()
                .thenCompose(username -> {
                    Log.d(TAG, "Получено имя пользователя: " + username);
                    return FirebaseDataManager.getUserTestResults()
                            .thenApply(testResults -> {
                                Log.d(TAG, "Получено результатов тестов: " + testResults.size());
                                // Создаем объект с данными о времени и сезоне
                                JSONObject timeDataJson = new JSONObject();
                                try {
                                    timeDataJson.put("timeOfDay", timeData.timeOfDay);
                                    timeDataJson.put("season", timeData.season);
                                    timeDataJson.put("timestamp", timeData.timestamp);
                                } catch (Exception e) {
                                    Log.e(TAG, "Ошибка при создании JSON с данными о времени", e);
                                }
                                JSONObject data = FirebaseDataManager.prepareTestDataForAI(testResults, username);
                                // Добавляем данные о времени в основной JSON
                                try {
                                    data.put("timeData", timeDataJson);
                                } catch (Exception e) {
                                    Log.e(TAG, "Ошибка при добавлении данных о времени в JSON", e);
                                }
                                return new Pair<>(testResults, data.toString(), username);
                            });
                })
                .thenAccept(triple -> {
                    List<FirebaseDataManager.TestResult> testResults = triple.first;
                    String jsonData = triple.second;
                    String username = triple.third;

                    Log.d(TAG, "Данные для анализа готовы. Количество результатов: " + testResults.size());
                    Log.d(TAG, "Формирование промта: username=" + username + ", season=" + timeData.season + ", timeOfDay=" + timeData.timeOfDay + ", timestamp=" + timeData.timestamp);

                    String systemPrompt;
                    if (testResults.isEmpty()) {
                        Log.d(TAG, "Результаты тестов отсутствуют, формируем промпт для мотивации");
                        systemPrompt = String.format(
                                "Ты — эмпатичный ИИ-психолог в приложении для ментального благополучия, созданного для русскоязычной аудитории 18–35 лет. " +
                                        "Объясни пользователю %s в тёплом, поддерживающем стиле (300–400 слов), почему регулярное прохождение тестов САН (самочувствие, активность, настроение) важно:\n" +
                                        "1) Отслеживание эмоционального состояния (например, как стресс влияет на настроение).\n" +
                                        "2) Повышение самосознания (понимание своих эмоций).\n" +
                                        "3) Получение персонализированных рекомендаций для заботы о себе.\n" +
                                        "Учти время года (%s) и время суток (%s) для примеров (например, прогулка летом, тёплый чай зимним вечером). " +
                                        "Используй тёплый, нейтральный тон, как у заботливого друга. Избегай названий городов и упоминаний часового пояса, ссылайся только на время года и суток. " +
                                        "Добавляй не более 4–5 эмодзи (😊, 🌟, 🛌, ☀️, 🌙) для мягкости и выразительности. Ссылайся на графики в приложении. " +
                                        "Завершай вдохновляющей фразой (например, 'Каждый шаг к себе — это прогресс! 🌟'). " +
                                        "Избегай медицинских терминов и диагнозов.",
                                username, timeData.season, timeData.timeOfDay
                        );
                    } else {
                        Log.d(TAG, "Результаты тестов есть, формируем промпт для анализа");
                        systemPrompt = String.format(
                                "Ты — trauma-informed ИИ-ассистент в приложении для ментального благополучия, созданного для русскоязычной аудитории 18–35 лет. " +
                                        "Анализируй результаты теста САН (самочувствие, активность, настроение) для пользователя %s и предоставь эмпатичный, поддерживающий анализ (300–400 слов). " +
                                        "Все рекомендации ИСКЛЮЧИТЕЛЬНО рекомендательные, медицинские диагнозы строго запрещены. Пользователь не может вести диалог, только запросить повторный анализ. " +
                                        "Учитывай время года (%s: дек–фев — зима, мар–май — весна, июн–авг — лето, сен–ноя — осень) и время суток (%s: утро 06:00–12:00, день 12:00–18:00, вечер 18:00–23:00, ночь 23:00–06:00) для релевантных рекомендаций. " +
                                        "Избегай упоминаний часового пояса или названий городов, ссылайся только на время года и суток. Ссылайся на графики самочувствия, активности и настроения в приложении. " +
                                        "Для разнообразия варьируй формулировки и приветствия. Следуй этим шагам:\n" +
                                        "1. **Анализ данных теста САН**:\n" +
                                        "   - Оцени до 25 последних результатов теста САН (или все доступные, если их меньше). Шкала: 1–7 (1–3 — низкие, возможный дискомфорт; 4 — нейтральные; 5–7 — высокие, норма).\n" +
                                        "   - Сравни текущие показатели с нормой (4–7).\n" +
                                        "   - Опиши динамику изменений на графиках приложения (улучшение 😊, ухудшение 😔, стабильность ➡️), если есть предыдущие тесты, с вариативными формулировками (например, 'настроение немного подросло' или 'самочувствие стало ниже').\n" +
                                        "   - Если есть поле 'note', кратко упомяни его в анализе.\n" +
                                        "2. **Trauma-informed интерпретация**:\n" +
                                        "   - Опиши эмоциональное состояние в тёплом, поддерживающем стиле, как у заботливого друга (например, 'похоже, день был непростым' или 'ты в хорошем ресурсе'). Низкие значения (≤3) рассматривай как сигнал возможного эмоционального дискомфорта, но избегай медицинских терминов.\n" +
                                        "   - Учти возможный стресс от учёбы, работы или социальной жизни, но не предполагай причин без данных.\n" +
                                        "3. **Рекомендации**:\n" +
                                        "   - Выбери 2–3 практики из пула, адаптированные к состоянию, времени года и суток. Вариируй формулировки для естественности. Пул практик:\n" +
                                        "     - **Низкие показатели (≤3)**:\n" +
                                        "       - Ночь: лечь спать (🛌), расслабляющая музыка, дыхательные упражнения (4-4-6 или квадратное дыхание).\n" +
                                        "       - Утро: лёгкая зарядка (☀️), дыхательное упражнение для бодрости, постановка небольшой цели.\n" +
                                        "       - День: техника grounding (5-4-3-2-1), прогулка, замена негативной мысли (например, 'я не справлюсь' на 'я делаю, что могу').\n" +
                                        "       - Вечер: письмо себе, лёгкая растяжка, тёплый чай (🌙).\n" +
                                        "       - Зима/вечер/ночь: тёплая ванна, горячий чай.\n" +
                                        "       - Лето/утро/день: прогулка, лёгкая физическая активность.\n" +
                                        "     - **Нейтральные/высокие (≥4)**:\n" +
                                        "       - Ночь: чтение книги (🛌), короткая медитация перед сном, расслабляющая музыка.\n" +
                                        "       - Утро: дневник благодарности (☀️), постановка цели на день, прослушивание любимой музыки.\n" +
                                        "       - День: творческое занятие (рисование, заметки), прогулка, беседа с другом.\n" +
                                        "       - Вечер: просмотр вдохновляющего видео, дневник благодарности, лёгкая растяжка (🌙).\n" +
                                        "     - **Общие, с учётом времени года/суток**: прогулка (летом/утром/днем), танцы под музыку, постановка небольшой цели (например, 'выучить 5 новых слов'), чтение книги (зимой/вечером/ночь), беседа с другом, создание коллажа идей (весной/днем).\n" +
                                        "   - Для ночного времени (23:00–06:00) с низкими показателями (≤3) приоритетно предлагай лечь спать, предполагая, что пользователь не работает ночью. " +
                                        "   - Привяжи рекомендации к показателям, времени года и суток (например, 'летней ночью попробуй лечь спать пораньше 🛌' или 'утром попробуй лёгкую зарядку ☀️').\n" +
                                        "4. **Формат ответа**:\n" +
                                        "   - Используй тёплый, нейтральный тон, как у заботливого друга, с вариативными приветствиями (например, 'Здравствуйте, %s, давайте посмотрим на ваши результаты? 😊' или 'Привет, %s, как дела сегодня? 🌟').\n" +
                                        "   - Структура:\n" +
                                        "     А) Анализ текущих результатов и динамики на графиках с эмодзи (😊, 😔, ➡️, не более 4–5 в тексте).\n" +
                                        "     Б) 2–3 рекомендации, привязанные к показателям, времени года и суток.\n" +
                                        "     В) Вдохновляющая фраза (например, 'Ты делаешь важный шаг для себя! 🌟' или 'Каждый день — новая возможность! ☀️').\n" +
                                        "   - Используй не более 4–5 эмодзи (😊, 😔, ➡️, 🌟, 🛌, ☀️, 🌙) для мягкости и выразительности. Избегай медицинских терминов, диагнозов и сложного языка. Подчеркивай эмоциональную безопасность.\n" +
                                        "Пример ввода: Самочувствие: 3, Активность: 4, Настроение: 2, Note: 'только текущий тест', Время: %s, сезон: %s.",
                                username, timeData.season, timeData.timeOfDay, username, username, timeData.timestamp, timeData.season
                        );
                    }

                    int systemPromptTokens = systemPrompt.length() / 4;
                    int dataTokens = jsonData.length() / 4;
                    int totalRequestTokens = systemPromptTokens + dataTokens;

                    Log.d(TAG, "Размер системного промпта: " + systemPrompt.length() + " символов (~" + systemPromptTokens + " токенов)");
                    Log.d(TAG, "Размер данных пользователя: " + jsonData.length() + " символов (~" + dataTokens + " токенов)");
                    Log.d(TAG, "Общий размер запроса: ~" + totalRequestTokens + " токенов");

                    Log.d(TAG, "Отправка запроса к API с системным промптом");

                    ApiClient.setStreamListener(partialResponse -> {
                        Log.d(TAG, "Получено потоковое обновление: " + partialResponse);
                        requireActivity().runOnUiThread(() -> {
                            updateStatusMessage(partialResponse);
                        });
                    });

                    ApiClient.sendChatRequest(systemPrompt, jsonData)
                            .thenAccept(response -> {
                                Log.d(TAG, "Получен финальный ответ от API: " + response);
                                requireActivity().runOnUiThread(() -> {
                                    isAnalysisInProgress = false;
                                    updateButtonState();
                                    chatRecyclerView.scrollToPosition(messages.size() - 1);
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

                                    for (int i = messages.size() - 1; i >= 0; i--) {
                                        ChatMessage chatMessage = messages.get(i);
                                        if (chatMessage.getType() == ChatMessage.TYPE_AI && chatMessage.getContent().contains("Анализирую")) {
                                            messages.set(i, new ChatMessage(formattedError, ChatMessage.TYPE_AI));
                                            chatAdapter.notifyItemChanged(i);
                                            break;
                                        }
                                    }

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
                        messages.add(new ChatMessage("Произошла ошибка при получении данных: " + e.getMessage() +
                                "\n\nПопробуйте перезапустить приложение или проверить подключение к интернету.", ChatMessage.TYPE_AI));
                        chatAdapter.notifyItemInserted(messages.size() - 1);
                        chatRecyclerView.smoothScrollToPosition(messages.size() - 1);
                    });
                    return null;
                });
    }

    private static final String SYSTEM_PROMPT = "Ты - эмпатичный и профессиональный психолог-консультант. " +
            "Твоя задача - помогать пользователям справляться с их эмоциональными проблемами, " +
            "давать поддержку и практические советы. Отвечай кратко, но информативно, " +
            "используя простой и понятный язык. Избегай длинных теоретических объяснений.";

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
        
        // Инициализация ViewModel
        viewModel = new ViewModelProvider(this).get(AiPsychologistViewModel.class);
        
        // Инициализация базы данных
        db = AppDatabase.getInstance(requireContext());
        chatMessageDao = db.chatMessageDao();

        // Всегда загружаем сообщения из базы данных при создании фрагмента
        loadSavedMessages();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        Log.d(TAG, "onViewCreated вызван");

        // Скрываем нижнюю панель навигации
        BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setVisibility(View.GONE);
        }

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
        messageInput = binding.messageInput;
        sendButton = binding.sendButton;
        analyzeButton = binding.analyzeButton;
        progressBar = binding.progressBar;

        // Настройка RecyclerView
        chatAdapter = new ChatAdapter(messages);
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

        // Обработчики нажатий
        sendButton.setOnClickListener(v -> sendMessage());
        analyzeButton.setOnClickListener(v -> analyzeResults());

        // Настройка наблюдателей
        viewModel.getMessages().observe(getViewLifecycleOwner(), newMessages -> {
            messages.clear();
            messages.addAll(newMessages);
            if (chatAdapter != null) {
                chatAdapter.notifyDataSetChanged();
                chatRecyclerView.scrollToPosition(messages.size() - 1);
            }
        });

        viewModel.getIsRequestActive().observe(getViewLifecycleOwner(), active -> {
            setInputEnabled(!active);
        });

        setupStreamListener();
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
            
            // Создаем приветственное сообщение
            ChatMessage welcomeMessage = new ChatMessage("Здравствуйте! Я ваш ИИ-психолог. Нажмите на кнопку \"Анализировать результаты\", чтобы я проанализировал ваши тесты.", ChatMessage.TYPE_AI);
            
            // Сохраняем приветственное сообщение в базу данных
            ChatMessageEntity entity = new ChatMessageEntity(welcomeMessage.getContent(), false);
            chatMessageDao.insert(entity);
            
            // Обновляем UI в главном потоке
            requireActivity().runOnUiThread(() -> {
                // Очищаем список сообщений, оставляя только приветственное
                messages.clear();
                messages.add(welcomeMessage);
                chatAdapter.notifyDataSetChanged();
                // Прокручиваем к началу
                chatRecyclerView.scrollToPosition(0);
                
                // Обновляем ViewModel
                List<ChatMessage> newMessages = new ArrayList<>();
                newMessages.add(welcomeMessage);
                viewModel.setMessages(newMessages);
            });
        }).start();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void setInputEnabled(boolean enabled) {
        if (sendButton != null) {
            sendButton.setEnabled(enabled);
            sendButton.setAlpha(enabled ? 1.0f : 0.5f);
        }
        if (analyzeButton != null) {
            analyzeButton.setEnabled(enabled);
            analyzeButton.setAlpha(enabled ? 1.0f : 0.5f);
        }
        if (messageInput != null) {
            messageInput.setEnabled(enabled);
            messageInput.setAlpha(enabled ? 1.0f : 0.5f);
        }
    }

    private void sendMessage() {
        String messageText = messageInput.getText().toString().trim();
        if (messageText.isEmpty() || viewModel.getIsRequestActive().getValue() == Boolean.TRUE) return;

        // Создаем и сохраняем сообщение пользователя
        ChatMessage userMessage = new ChatMessage(messageText, ChatMessage.TYPE_USER);
        viewModel.addMessage(userMessage);
        saveMessage(userMessage); // Сохраняем сообщение пользователя

        // Добавляем сообщение ИИ с индикатором загрузки
        ChatMessage aiMessage = new ChatMessage("⌛ Пожалуйста, подождите...", ChatMessage.TYPE_AI);
        aiMessage.setLoading(true);
        viewModel.addMessage(aiMessage);

        // Очищаем поле ввода
        messageInput.setText("");

        // Формируем историю сообщений для контекста
        StringBuilder conversationHistory = new StringBuilder();
        // Ограничиваем историю последними 10 сообщениями
        int startIndex = Math.max(0, messages.size() - 10);
        for (int i = startIndex; i < messages.size(); i++) {
            ChatMessage msg = messages.get(i);
            if (msg.getType() == ChatMessage.TYPE_USER) {
                conversationHistory.append("User: ").append(msg.getContent()).append("\n");
            } else if (!msg.isLoading()) {
                conversationHistory.append("Assistant: ").append(msg.getContent()).append("\n");
            }
        }

        // Отправляем запрос
        viewModel.setRequestActive(true);
        currentRequest = ApiClient.sendChatRequest(SYSTEM_PROMPT + "\n\nПредыдущий разговор:\n" + conversationHistory.toString(),
                messageText);

        currentRequest.whenComplete((response, error) -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (error != null) {
                        Toast.makeText(getContext(), "Произошла ошибка: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        viewModel.removeLastMessage();
                    }
                    // В любом случае разблокируем интерфейс
                    viewModel.setRequestActive(false);
                });
            }
        });
    }

    private void analyzeResults() {
        if (viewModel.getIsRequestActive().getValue() == Boolean.TRUE) {
            Toast.makeText(getContext(), "Пожалуйста, дождитесь окончания текущего анализа", Toast.LENGTH_SHORT).show();
            return;
        }

        // Блокируем ввод
        setInputEnabled(false);

        // Получаем данные из Firebase
        FirebaseDataManager.getLatestTestResults(results -> {
            if (results == null || results.isEmpty()) {
                // Если в Firebase нет данных, пробуем получить из SharedPreferences
                SharedPreferencesManager prefsManager = new SharedPreferencesManager(requireContext());
                String jsonData = prefsManager.getTestResults();
                if (jsonData == null || jsonData.isEmpty()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Нет данных для анализа", Toast.LENGTH_SHORT).show();
                        setInputEnabled(true);
                    });
                    return;
                }
                processTestResults(jsonData);
            } else {
                // Преобразуем результаты в JSON для анализа
                try {
                    JSONObject jsonResults = new JSONObject(results);
                    processTestResults(jsonResults.toString());
                } catch (Exception e) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Ошибка при обработке данных: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        setInputEnabled(true);
                    });
                }
            }
        });
    }

    private void processTestResults(String jsonData) {
        requireActivity().runOnUiThread(() -> {
            // Добавляем сообщение ИИ с индикатором загрузки
            ChatMessage aiMessage = new ChatMessage("Анализирую ваши результаты...", ChatMessage.TYPE_AI);
            aiMessage.setLoading(true);
            viewModel.addMessage(aiMessage);

            // Ограничиваем размер данных, если они слишком большие
            String optimizedJsonData = jsonData;
            if (jsonData.length() > 15000) {
                optimizedJsonData = jsonData.substring(0, 15000) + "...";
                Log.d(TAG, "Данные для анализа были сокращены с " + jsonData.length() + " до 15000 символов");
            }

            viewModel.setRequestActive(true);
            currentRequest = ApiClient.sendChatRequest(
                    "Ты - эмпатичный и профессиональный психолог. Проанализируй результаты тестов пользователя и дай рекомендации. " +
                            "Обрати внимание на динамику изменений в настроении, самочувствии и активности. " +
                            "Дай конкретные советы по улучшению состояния, если это необходимо.",
                    optimizedJsonData
            );

            currentRequest.whenComplete((response, error) -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (error != null) {
                            Toast.makeText(getContext(), "Произошла ошибка: " + error.getMessage(), Toast.LENGTH_LONG).show();
                            viewModel.removeLastMessage();
                        }
                        // В любом случае разблокируем интерфейс
                        viewModel.setRequestActive(false);
                    });
                }
            });
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        // Сохранение состояния происходит автоматически через ViewModel
    }

    @Override
    public void onStop() {
        super.onStop();
        if (currentRequest != null && !currentRequest.isDone()) {
            currentRequest.cancel(true);
        }
        
        // Сохраняем все текущие сообщения в базу данных
        new Thread(() -> {
            // Сначала очищаем базу данных
            chatMessageDao.deleteAll();
            
            // Затем сохраняем все текущие сообщения
            List<ChatMessage> currentMessages = viewModel.getMessages().getValue();
            if (currentMessages != null && !currentMessages.isEmpty()) {
                for (ChatMessage message : currentMessages) {
                    ChatMessageEntity entity = new ChatMessageEntity(
                            message.getContent(), 
                            message.getType() == ChatMessage.TYPE_USER
                    );
                    chatMessageDao.insert(entity);
                }
            }
        }).start();
    }

    @Override
    public void onDestroyView() {
        if (cooldownTimer != null) {
            cooldownTimer.cancel();
            cooldownTimer = null;
        }
        isAnalysisInProgress = false;
        
        // Восстанавливаем нижнюю панель навигации при выходе из фрагмента
        BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setVisibility(android.view.View.VISIBLE);
        }
        
        super.onDestroyView();
        binding = null;
        // НЕ отменяем текущий запрос и слушателя при уничтожении view
        // ApiClient.setStreamListener(null);
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
                // Обновляем только текст, если это обновление контента
                ChatMessage message = messages.get(position);
                if (message.getType() == ChatMessage.TYPE_AI) {
                    // Применяем Markdown только если текст изменился
                    String newContent = message.getContent();
                    if (holder.messageText.getTag() == null || !holder.messageText.getTag().equals(newContent)) {
                        markwon.setMarkdown(holder.messageText, newContent);
                        holder.messageText.setTag(newContent);
                    }
                    
                    // Обновляем состояние загрузки только если оно изменилось
                    boolean isLoading = message.isLoading();
                    if ((isLoading && holder.messageProgress.getVisibility() != View.VISIBLE) ||
                        (!isLoading && holder.messageProgress.getVisibility() != View.GONE)) {
                        holder.messageProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                    }
                }
                return;
            }
            onBindViewHolder(holder, position);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            ChatMessage message = messages.get(position);
            
            // Настройка стилей сообщения в зависимости от типа
            if (message.getType() == ChatMessage.TYPE_USER) {
                // Сообщение пользователя
                holder.messageCard.setCardBackgroundColor(getResources().getColor(R.color.user_message_background));
                
                // Выравнивание сообщения пользователя справа
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) holder.messageCard.getLayoutParams();
                params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
                params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
                params.horizontalBias = 1.0f; // Смещаем вправо
                holder.messageCard.setLayoutParams(params);
                
                // Простой текст для сообщения пользователя
                holder.messageText.setText(message.getContent());
                holder.messageProgress.setVisibility(View.GONE);
            } else {
                // Сообщение ИИ
                holder.messageCard.setCardBackgroundColor(getResources().getColor(R.color.ai_message_background));
                
                // Выравнивание сообщения ИИ слева
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) holder.messageCard.getLayoutParams();
                params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
                params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
                params.horizontalBias = 0.0f; // Смещаем влево
                holder.messageCard.setLayoutParams(params);
                
                // Markdown форматирование для сообщения ИИ
                markwon.setMarkdown(holder.messageText, message.getContent());
                holder.messageText.setMovementMethod(LinkMovementMethod.getInstance());
                
                // Показываем/скрываем индикатор загрузки
                holder.messageProgress.setVisibility(message.isLoading() ? View.VISIBLE : View.GONE);
            }
            
            // Сохраняем текущий контент как тег
            holder.messageText.setTag(message.getContent());
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        class ChatViewHolder extends RecyclerView.ViewHolder {
            TextView messageText;
            ProgressBar messageProgress;
            CardView messageCard;

            ChatViewHolder(View itemView) {
                super(itemView);
                messageText = itemView.findViewById(R.id.message_text);
                messageProgress = itemView.findViewById(R.id.message_progress);
                messageCard = itemView.findViewById(R.id.message_card);
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

            return oldMessage.getType() == newMessage.getType() &&
                    oldMessage.getContent().equals(newMessage.getContent());
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

    private void setupStreamListener() {
        ApiClient.setStreamListener(partialResponse -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!messages.isEmpty()) {
                        ChatMessage lastMessage = messages.get(messages.size() - 1);
                        if (lastMessage.getType() == ChatMessage.TYPE_AI) {
                            // Проверяем, является ли это промежуточным "размышлением"
                            if (!partialResponse.contains("Анализирую") && !partialResponse.contains("Думаю") && 
                                    !(partialResponse.contains("<think>") && !partialResponse.contains("</think>"))) {
                                
                                // Обрабатываем теги <think> в ответе
                                String processedResponse = partialResponse;
                                int thinkEndIndex = processedResponse.indexOf("</think>");
                                if (thinkEndIndex != -1) {
                                    processedResponse = processedResponse.substring(thinkEndIndex + "</think>".length()).trim();
                                } else {
                                    int thinkStartIndex = processedResponse.indexOf("<think>");
                                    if (thinkStartIndex != -1) {
                                        processedResponse = processedResponse.substring(0, thinkStartIndex).trim();
                                        if (processedResponse.isEmpty()) {
                                            // Если после удаления тегов ничего не осталось, не обновляем сообщение
                                            return;
                                        }
                                    }
                                }
                                
                                String oldContent = lastMessage.getContent();
                                lastMessage.setContent(processedResponse);
                                lastMessage.setLoading(false);
                                
                                // Обновляем только если контент действительно изменился
                                if (!processedResponse.equals(oldContent)) {
                                    viewModel.updateLastMessage(processedResponse, false);
                                    
                                    // Сохраняем все ответы ИИ, а не только финальные
                                    saveMessage(new ChatMessage(processedResponse, ChatMessage.TYPE_AI));
                                    
                                    // Если ответ завершен, сбрасываем флаг активного запроса
                                    if (processedResponse.trim().endsWith(".")) {
                                        viewModel.setRequestActive(false);
                                    }
                                }
                            }
                        }
                    }
                });
            }
        });
    }

    private void loadSavedMessages() {
        new Thread(() -> {
            List<ChatMessageEntity> savedMessages = chatMessageDao.getLastMessages(MAX_SAVED_MESSAGES);
            List<ChatMessage> loadedMessages = new ArrayList<>();
            
            // Добавляем сообщения в том же порядке, в котором они загружены из базы данных
            for (ChatMessageEntity entity : savedMessages) {
                loadedMessages.add(new ChatMessage(entity.getText(), entity.isUser() ? ChatMessage.TYPE_USER : ChatMessage.TYPE_AI));
            }
            
            if (loadedMessages.isEmpty()) {
                // Если сообщений нет, добавляем приветственное сообщение
                ChatMessage welcomeMessage = new ChatMessage("Здравствуйте! Я ваш ИИ-психолог. Нажмите на кнопку \"Анализировать результаты\", чтобы я проанализировал ваши тесты.", ChatMessage.TYPE_AI);
                loadedMessages.add(welcomeMessage);
                
                // И сохраняем его в базу данных
                ChatMessageEntity entity = new ChatMessageEntity(welcomeMessage.getContent(), false);
                chatMessageDao.insert(entity);
            }
            
            // Переносим обновление LiveData в главный поток
            List<ChatMessage> finalLoadedMessages = loadedMessages;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    viewModel.setMessages(finalLoadedMessages);
                });
            }
        }).start();
    }

    private void saveMessage(ChatMessage message) {
        new Thread(() -> {
            ChatMessageEntity entity = new ChatMessageEntity(message.getContent(), message.getType() == ChatMessage.TYPE_USER);
            chatMessageDao.insertAndMaintainLimit(entity, MAX_SAVED_MESSAGES);
        }).start();
    }
}
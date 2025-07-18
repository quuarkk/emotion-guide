package com.example.emo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emo.databinding.FragmentFirstBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// Реализуем listener адаптера
public class FirstFragment extends Fragment implements SanAdapter.OnScoreSelectedListener {

    private FragmentFirstBinding binding;
    private SanAdapter adapter;
    private List<SanQuestion> questions;
    private static final String TAG = "FirstFragment";

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Инициализация списка вопросов САН
        questions = initializeQuestions();

        // Настройка RecyclerView
        RecyclerView recyclerView = binding.sanRecyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // Используем конструктор по умолчанию
        adapter = new SanAdapter();
        // Устанавливаем listener
        adapter.setOnScoreSelectedListener(this);
        recyclerView.setAdapter(adapter);

        // Передаем начальный список через submitList
        adapter.submitList(questions);

        // Анимация появления списка
        recyclerView.setAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in));

        // Кнопка расчета
        Button calculateButton = binding.calculateButton;
        calculateButton.setOnClickListener(v -> {
            calculateAndDisplayState(null); // Передаем null, так как resultTextView больше не нужен
        });
    }

    private List<SanQuestion> initializeQuestions() {
        List<SanQuestion> questionList = new ArrayList<>();
        questionList.add(new SanQuestion("Самочувствие хорошее", "Самочувствие плохое")); // Инвертирован
        questionList.add(new SanQuestion("Чувствую себя сильным", "Чувствую себя слабым")); // Инвертирован
        questionList.add(new SanQuestion("Активный", "Пассивный")); // Не инвертирован
        questionList.add(new SanQuestion("Подвижный", "Малоподвижный")); // Не инвертирован
        questionList.add(new SanQuestion("Веселый", "Грустный")); // Инвертирован
        questionList.add(new SanQuestion("Хорошее настроение", "Плохое настроение")); // Инвертирован
        questionList.add(new SanQuestion("Работоспособный", "Разбитый")); // Инвертирован
        questionList.add(new SanQuestion("Полный сил", "Обессиленный")); // Инвертирован
        questionList.add(new SanQuestion("Быстрый", "Медлительный")); // Не инвертирован
        questionList.add(new SanQuestion("Деятельный", "Бездеятельный")); // Не инвертирован
        questionList.add(new SanQuestion("Счастливый", "Несчастный")); // Инвертирован
        questionList.add(new SanQuestion("Жизнерадостный", "Мрачный")); // Инвертирован
        questionList.add(new SanQuestion("Расслабленный", "Напряженный")); // Инвертирован
        questionList.add(new SanQuestion("Здоровый", "Больной")); // Инвертирован
        questionList.add(new SanQuestion("Увлеченный", "Безучастный")); // Не инвертирован
        questionList.add(new SanQuestion("Заинтересованный", "Равнодушный")); // Не инвертирован
        questionList.add(new SanQuestion("Восторженный", "Унылый")); // Инвертирован
        questionList.add(new SanQuestion("Радостный", "Печальный")); // Инвертирован
        questionList.add(new SanQuestion("Отдохнувший", "Усталый")); // Инвертирован
        questionList.add(new SanQuestion("Свежий", "Изнуренный")); // Инвертирован
        questionList.add(new SanQuestion("Возбужденный", "Сонливый")); // Не инвертирован
        questionList.add(new SanQuestion("Желание работать", "Желание отдохнуть")); // Не инвертирован
        questionList.add(new SanQuestion("Спокойный", "Взволнованный")); // Инвертирован
        questionList.add(new SanQuestion("Оптимистичный", "Пессимистичный")); // Инвертирован
        questionList.add(new SanQuestion("Выносливый", "Утомляемый")); // Инвертирован
        questionList.add(new SanQuestion("Бодрый", "Вялый")); // Инвертирован
        questionList.add(new SanQuestion("Соображать легко", "Соображать трудно")); // Не инвертирован
        questionList.add(new SanQuestion("Внимательный", "Рассеянный")); // Не инвертирован
        questionList.add(new SanQuestion("Полный надежд", "Разочарованный")); // Инвертирован
        questionList.add(new SanQuestion("Довольный", "Недовольный")); // Инвертирован
        return questionList;
    }

    // Реализация метода listener'а
    @Override
    public void onScoreSelected(int position, int score) {
        if (position >= 0 && position < questions.size()) {
            // 1. Создаем НОВЫЙ список
            List<SanQuestion> updatedQuestions = new ArrayList<>(questions);

            // 2. Получаем объект из НОВОГО списка и обновляем его
            SanQuestion questionToUpdate = updatedQuestions.get(position);
            // Создаем новый объект SanQuestion с обновленным score
            // Это важно для DiffUtil, чтобы он корректно определил изменение
            SanQuestion updatedQuestion = new SanQuestion(questionToUpdate.getPositivePole(), questionToUpdate.getNegativePole());
            updatedQuestion.setScore(score);
            updatedQuestions.set(position, updatedQuestion); // Заменяем старый объект новым

            // 3. Обновляем поле questions фрагмента
            this.questions = updatedQuestions;

            // 4. Передаем НОВЫЙ список в адаптер
            adapter.submitList(updatedQuestions);

            Log.d(TAG, "Score updated at position " + position + " to " + score);
        }
    }

    private void calculateAndDisplayState(TextView resultTextView) {
        // Проверяем авторизацию
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(getContext(), "Пожалуйста, войдите в аккаунт", Toast.LENGTH_LONG).show();
            // Переходим к экрану логина
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
            return;
        }

        int[] wellbeingIndices = {0, 1, 6, 7, 12, 13, 18, 19, 24, 25};
        int[] activityIndices = {2, 3, 8, 9, 14, 15, 20, 21, 26, 27};
        int[] moodIndices = {4, 5, 10, 11, 16, 17, 22, 23, 28, 29};

        float wellbeingScore = calculateCategoryScore(wellbeingIndices);
        float activityScore = calculateCategoryScore(activityIndices);
        float moodScore = calculateCategoryScore(moodIndices);

        // Сохранение результатов в Firebase
        saveTestResult(wellbeingScore, activityScore, moodScore);

        // Показываем SecondFragment как диалог
        SecondFragment dialog = new SecondFragment();
        Bundle args = new Bundle();
        args.putFloat("wellbeing_score", wellbeingScore);
        args.putFloat("activity_score", activityScore);
        args.putFloat("mood_score", moodScore);
        args.putString("interpretation", interpretState(wellbeingScore, activityScore, moodScore));
        dialog.setArguments(args);
        dialog.show(getParentFragmentManager(), "SecondFragment");
    }

    private void saveTestResult(float wellbeingScore, float activityScore, float moodScore) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference testResultsRef = FirebaseDatabase.getInstance("https://emotions-guide-c173c-default-rtdb.europe-west1.firebasedatabase.app/")
                .getReference()
                .child("Users")
                .child(userId)
                .child("TestResults");

        // Создаём новый результат теста
        TestResult result = new TestResult(wellbeingScore, activityScore, moodScore, new Date().getTime());
        String resultId = testResultsRef.push().getKey(); // Генерируем уникальный ключ для результата

        Log.d(TAG, "Сохранение результата: wellbeing=" + wellbeingScore + ", activity=" + activityScore + ", mood=" + moodScore);

        testResultsRef.child(resultId).setValue(result)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Результат теста успешно сохранён: " + resultId);
                    Toast.makeText(getContext(), "Результат сохранён!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Ошибка сохранения результата теста: " + e.getMessage());
                    Toast.makeText(getContext(), "Ошибка сохранения: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private float calculateCategoryScore(int[] indices) {
        int sum = 0;
        for (int index : indices) {
            int rawScore = questions.get(index).getScore(); // -3...3
            int adjustedScore = -rawScore + 4; // 3 → 1, -3 → 7
            sum += adjustedScore; // 1-7
        }
        return sum / 10.0f;
    }

    private String interpretState(float wellbeing, float activity, float mood) {
        StringBuilder state = new StringBuilder();
        float overallScore = (wellbeing + activity + mood) / 3.0f;

        // Определяем основной статус
        state.append("");
        if (overallScore >= 5.5) {
            state.append("Отличное состояние! ⭐\n");
            state.append("Вы чувствуете себя бодро, активно и в хорошем настроении.\n\n");
            state.append("Детали:\n");
            state.append("- Ваши показатели сбалансированы, что говорит о гармоничном состоянии.\n");
        } else if (overallScore >= 4.5) {
            state.append("Хорошее состояние! 🌟\n");
            state.append("Вы чувствуете себя достаточно хорошо и энергично.\n\n");
            state.append("Детали:\n");
            state.append("- Показатели находятся на стабильном уровне, есть небольшой потенциал для улучшения.\n");
        } else if (overallScore >= 3.5) {
            state.append("Нормальное состояние 🌤\n");
            state.append("Вы чувствуете себя умеренно, с некоторыми колебаниями в самочувствии.\n\n");
            state.append("Детали:\n");
            state.append("- Показатели в пределах нормы, но есть области, требующие внимания.\n");
        } else if (overallScore >= 2.5) {
            state.append("Пониженное состояние 🌥\n");
            state.append("Вы чувствуете некоторую усталость и спад энергии.\n\n");
            state.append("Детали:\n");
            state.append("- Показатели указывают на необходимость восстановления и отдыха.\n");
        } else {
            state.append("Требуется восстановление ⛅\n");
            state.append("Вы испытываете значительную усталость и недостаток энергии.\n\n");
            state.append("Детали:\n");
            state.append("- Показатели говорят о том, что вашему организму нужен отдых и поддержка.\n");
        }

        // Определяем приоритетные области для улучшения
        List<String> priorities = new ArrayList<>();
        if (wellbeing < 4.0) priorities.add("самочувствие");
        if (activity < 4.0) priorities.add("активность");
        if (mood < 4.0) priorities.add("настроение");

        // Формируем персонализированные рекомендации
        if (!priorities.isEmpty()) {
            state.append("\nПриоритетные области для улучшения: ").append(String.join(", ", priorities)).append(" 💡\n");
        }

        // Добавляем конкретные рекомендации
        state.append("\nРекомендации: 📋\n");

        // Проверяем критически низкие показатели
        if (wellbeing <= 2.0 && activity <= 2.0 && mood <= 2.0) {
            state.append("\n❗ Важно:\n");
            state.append("Если вы находитесь в кризисной ситуации или вам тяжело справиться самостоятельно,\n");
            state.append("позвоните на Единый телефон доверия: <phone>+7 (495) 400-99-99</phone>\n");
            state.append("Квалифицированные специалисты готовы вас выслушать и поддержать 24/7\n\n");
        }

        // Рекомендации на основе комбинаций показателей
        if (wellbeing <= 2.0 && activity <= 2.0 && mood <= 2.0) {
            state.append("- Отдохни, дружище! 20 минут в тишине с закрытыми глазами \uD83D\uDE34\n");
            state.append("  → Снизит уровень стресса и восстановит энергию\n");
            state.append("- Попробуй технику 4-7-8: вдох на 4, задержка на 7, выдох на 8 \uD83D\uDCA8\n");
            state.append("  → Активирует парасимпатическую нервную систему для расслабления\n");
            state.append("- Прими тёплый душ или ванну \uD83D\uDEB0\n");
            state.append("  → Расслабит мышцы и улучшит кровообращение\n");
            state.append("- Отложи все дела на 1-2 часа, ты заслужил отдых \uD83D\uDE2D\n");
            state.append("  → Даст организму время на восстановление\n");
            state.append("- Запиши 3 вещи, за которые ты благодарен сегодня \uD83D\uDCDD\n");
            state.append("  → Снизит уровень кортизола и повысит уровень серотонина\n");
        } else if (wellbeing <= 3.0 && activity >= 5.0) {
            state.append("- Не перегружай себя! Добавь 15-минутные перерывы между делами \uD83D\uDE34\n");
            state.append("  → Предотвратит эмоциональное выгорание\n");
            state.append("- Сделай лёгкую растяжку 5 минут \uD83D\uDC83\n");
            state.append("  → Улучшит кровообращение и снимет мышечное напряжение\n");
            state.append("- Проверь режим сна: спи не менее 7 часов \uD83D\uDE34\n");
            state.append("  → Обеспечит полноценное восстановление организма\n");
            state.append("- Попробуй технику прогрессивной мышечной релаксации \uD83D\uDCAA\n");
            state.append("  → Снимет физическое и психическое напряжение\n");
        } else if (mood <= 3.0 && wellbeing >= 5.0) {
            state.append("- Включи любимую музыку на 10 минут \uD83C\uDFB6\n");
            state.append("  → Стимулирует выработку дофамина и эндорфинов\n");
            state.append("- Позвони близкому человеку \uD83D\uDCDE\n");
            state.append("  → Активирует систему социальной поддержки\n");
            state.append("- Посмотри что-нибудь смешное \uD83D\uDE02\n");
            state.append("  → Вызовет естественный выброс эндорфинов\n");
            state.append("- Запиши 3 приятных момента за сегодня \uD83D\uDCDD\n");
            state.append("  → Перенастроит фокус внимания на позитив\n");
        } else if (activity <= 3.0 && mood >= 5.0) {
            state.append("- Прогуляйся 15 минут на свежем воздухе \uD83C\uDF0D\n");
            state.append("  → Увеличит уровень кислорода в крови\n");
            state.append("- Потанцуй под любимую песню \uD83D\uDD7A\n");
            state.append("  → Активирует двигательные центры мозга\n");
            state.append("- Попробуй йогу или лёгкую зарядку \uD83D\uDEB4\n");
            state.append("  → Улучшит кровообращение и повысит энергию\n");
            state.append("- Сделай 5-минутную технику 4-7-8 \uD83D\uDCA8\n");
            state.append("  → Оптимизирует работу дыхательной системы\n");
        } else if (overallScore >= 5.5) {
            state.append("- Поддерживай ритм: 45 минут активности, 10 минут отдыха \uD83D\uDD52\n");
            state.append("  → Оптимизирует продуктивность и предотвращает усталость\n");
            state.append("- Попробуй новое хобби или навык \uD83C\uDFAF\n");
            state.append("  → Стимулирует нейропластичность мозга\n");
            state.append("- Поделись энергией с другими \uD83D\uDC4B\n");
            state.append("  → Усилит чувство социальной связанности\n");
            state.append("- Запланируй активный отдых на выходные \uD83C\uDFD6\n");
            state.append("  → Создаст позитивное ожидание и мотивацию\n");
            state.append("- Запиши 3 цели на завтра \uD83D\uDCDD\n");
            state.append("  → Активирует систему вознаграждения мозга\n");
        } else if (overallScore >= 4.5) {
            state.append("- Сделай 10-минутную дыхательную гимнастику \uD83D\uDCA8\n");
            state.append("  → Нормализует работу вегетативной нервной системы\n");
            state.append("- Выпей тёплый чай и отдохни 15 минут \uD83C\uDF75\n");
            state.append("  → Снизит уровень стресса и улучшит концентрацию\n");
            state.append("- Запиши 3 цели на сегодня \uD83D\uDCDD\n");
            state.append("  → Создаст структуру и направление действий\n");
            state.append("- Сделай лёгкую разминку \uD83D\uDCAA\n");
            state.append("  → Улучшит кровообращение и повысит бодрость\n");
            state.append("- Попробуй технику 4-7-8 для расслабления \uD83D\uDCA8\n");
            state.append("  → Снизит уровень тревожности\n");
        } else if (overallScore >= 3.5) {
            state.append("- Позволь себе 5 минут медитации \uD83D\uDE34\n");
            state.append("  → Снизит уровень кортизола и улучшит концентрацию\n");
            state.append("- Выпей стакан воды \uD83D\uDCA6\n");
            state.append("  → Улучшит когнитивные функции и уровень энергии\n");
            state.append("- Сделай 3-5 простых упражнений \uD83D\uDCAA\n");
            state.append("  → Активирует выработку эндорфинов\n");
            state.append("- Позвони другу \uD83D\uDCDE\n");
            state.append("  → Снизит уровень стресса через социальную поддержку\n");
            state.append("- Запиши 3 вещи, за которые ты благодарен \uD83D\uDCDD\n");
            state.append("  → Снизит уровень тревожности и улучшит настроение\n");
        } else {
            state.append("- Сделай паузу: 20 минут отдыха \uD83D\uDE34\n");
            state.append("  → Даст организму время на восстановление\n");
            state.append("- Выпей тёплый чай \uD83C\uDF75\n");
            state.append("  → Снизит уровень стресса и улучшит концентрацию\n");
            state.append("- Попробуй технику 4-7-8: вдох на 4, задержка на 7, выдох на 8 \uD83D\uDCA8\n");
            state.append("  → Активирует парасимпатическую нервную систему\n");
            state.append("- Послушай спокойную музыку \uD83C\uDFB6\n");
            state.append("  → Снизит уровень кортизола и улучшит настроение\n");
            state.append("- Запиши 3 приятных момента за сегодня \uD83D\uDCDD\n");
            state.append("  → Перенастроит фокус внимания на позитив\n");
        }

        // Добавляем специфические рекомендации по каждому показателю
        if (wellbeing <= 3.0) {
            state.append("\nДля улучшения самочувствия: \uD83D\uDCAA\n");
            state.append("- Проверь режим сна и питания \uD83D\uDE34\n");
            state.append("  → Обеспечит базовые потребности организма\n");
            state.append("- Сделай лёгкую растяжку \uD83D\uDC83\n");
            state.append("  → Улучшит кровообращение и гибкость\n");
            state.append("- Проветри помещение \uD83C\uDF2C\n");
            state.append("  → Увеличит уровень кислорода в крови\n");
            state.append("- Попробуй технику прогрессивной мышечной релаксации \uD83D\uDCAA\n");
            state.append("  → Снимет физическое и психическое напряжение\n");
        }

        if (activity <= 3.0) {
            state.append("\nДля повышения активности: \uD83D\uDEB6\n");
            state.append("- Сделай 10 приседаний \uD83D\uDCAA\n");
            state.append("  → Активирует крупные мышечные группы\n");
            state.append("- Пройдись по лестнице \uD83D\uDEB6\n");
            state.append("  → Улучшит кровообращение и повысит энергию\n");
            state.append("- Сделай 5-минутную зарядку \uD83D\uDCAA\n");
            state.append("  → Стимулирует выработку эндорфинов\n");
            state.append("- Попробуй технику 4-7-8 для бодрости \uD83D\uDCA8\n");
            state.append("  → Увеличит уровень кислорода в крови\n");
        }

        if (mood <= 3.0) {
            state.append("\nДля улучшения настроения: \uD83D\uDE0A\n");
            state.append("- Вспомни приятный момент \uD83D\uDE0D\n");
            state.append("  → Активирует позитивные нейронные связи\n");
            state.append("- Посмотри смешное видео \uD83D\uDE02\n");
            state.append("  → Вызовет естественный выброс эндорфинов\n");
            state.append("- Позвони другу \uD83D\uDCDE\n");
            state.append("  → Активирует систему социальной поддержки\n");
            state.append("- Запиши 3 вещи, за которые ты благодарен \uD83D\uDCDD\n");
            state.append("  → Снизит уровень тревожности и улучшит настроение\n");
        }

        return state.toString();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
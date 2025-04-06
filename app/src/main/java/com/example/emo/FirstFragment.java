package com.example.emo;

import android.content.Intent;
import android.os.Bundle;
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

        // Средний балл по всем категориям
        float overallScore = (wellbeing + activity + mood) / 3.0f;

        // Основная интерпретация по общему баллу
        if (overallScore >= 5.0) {
            state.append("Отличное состояние! \uD83C\uDF1F\nВы чувствуете себя бодро, активно и в хорошем настроении.");
        } else if (overallScore >= 4.0) {
            state.append("Нормальное состояние \uD83D\uDE42\nВ целом вы в порядке, но есть над чем поработать.");
        } else if (overallScore >= 3.0) {
            state.append("Переменчивое состояние \uD83C\uDF25️\nВаше самочувствие, активность и настроение нестабильны.");
        } else {
            state.append("Плохое состояние \uD83D\uDE1F\nВы чувствуете усталость, пассивность или подавленность.");
        }

        // Учет соотношения между показателями
        state.append("\n\nДетали:\n");
        if (wellbeing < 4.0 && activity < 4.0 && mood >= 4.0) {
            state.append("- Несмотря на хорошее настроение, ваше физическое состояние и активность низкие. Возможно, стоит отдохнуть.");
        } else if (wellbeing >= 4.0 && activity >= 4.0 && mood < 4.0) {
            state.append("- Вы физически в порядке и активны, но настроение подкачало. Попробуйте найти что-то вдохновляющее!");
        } else if (wellbeing < 4.0 && activity >= 4.0 && mood < 4.0) {
            state.append("- Вы активны, но самочувствие и настроение оставляют желать лучшего. Не перегружайте себя.");
        } else if (Math.abs(wellbeing - activity) > 1.5 || Math.abs(wellbeing - mood) > 1.5 || Math.abs(activity - mood) > 1.5) {
            state.append("- Ваши показатели сильно различаются. Это может указывать на внутренний дисбаланс.");
        } else {
            state.append("- Ваши показатели сбалансированы, что говорит о гармоничном состоянии.");
        }

        // Рекомендации
        state.append("\n\nРекомендации:\n");

        // Общие рекомендации на основе среднего балла
        if (overallScore >= 5.5) {
            state.append("- Вы в отличной форме! \uD83C\uDF1F Продолжайте поддерживать баланс между активностью, отдыхом и позитивным настроем.\n");
            state.append("- Попробуйте поставить себе новую цель: например, освоить новый навык или попробовать новый вид спорта.\n");
        } else if (overallScore >= 4.5) {
            state.append("- Вы на правильном пути! \uD83D\uDE0A Поддерживайте своё состояние, добавляя небольшие улучшения в распорядок дня.\n");
            state.append("- Попробуйте выделить 10 минут на медитацию или чтение вдохновляющей книги.\n");
        } else if (overallScore >= 3.5) {
            state.append("- Ваше состояние в норме, но есть куда стремиться. \uD83C\uDF25️ Попробуйте небольшие изменения, чтобы почувствовать себя лучше.\n");
            state.append("- Составьте план на день с 2-3 приятными делами, которые вас вдохновят.\n");
        } else if (overallScore >= 2.5) {
            state.append("- Похоже, вам нужно немного заботы о себе. \uD83D\uDE1F Давайте попробуем улучшить ваше состояние.\n");
            state.append("- Начните с малого: сделайте 5 глубоких вдохов и выдохов, чтобы снять напряжение.\n");
        } else {
            state.append("- Похоже, вы чувствуете себя не лучшим образом. \uD83D\uDE22 Давайте попробуем вернуть вам силы и хорошее настроение.\n");
            state.append("- Сделайте паузу: выпейте тёплый чай, включите спокойную музыку и позвольте себе отдохнуть.\n");
        }

        // Рекомендации по самочувствию (wellbeing)
        if (wellbeing < 2.5) {
            state.append("- Ваше самочувствие требует внимания. \uD83C\uDFE5 Постарайтесь отдохнуть: лягте на 15 минут с закрытыми глазами, дышите медленно и глубоко.\n");
            state.append("- Проверьте, достаточно ли вы пьёте воды — обезвоживание может усиливать усталость.\n");
        } else if (wellbeing < 3.5) {
            state.append("- Самочувствие ниже среднего. \uD83D\uDE2C Попробуйте лёгкую растяжку или тёплый душ, чтобы снять напряжение.\n");
            state.append("- Добавьте в рацион что-то полезное: например, фрукты или орехи для энергии.\n");
        } else if (wellbeing < 4.5) {
            state.append("- Самочувствие на среднем уровне. \uD83D\uDE10 Уделите внимание сну: постарайтесь лечь спать на 30 минут раньше обычного.\n");
            state.append("- Сделайте 5-минутную дыхательную гимнастику: вдох на 4 счёта, выдох на 6.\n");
        } else {
            state.append("- Отличное самочувствие! \uD83D\uDCAA Поддерживайте его, соблюдая режим дня и питание.\n");
        }

        // Рекомендации по активности (activity)
        if (activity < 2.5) {
            state.append("- Уровень активности очень низкий. \uD83D\uDEB6 Начните с малого: сделайте 10-минутную прогулку на свежем воздухе.\n");
            state.append("- Попробуйте простые упражнения: потянитесь или сделайте несколько приседаний.\n");
        } else if (activity < 3.5) {
            state.append("- Активность ниже среднего. \uD83C\uDFCB️ Добавьте движения: например, потанцуйте под любимую песню.\n");
            state.append("- Если вы сидите долго, вставайте каждые 30 минут и делайте лёгкую разминку.\n");
        } else if (activity < 4.5) {
            state.append("- Активность на среднем уровне. \uD83D\uDEB4 Попробуйте что-то новое: например, йогу или короткую пробежку.\n");
            state.append("- Поставьте цель: пройти 5000 шагов за день и отслеживайте прогресс.\n");
        } else {
            state.append("- Вы полны энергии! \uD83C\uDFC3 Поддерживайте активность, чередуя разные виды нагрузок.\n");
        }

        // Рекомендации по настроению (mood)
        if (mood < 2.5) {
            state.append("- Настроение на низком уровне. \uD83D\uDE22 Попробуйте посмотреть смешное видео или позвонить другу.\n");
            state.append("- Напишите 3 вещи, за которые вы благодарны сегодня — это поможет переключить мысли.\n");
        } else if (mood < 3.5) {
            state.append("- Настроение ниже среднего. \uD83D\uDE15 Сделайте что-то приятное: заварите любимый чай или включите расслабляющую музыку.\n");
            state.append("- Попробуйте технику '5-4-3-2-1': назовите 5 вещей, которые видите, 4 — которые слышите, и так далее.\n");
        } else if (mood < 4.5) {
            state.append("- Настроение на среднем уровне. \uD83D\uDE42 Найдите вдохновение: посмотрите мотивирующий фильм или почитайте вдохновляющие цитаты.\n");
            state.append("- Сделайте что-то творческое: нарисуйте, напишите или приготовьте новое блюдо.\n");
        } else {
            state.append("- Отличное настроение! \uD83D\uDE04 Делитесь позитивом с окружающими — это усилит ваше состояние.\n");
        }

        // Учёт комбинаций показателей
        if (wellbeing < 3.5 && activity >= 4.5) {
            state.append("- Вы активны, но самочувствие подкачало. \uD83D\uDEA7 Не перегружайте себя — добавьте больше отдыха между делами.\n");
        }
        if (wellbeing >= 4.5 && mood < 3.5) {
            state.append("- Вы чувствуете себя хорошо, но настроение ниже среднего. \uD83C\uDF1E Попробуйте выйти на природу или послушать любимую музыку.\n");
        }
        if (activity < 3.5 && mood >= 4.5) {
            state.append("- У вас хорошее настроение, но активность низкая. \uD83D\uDE0A Используйте позитивный настрой для лёгкой активности, например, прогулки.\n");
        }
        if (wellbeing < 3.5 && mood < 3.5 && activity < 3.5) {
            state.append("- Все показатели низкие. \uD83D\uDE1E Начните с малого: сделайте глубокий вдох, выпейте воды и отдохните 15 минут.\n");
        }
        if (Math.abs(wellbeing - activity) > 2.0 || Math.abs(wellbeing - mood) > 2.0 || Math.abs(activity - mood) > 2.0) {
            state.append("- Ваши показатели сильно различаются. \uD83D\uDD04 Постарайтесь найти баланс: чередуйте отдых, активность и приятные дела.\n");
        }

        return state.toString();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
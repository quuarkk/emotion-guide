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
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class FirstFragment extends Fragment {

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
        adapter = new SanAdapter(questions) {
            @Override
            public void onBindViewHolder(@NonNull SanViewHolder holder, int position) {
                super.onBindViewHolder(holder, position);
                updateProgress(); // Обновляем прогресс при изменении SeekBar
            }
        };
        recyclerView.setAdapter(adapter);

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
        questionList.add(new SanQuestion("Самочувствие хорошее", "Самочувствие плохое"));
        questionList.add(new SanQuestion("Чувствую себя сильным", "Чувствую себя слабым"));
        questionList.add(new SanQuestion("Активный", "Пассивный"));
        questionList.add(new SanQuestion("Подвижный", "Малоподвижный"));
        questionList.add(new SanQuestion("Веселый", "Грустный"));
        questionList.add(new SanQuestion("Хорошее настроение", "Плохое настроение"));
        questionList.add(new SanQuestion("Работоспособный", "Разбитый"));
        questionList.add(new SanQuestion("Полный сил", "Обессиленный"));
        questionList.add(new SanQuestion("Быстрый", "Медлительный"));
        questionList.add(new SanQuestion("Деятельный", "Бездеятельный"));
        questionList.add(new SanQuestion("Счастливый", "Несчастный"));
        questionList.add(new SanQuestion("Жизнерадостный", "Мрачный"));
        questionList.add(new SanQuestion("Расслабленный", "Напряженный"));
        questionList.add(new SanQuestion("Здоровый", "Больной"));
        questionList.add(new SanQuestion("Увлеченный", "Безучастный"));
        questionList.add(new SanQuestion("Заинтересованный", "Равнодушный"));
        questionList.add(new SanQuestion("Восторженный", "Унылый"));
        questionList.add(new SanQuestion("Радостный", "Печальный"));
        questionList.add(new SanQuestion("Отдохнувший", "Усталый"));
        questionList.add(new SanQuestion("Свежий", "Изнуренный"));
        questionList.add(new SanQuestion("Возбужденный", "Сонливый"));
        questionList.add(new SanQuestion("Желание работать", "Желание отдохнуть"));
        questionList.add(new SanQuestion("Спокойный", "Взволнованный"));
        questionList.add(new SanQuestion("Оптимистичный", "Пессимистичный"));
        questionList.add(new SanQuestion("Выносливый", "Утомляемый"));
        questionList.add(new SanQuestion("Бодрый", "Вялый"));
        questionList.add(new SanQuestion("Соображать легко", "Соображать трудно"));
        questionList.add(new SanQuestion("Внимательный", "Рассеянный"));
        questionList.add(new SanQuestion("Полный надежд", "Разочарованный"));
        questionList.add(new SanQuestion("Довольный", "Недовольный"));
        return questionList;
    }

    private void updateProgress() {
        int completed = 0;
        for (SanQuestion question : questions) {
            if (question.getScore() != 3) { // Считаем измененные значения (не нейтральные)
                completed++;
            }
        }
        binding.progressBar.setProgress(completed);
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
        int[] invertedIndices = {0, 1, 4, 5, 6, 7, 10, 11, 12, 13, 16, 17, 18, 19, 22, 23, 24, 25, 28, 29};
        int sum = 0;
        for (int index : indices) {
            int rawScore = questions.get(index).getScore(); // -3...3
            boolean shouldInvert = Arrays.stream(invertedIndices).anyMatch(i -> i == index);
            int adjustedScore = shouldInvert ? rawScore + 4 : -rawScore + 4; // Корректно для обеих ситуаций
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
        if (wellbeing < 4.0) state.append("- Позаботьтесь о здоровье: отдых, питание, сон.\n");
        if (activity < 4.0) state.append("- Добавьте движения: прогулка или легкая зарядка.\n");
        if (mood < 4.0) state.append("- Поднимите настроение: музыка, хобби, общение.\n");
        if (overallScore >= 5.0) state.append("- Продолжайте в том же духе!\n");

        return state.toString();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
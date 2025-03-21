package com.example.emo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emo.databinding.FragmentFirstBinding;

import java.util.ArrayList;
import java.util.List;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    private SanAdapter adapter;
    private List<SanQuestion> questions;

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

//        BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottom_navigation);
//        if (bottomNav != null) {
//            int bottomNavHeight = bottomNav.getHeight();
//            if (bottomNavHeight == 0) {
//                bottomNav.post(() -> {
//                    int height = bottomNav.getHeight();
//                    binding.getRoot().setPadding(16, 16, 16, height + 16);
//                });
//            } else {
//                binding.getRoot().setPadding(16, 16, 16, bottomNavHeight + 16);
//            }
//        }

        // Кнопка расчета
        Button calculateButton = binding.calculateButton;
        CardView resultCard = binding.resultCard;
        TextView resultTextView = binding.resultTextView;

        calculateButton.setOnClickListener(v -> {
            calculateAndDisplayState(resultTextView);
            resultCard.setVisibility(View.VISIBLE);
            resultCard.startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_in_left));
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
        int[] wellbeingIndices = {0, 1, 6, 7, 12, 13, 18, 19, 24, 25};
        int[] activityIndices = {2, 3, 8, 9, 14, 15, 20, 21, 26, 27};
        int[] moodIndices = {4, 5, 10, 11, 16, 17, 22, 23, 28, 29};

        float wellbeingScore = calculateCategoryScore(wellbeingIndices);
        float activityScore = calculateCategoryScore(activityIndices);
        float moodScore = calculateCategoryScore(moodIndices);

        String result = String.format(
                "\uD83D\uDC99 Самочувствие: %.1f\n" +
                        "\uD83D\uDC9A Активность: %.1f\n" +
                        "\uD83D\uDC9B Настроение: %.1f\n\n" +
                        "Ваше состояние:\n%s",
                wellbeingScore, activityScore, moodScore, interpretState(wellbeingScore, activityScore, moodScore)
        );
        resultTextView.setText(result);
    }

    private float calculateCategoryScore(int[] indices) {
        int sum = 0;
        for (int index : indices) {
            int rawScore = questions.get(index).getScore(); // 0-6
            int adjustedScore = (index == 2 || index == 8 || index == 12 || index == 20) ? 6 - rawScore : rawScore;
            sum += adjustedScore + 1; // Преобразуем в 1-7
        }
        return sum / 10.0f; // Среднее по 10 вопросам
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
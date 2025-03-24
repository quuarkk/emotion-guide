package com.example.emo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.example.emo.databinding.FragmentSecondBinding;

public class SecondFragment extends DialogFragment {

    private FragmentSecondBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSecondBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Настройка размеров и фона диалога
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Получаем данные из аргументов и отображаем результаты
        if (getArguments() != null) {
            float wellbeingScore = getArguments().getFloat("wellbeing_score", 0f);
            float activityScore = getArguments().getFloat("activity_score", 0f);
            float moodScore = getArguments().getFloat("mood_score", 0f);
            String interpretation = getArguments().getString("interpretation", "Нет данных");

            String result = String.format(
                    "\uD83D\uDC99 Самочувствие: %.1f\n" +
                            "\uD83D\uDC9A Активность: %.1f\n" +
                            "\uD83D\uDC9B Настроение: %.1f\n\n" +
                            "Ваше состояние:\n%s",
                    wellbeingScore, activityScore, moodScore, interpretation
            );
            binding.textviewSecond.setText(result);
        } else {
            binding.textviewSecond.setText("Ошибка: данные не переданы");
        }

        // Закрытие диалога по кнопке "Назад"
        binding.buttonSecond.setOnClickListener(v -> dismiss());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null) {
            getDialog().setCanceledOnTouchOutside(true); // Закрытие при нажатии вне области
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
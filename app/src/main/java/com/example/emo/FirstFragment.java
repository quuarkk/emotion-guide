package com.example.emo;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.emo.databinding.FragmentFirstBinding;

import android.graphics.PorterDuff;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    private ImageView moodEmoji;
    private TextView moodTextView;
    private SeekBar moodSeekBar;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            // Инициализация компонентов
            moodEmoji = binding.moodEmoji;
            moodTextView = binding.moodTextView;
            moodSeekBar = binding.moodSeekBar;

            // Установка максимального значения SeekBar на 6 (для диапазона от -3 до 3)
            moodSeekBar.setMax(6);
            
            // Установка начального значения в центр (3 соответствует 0 на нашей шкале)
            moodSeekBar.setProgress(3);
            updateMoodUI(0); // Начальное значение 0 (нейтральное)

            // Обработчик изменения настроения
            moodSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    // Преобразуем значение прогресса (0-6) в диапазон от -3 до 3
                    int moodValue = progress - 3;
                    updateMoodUI(moodValue);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    // Не используется
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    // Не используется
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateMoodUI(int moodValue) {
        // Определяем настроение на основе значения ползунка (от -3 до 3)
        String mood;
        int colorResId;
        int emojiResId;

        if (moodValue == -3) {
            mood = "Очень плохо";
            colorResId = R.color.mood_very_bad;
            emojiResId = R.drawable.very_bad__3;
        } else if (moodValue == -2) {
            mood = "Плохо";
            colorResId = R.color.mood_bad;
            emojiResId = R.drawable.badly__2;
        } else if (moodValue == -1) {
            mood = "Грустно";
            colorResId = R.color.mood_neutral;
            emojiResId = R.drawable.little_bad__1;
        } else if (moodValue == 0) {
            mood = "Нейтрально";
            colorResId = R.color.mood_neutral;
            emojiResId = R.drawable.neutral_0;
        } else if (moodValue == 1) {
            mood = "Нормально";
            colorResId = R.color.mood_good;
            emojiResId = R.drawable.fine_1;
        } else if (moodValue == 2) {
            mood = "Хорошо";
            colorResId = R.color.mood_good;
            emojiResId = R.drawable.joyful_2;
        } else if (moodValue == 3) {
            mood = "Отлично";
            colorResId = R.color.mood_very_good;
            emojiResId = R.drawable.great_3;
        } else {
            // Значение по умолчанию
            mood = "Нейтрально";
            colorResId = R.color.mood_neutral;
            emojiResId = R.drawable.neutral_0;
        }

        // Изменим формат отображения текста, чтобы показывать реальное значение от -3 до 3
        moodTextView.setText(String.format("%s (%d)", mood, moodValue));
        int color = ContextCompat.getColor(requireContext(), colorResId);
        moodTextView.setTextColor(color);

        // Изменяем цвет ползунка без использования рефлексии
        moodSeekBar.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);

        // Для Android 6.0+ можно использовать этот метод вместо рефлексии
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            moodSeekBar.getThumb().setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }

        // Обновление смайлика в зависимости от настроения
        moodEmoji.setImageResource(emojiResId);
        setEmojiSize(0.5f); // Стандартный размер для всех эмодзи
    }

    private void setEmojiSize(float scale) {
        ViewGroup.LayoutParams params = moodEmoji.getLayoutParams();
        int baseSize = (int) (150 * getResources().getDisplayMetrics().density);
        params.width = (int) (baseSize * scale);
        params.height = (int) (baseSize * scale);
        moodEmoji.setLayoutParams(params);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
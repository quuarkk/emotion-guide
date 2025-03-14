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

import java.lang.reflect.Field;

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
            
            moodSeekBar.setProgress(4); // 5 по умолчанию (индекс 4 + 1)
            updateMoodUI(5);
            
            // Обработчик изменения настроения
            moodSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int moodValue = progress + 1; // От 1 до 10
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
        // Определяем настроение на основе значения ползунка (от 1 до 10)
        String mood;
        int colorResId;
        
        if (moodValue <= 2) {
            mood = "Очень плохо";
            colorResId = R.color.mood_very_bad;
        } else if (moodValue <= 4) {
            mood = "Плохо";
            colorResId = R.color.mood_bad;
        } else if (moodValue <= 6) {
            mood = "Нормально";
            colorResId = R.color.mood_neutral;
        } else if (moodValue <= 8) {
            mood = "Хорошо";
            colorResId = R.color.mood_good;
        } else {
            mood = "Отлично";
            colorResId = R.color.mood_very_good;
        }
        
        // Обновляем текст и цвет
        moodTextView.setText(String.format("%s (%d/10)", mood, moodValue));
        int color = ContextCompat.getColor(requireContext(), colorResId);
        moodTextView.setTextColor(color);
        
        // Изменяем цвет ползунка без использования рефлексии
        moodSeekBar.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
        
        // Для Android 6.0+ можно использовать этот метод вместо рефлексии
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            moodSeekBar.getThumb().setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }
        
        // Обновление смайлика в зависимости от настроения
        if (moodValue <= 3) {
            // Грустный смайлик для плохого настроения
            moodEmoji.setImageResource(R.drawable.emoji_sad);
            setEmojiSize(0.9f); // Немного уменьшаем грустный смайлик
        } else if (moodValue <= 7) {
            // Нейтральный смайлик для среднего настроения
            moodEmoji.setImageResource(R.drawable.emoji_neutral);
            setEmojiSize(1.0f); // Стандартный размер
        } else {
            // Счастливый смайлик для хорошего настроения
            moodEmoji.setImageResource(R.drawable.emoji_happy);
            setEmojiSize(1.1f); // Немного увеличиваем счастливый смайлик
        }
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
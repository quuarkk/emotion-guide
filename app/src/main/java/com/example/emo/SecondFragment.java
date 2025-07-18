package com.example.emo;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
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
            
            // Делаем номер телефона кликабельным
            makePhoneNumberClickable(binding.textviewSecond);
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

    private void makePhoneNumberClickable(TextView textView) {
        String text = textView.getText().toString();
        SpannableString spannableString = new SpannableString(text);
        
        int startIndex = text.indexOf("<phone>");
        int endIndex = text.indexOf("</phone>");
        
        if (startIndex != -1 && endIndex != -1) {
            // Извлекаем номер телефона
            final String phoneNumber = text.substring(startIndex + 7, endIndex);
            
            // Создаем кликабельный span
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View view) {
                    // Создаем Intent для звонка
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + phoneNumber.replaceAll("[^+0-9]", "")));
                    try {
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Не удалось открыть приложение для звонка", Toast.LENGTH_SHORT).show();
                    }
                }
            };
            
            // Удаляем теги из текста
            String cleanText = text.replace("<phone>", "").replace("</phone>", "");
            SpannableString cleanSpannable = new SpannableString(cleanText);
            
            // Вычисляем новые индексы для номера телефона в очищенном тексте
            int phoneStartIndex = startIndex;
            int phoneEndIndex = startIndex + phoneNumber.length();
            
            // Применяем span к тексту
            cleanSpannable.setSpan(clickableSpan, phoneStartIndex, phoneEndIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            // Устанавливаем текст и делаем его кликабельным
            textView.setText(cleanSpannable);
            textView.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }
}
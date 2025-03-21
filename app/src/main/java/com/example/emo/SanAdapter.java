package com.example.emo;

import android.content.res.ColorStateList;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SanAdapter extends RecyclerView.Adapter<SanAdapter.SanViewHolder> {
    private List<SanQuestion> questions;
    private static final String TAG = "SanAdapter";

    public SanAdapter(List<SanQuestion> questions) {
        this.questions = questions;
    }

    @NonNull
    @Override
    public SanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_san_question, parent, false);
        return new SanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SanViewHolder holder, int position) {
        SanQuestion question = questions.get(position);
        holder.positiveText.setText(question.getPositivePole());
        holder.negativeText.setText(question.getNegativePole());

        // Сброс цвета всех кнопок
        resetButtonColors(holder);

        // Установка текущего выбранного значения
        highlightSelectedButton(holder, question.getScore());

        // Обработчики нажатий на кнопки
        holder.btn3Positive.setOnClickListener(v -> setScoreAndHighlight(holder, question, 3));
        holder.btn2Positive.setOnClickListener(v -> setScoreAndHighlight(holder, question, 2));
        holder.btn1Positive.setOnClickListener(v -> setScoreAndHighlight(holder, question, 1));
        holder.btn0.setOnClickListener(v -> setScoreAndHighlight(holder, question, 0));
        holder.btn1Negative.setOnClickListener(v -> setScoreAndHighlight(holder, question, -1));
        holder.btn2Negative.setOnClickListener(v -> setScoreAndHighlight(holder, question, -2));
        holder.btn3Negative.setOnClickListener(v -> setScoreAndHighlight(holder, question, -3));
    }

    private void resetButtonColors(SanViewHolder holder) {
        int defaultColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.button_background);
        int textColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.text_color);

        holder.btn3Positive.getBackground().setTintList(ColorStateList.valueOf(defaultColor));
        holder.btn2Positive.getBackground().setTintList(ColorStateList.valueOf(defaultColor));
        holder.btn1Positive.getBackground().setTintList(ColorStateList.valueOf(defaultColor));
        holder.btn0.getBackground().setTintList(ColorStateList.valueOf(defaultColor));
        holder.btn1Negative.getBackground().setTintList(ColorStateList.valueOf(defaultColor));
        holder.btn2Negative.getBackground().setTintList(ColorStateList.valueOf(defaultColor));
        holder.btn3Negative.getBackground().setTintList(ColorStateList.valueOf(defaultColor));

        // Устанавливаем цвет текста для невыделенных кнопок
        holder.btn3Positive.setTextColor(textColor);
        holder.btn2Positive.setTextColor(textColor);
        holder.btn1Positive.setTextColor(textColor);
        holder.btn0.setTextColor(textColor);
        holder.btn1Negative.setTextColor(textColor);
        holder.btn2Negative.setTextColor(textColor);
        holder.btn3Negative.setTextColor(textColor);

        Log.d(TAG, "Reset button text color to: " + Integer.toHexString(textColor));
    }

    private void highlightSelectedButton(SanViewHolder holder, int score) {
        int positiveColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.button_positive);
        int negativeColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.button_negative);
        int neutralColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.button_zero);
        int highlightedTextColor = ContextCompat.getColor(holder.itemView.getContext(), android.R.color.white); // Белый текст для выделенных кнопок

        Log.d(TAG, "Highlighting button with score: " + score);
        Log.d(TAG, "Highlighted text color: " + Integer.toHexString(highlightedTextColor));

        switch (score) {
            case 3:
                holder.btn3Positive.getBackground().setTintList(ColorStateList.valueOf(positiveColor));
                holder.btn3Positive.setTextColor(highlightedTextColor);
                break;
            case 2:
                holder.btn2Positive.getBackground().setTintList(ColorStateList.valueOf(positiveColor));
                holder.btn2Positive.setTextColor(highlightedTextColor);
                break;
            case 1:
                holder.btn1Positive.getBackground().setTintList(ColorStateList.valueOf(positiveColor));
                holder.btn1Positive.setTextColor(highlightedTextColor);
                break;
            case 0:
                holder.btn0.getBackground().setTintList(ColorStateList.valueOf(neutralColor));
                holder.btn0.setTextColor(highlightedTextColor);
                break;
            case -1:
                holder.btn1Negative.getBackground().setTintList(ColorStateList.valueOf(negativeColor));
                holder.btn1Negative.setTextColor(highlightedTextColor);
                break;
            case -2:
                holder.btn2Negative.getBackground().setTintList(ColorStateList.valueOf(negativeColor));
                holder.btn2Negative.setTextColor(highlightedTextColor);
                break;
            case -3:
                holder.btn3Negative.getBackground().setTintList(ColorStateList.valueOf(negativeColor));
                holder.btn3Negative.setTextColor(highlightedTextColor);
                break;
        }
    }

    private void setScoreAndHighlight(SanViewHolder holder, SanQuestion question, int score) {
        question.setScore(score);
        resetButtonColors(holder);
        highlightSelectedButton(holder, score);
        notifyItemChanged(holder.getAdapterPosition()); // Обновляем элемент
    }

    @Override
    public int getItemCount() {
        return questions.size();
    }

    static class SanViewHolder extends RecyclerView.ViewHolder {
        TextView positiveText, negativeText;
        Button btn3Positive, btn2Positive, btn1Positive, btn0, btn1Negative, btn2Negative, btn3Negative;

        SanViewHolder(@NonNull View itemView) {
            super(itemView);
            positiveText = itemView.findViewById(R.id.positiveText);
            negativeText = itemView.findViewById(R.id.negativeText);
            btn3Positive = itemView.findViewById(R.id.btn3Positive);
            btn2Positive = itemView.findViewById(R.id.btn2Positive);
            btn1Positive = itemView.findViewById(R.id.btn1Positive);
            btn0 = itemView.findViewById(R.id.btn0);
            btn1Negative = itemView.findViewById(R.id.btn1Negative);
            btn2Negative = itemView.findViewById(R.id.btn2Negative);
            btn3Negative = itemView.findViewById(R.id.btn3Negative);
        }
    }
}
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
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SanAdapter extends RecyclerView.Adapter<SanAdapter.SanViewHolder> {
    private final ArrayList<SanQuestion> questions = new ArrayList<>();
    private static final String TAG = "SanAdapter";

    // --- Возвращаем Listener ---
    public interface OnScoreSelectedListener {
        void onScoreSelected(int position, int score);
    }
    private OnScoreSelectedListener scoreSelectedListener;
    public void setOnScoreSelectedListener(OnScoreSelectedListener listener) {
        this.scoreSelectedListener = listener;
    }
    // --- Конец Listener ---

    public SanAdapter() {
    }

    public void submitList(List<SanQuestion> newQuestions) {
        SanDiffCallback diffCallback = new SanDiffCallback(this.questions, newQuestions);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        this.questions.clear();
        this.questions.addAll(newQuestions);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public SanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_san_question, parent, false);
        SanViewHolder holder = new SanViewHolder(view);

        // Устанавливаем слушатели один раз здесь
        View.OnClickListener listener = v -> {
            int position = holder.getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                SanQuestion question = questions.get(position);
                int score = 0; // Значение по умолчанию
                int id = v.getId();
                if (id == R.id.score_minus_3) {
                    score = -3;
                } else if (id == R.id.score_minus_2) {
                    score = -2;
                } else if (id == R.id.score_minus_1) {
                    score = -1;
                } else if (id == R.id.score_0) {
                    score = 0;
                } else if (id == R.id.score_plus_1) {
                    score = 1;
                } else if (id == R.id.score_plus_2) {
                    score = 2;
                } else if (id == R.id.score_plus_3) {
                    score = 3;
                }
                // Вызываем listener вместо прямого обновления
                if (scoreSelectedListener != null) {
                    scoreSelectedListener.onScoreSelected(position, score);
                }
            }
        };

        holder.scoreMinus3.setOnClickListener(listener);
        holder.scoreMinus2.setOnClickListener(listener);
        holder.scoreMinus1.setOnClickListener(listener);
        holder.score0.setOnClickListener(listener);
        holder.scorePlus1.setOnClickListener(listener);
        holder.scorePlus2.setOnClickListener(listener);
        holder.scorePlus3.setOnClickListener(listener);

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull SanViewHolder holder, int position) {
        SanQuestion question = questions.get(position);
        holder.positiveLabel.setText(question.getPositivePole());
        holder.negativeLabel.setText(question.getNegativePole());

        // Сброс состояния всех кнопок
        resetButtonStates(holder);

        // Установка текущего выбранного значения
        highlightSelectedButton(holder, question.getScore());
    }

    private void resetButtonStates(SanViewHolder holder) {
        // Сбрасываем состояния
        holder.scoreMinus3.setSelected(false);
        holder.scoreMinus2.setSelected(false);
        holder.scoreMinus1.setSelected(false);
        holder.score0.setSelected(false);
        holder.scorePlus1.setSelected(false);
        holder.scorePlus2.setSelected(false);
        holder.scorePlus3.setSelected(false);
    }

    private void highlightSelectedButton(SanViewHolder holder, int score) {
        Log.d(TAG, "Highlighting button with score: " + score);

        switch (score) {
            case 3:
                holder.scorePlus3.setSelected(true);
                break;
            case 2:
                holder.scorePlus2.setSelected(true);
                break;
            case 1:
                holder.scorePlus1.setSelected(true);
                break;
            case 0:
                holder.score0.setSelected(true);
                break;
            case -1:
                holder.scoreMinus1.setSelected(true);
                break;
            case -2:
                holder.scoreMinus2.setSelected(true);
                break;
            case -3:
                holder.scoreMinus3.setSelected(true);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return questions.size();
    }

    static class SanViewHolder extends RecyclerView.ViewHolder {
        TextView positiveLabel, negativeLabel;
        Button scoreMinus3, scoreMinus2, scoreMinus1, score0, scorePlus1, scorePlus2, scorePlus3;

        SanViewHolder(@NonNull View itemView) {
            super(itemView);
            positiveLabel = itemView.findViewById(R.id.positive_label);
            negativeLabel = itemView.findViewById(R.id.negative_label);
            scoreMinus3 = itemView.findViewById(R.id.score_minus_3);
            scoreMinus2 = itemView.findViewById(R.id.score_minus_2);
            scoreMinus1 = itemView.findViewById(R.id.score_minus_1);
            score0 = itemView.findViewById(R.id.score_0);
            scorePlus1 = itemView.findViewById(R.id.score_plus_1);
            scorePlus2 = itemView.findViewById(R.id.score_plus_2);
            scorePlus3 = itemView.findViewById(R.id.score_plus_3);
        }
    }

    private static class SanDiffCallback extends DiffUtil.Callback {

        private final List<SanQuestion> oldList;
        private final List<SanQuestion> newList;

        public SanDiffCallback(List<SanQuestion> oldList, List<SanQuestion> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            // Предполагаем, что комбинация полюсов уникальна для идентификации элемента
            SanQuestion oldItem = oldList.get(oldItemPosition);
            SanQuestion newItem = newList.get(newItemPosition);
            return Objects.equals(oldItem.getPositivePole(), newItem.getPositivePole()) &&
                   Objects.equals(oldItem.getNegativePole(), newItem.getNegativePole());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            SanQuestion oldItem = oldList.get(oldItemPosition);
            SanQuestion newItem = newList.get(newItemPosition);
            // Содержимое изменилось, если изменился счет или полюса
            return oldItem.getScore() == newItem.getScore() &&
                   Objects.equals(oldItem.getPositivePole(), newItem.getPositivePole()) &&
                   Objects.equals(oldItem.getNegativePole(), newItem.getNegativePole());
        }
    }
}
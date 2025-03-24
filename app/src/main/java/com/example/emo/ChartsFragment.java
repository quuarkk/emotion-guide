package com.example.emo;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat; // Добавлено для ContextCompat
import androidx.fragment.app.Fragment;

import com.example.emo.databinding.FragmentChartsBinding;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChartsFragment extends Fragment {

    private FragmentChartsBinding binding;
    private static final String TAG = "ChartsFragment";

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentChartsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Загружаем данные и строим графики
        loadTestResults();
    }

    private void loadTestResults() {
        // Проверяем авторизацию
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(getContext(), "Пожалуйста, войдите в аккаунт", Toast.LENGTH_LONG).show();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference testResultsRef = FirebaseDatabase.getInstance("https://emotions-guide-c173c-default-rtdb.europe-west1.firebasedatabase.app/")
                .getReference()
                .child("Users")
                .child(userId)
                .child("TestResults");

        testResultsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<TestResult> testResults = new ArrayList<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    TestResult result = dataSnapshot.getValue(TestResult.class);
                    if (result != null) {
                        testResults.add(result);
                        Log.d(TAG, "Загружен результат: wellbeing=" + result.getWellbeingScore() +
                                ", activity=" + result.getActivityScore() +
                                ", mood=" + result.getMoodScore() +
                                ", timestamp=" + result.getTimestamp());
                    }
                }

                Log.d(TAG, "Всего результатов: " + testResults.size());
                if (testResults.isEmpty()) {
                    Toast.makeText(getContext(), "Нет данных для отображения", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (binding != null) {
                    setupCharts(testResults);
                } else {
                    Log.w(TAG, "binding is null, skipping setupCharts");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Ошибка загрузки результатов теста: " + error.getMessage());
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Ошибка загрузки данных: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void setupCharts(List<TestResult> testResults) {
        // График для Самочувствия
        setupChart(binding.wellbeingChart, testResults, "Самочувствие", R.color.chart_wellbeing_line, result -> result.getWellbeingScore());

        // График для Активности
        setupChart(binding.activityChart, testResults, "Активность", R.color.chart_activity_line, result -> result.getActivityScore());

        // График для Настроения
        setupChart(binding.moodChart, testResults, "Настроение", R.color.chart_mood_line, result -> result.getMoodScore());
    }

    private interface ScoreExtractor {
        float extract(TestResult result);
    }

    private void setupChart(LineChart chart, List<TestResult> testResults, String label, int colorResId, ScoreExtractor extractor) {
        // Проверяем доступность контекста
        if (getContext() == null) {
            Log.e(TAG, "Контекст недоступен, невозможно настроить график");
            return;
        }

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < testResults.size(); i++) {
            TestResult result = testResults.get(i);
            entries.add(new Entry(i, extractor.extract(result)));
        }

        LineDataSet dataSet = new LineDataSet(entries, label);
        // Используем идентификатор ресурса цвета, переданный в метод
        dataSet.setColor(ContextCompat.getColor(getContext(), colorResId));
        dataSet.setValueTextColor(ContextCompat.getColor(getContext(), R.color.chart_text_color));
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(ContextCompat.getColor(getContext(), colorResId));
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        // Закомментируем эти строки для избежания проблем с совместимостью
        // dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        // dataSet.setCubicIntensity(0.2f);
        dataSet.setDrawValues(false);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        // Устанавливаем цвет фона графика
        chart.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.chart_background));

        // Отключаем легенду
        chart.getLegend().setEnabled(false);

        // Настраиваем ось X
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(0f);
        xAxis.setLabelCount(Math.min(testResults.size(), 4), true);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(8f);
        xAxis.setTextColor(ContextCompat.getColor(getContext(), R.color.chart_text_color));
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd.MM", Locale.getDefault());

            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < testResults.size()) {
                    return sdf.format(new Date(testResults.get(index).getTimestamp()));
                }
                return "";
            }
        });

        // Настраиваем ось Y
        chart.getAxisLeft().setAxisMinimum(1f);
        chart.getAxisLeft().setAxisMaximum(7f);
        chart.getAxisLeft().setDrawGridLines(true);
        chart.getAxisLeft().setGridColor(ContextCompat.getColor(getContext(), R.color.chart_grid_color));
        chart.getAxisLeft().setTextColor(ContextCompat.getColor(getContext(), R.color.chart_text_color));
        chart.getAxisLeft().setTextSize(10f);
        chart.getAxisRight().setEnabled(false);

        // Устанавливаем отступы
        chart.setExtraBottomOffset(40f);
        chart.setExtraLeftOffset(10f);
        chart.setExtraRightOffset(10f);

        // Включаем масштабирование и прокрутку
        chart.setPinchZoom(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);

        chart.getDescription().setEnabled(false);
        chart.invalidate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
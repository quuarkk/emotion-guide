package com.example.emo;

import java.util.Date;

public class TestResult {
    private float wellbeingScore;
    private float activityScore;
    private float moodScore;
    private long timestamp; // Время прохождения теста в миллисекундах

    // Пустой конструктор для Firebase
    public TestResult() {
    }

    public TestResult(float wellbeingScore, float activityScore, float moodScore, long timestamp) {
        this.wellbeingScore = wellbeingScore;
        this.activityScore = activityScore;
        this.moodScore = moodScore;
        this.timestamp = timestamp;
    }

    public float getWellbeingScore() {
        return wellbeingScore;
    }

    public void setWellbeingScore(float wellbeingScore) {
        this.wellbeingScore = wellbeingScore;
    }

    public float getActivityScore() {
        return activityScore;
    }

    public void setActivityScore(float activityScore) {
        this.activityScore = activityScore;
    }

    public float getMoodScore() {
        return moodScore;
    }

    public void setMoodScore(float moodScore) {
        this.moodScore = moodScore;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
package com.example.emo;

public class SanQuestion {
    private String positivePole;
    private String negativePole;
    private int score; // Теперь от -3 до 3

    public SanQuestion(String positivePole, String negativePole) {
        this.positivePole = positivePole;
        this.negativePole = negativePole;
        this.score = 0; // По умолчанию нейтральное значение
    }

    public String getPositivePole() {
        return positivePole;
    }

    public String getNegativePole() {
        return negativePole;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
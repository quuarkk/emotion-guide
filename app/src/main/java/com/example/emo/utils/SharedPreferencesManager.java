package com.example.emo.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesManager {
    private static final String PREF_NAME = "EmotionGuidePrefs";
    private static final String KEY_TEST_RESULTS = "test_results";

    private final SharedPreferences preferences;

    public SharedPreferencesManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveTestResults(String jsonData) {
        preferences.edit().putString(KEY_TEST_RESULTS, jsonData).apply();
    }

    public String getTestResults() {
        return preferences.getString(KEY_TEST_RESULTS, null);
    }

    public void clearTestResults() {
        preferences.edit().remove(KEY_TEST_RESULTS).apply();
    }
} 
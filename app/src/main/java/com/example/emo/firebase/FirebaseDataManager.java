package com.example.emo.firebase;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class FirebaseDataManager {
    private static final String TAG = "FirebaseDataManager";
    private static final String USERS_PATH = "Users";
    private static final String TEST_RESULTS_PATH = "TestResults";
    
    // Инициализация Firebase с правильным регионом
    private static FirebaseDatabase database;
    
    static {
        try {
            // Получаем экземпляр базы данных с правильным URL
            database = FirebaseDatabase.getInstance("https://emotions-guide-c173c-default-rtdb.europe-west1.firebasedatabase.app");
            
            // Настройки для улучшения производительности и надежности
            database.setPersistenceEnabled(true);
            database.getReference().keepSynced(true);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка инициализации Firebase", e);
            // Пробуем получить экземпляр по умолчанию, если не удалось с URL
            database = FirebaseDatabase.getInstance();
        }
    }

    public static class TestResult {
        private final int activityScore;
        private final int moodScore;
        private final int wellbeingScore;
        private final long timestamp;

        public TestResult(int activityScore, int moodScore, int wellbeingScore, long timestamp) {
            this.activityScore = activityScore;
            this.moodScore = moodScore;
            this.wellbeingScore = wellbeingScore;
            this.timestamp = timestamp;
        }

        public int getActivityScore() {
            return activityScore;
        }

        public int getMoodScore() {
            return moodScore;
        }

        public int getWellbeingScore() {
            return wellbeingScore;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    public static CompletableFuture<List<TestResult>> getUserTestResults() {
        CompletableFuture<List<TestResult>> future = new CompletableFuture<>();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        if (currentUser == null) {
            future.complete(new ArrayList<>());
            return future;
        }
        
        String userId = currentUser.getUid();
        DatabaseReference testResultsRef = database
                .getReference(USERS_PATH)
                .child(userId)
                .child(TEST_RESULTS_PATH);
        
        // Устанавливаем таймаут для запроса
        testResultsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<TestResult> results = new ArrayList<>();
                
                for (DataSnapshot testSnapshot : dataSnapshot.getChildren()) {
                    try {
                        // Пробуем получить значения разных типов
                        Object activityObj = testSnapshot.child("activityScore").getValue();
                        Object moodObj = testSnapshot.child("moodScore").getValue();
                        Object wellbeingObj = testSnapshot.child("wellbeingScore").getValue();
                        Object timestampObj = testSnapshot.child("timestamp").getValue();
                        
                        int activityScore = parseValue(activityObj);
                        int moodScore = parseValue(moodObj);
                        int wellbeingScore = parseValue(wellbeingObj);
                        long timestamp = parseTimestamp(timestampObj);
                        
                        results.add(new TestResult(
                                activityScore,
                                moodScore,
                                wellbeingScore,
                                timestamp
                        ));
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing test result", e);
                    }
                }
                
                // Сортируем результаты по времени (от старых к новым)
                Collections.sort(results, Comparator.comparingLong(TestResult::getTimestamp));
                
                future.complete(results);
            }
            
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error fetching test results", databaseError.toException());
                future.completeExceptionally(databaseError.toException());
            }
        });
        
        // Устанавливаем таймаут для запроса
        scheduleTimeout(future, 10, TimeUnit.SECONDS);
        
        return future;
    }
    
    private static int parseValue(Object value) {
        if (value == null) return 0;
        
        if (value instanceof Long) {
            return ((Long) value).intValue();
        } else if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Double) {
            return ((Double) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
    
    private static long parseTimestamp(Object value) {
        if (value == null) return System.currentTimeMillis();
        
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Double) {
            return ((Double) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return System.currentTimeMillis();
            }
        }
        return System.currentTimeMillis();
    }
    
    public static CompletableFuture<String> getUserName() {
        CompletableFuture<String> future = new CompletableFuture<>();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        if (currentUser == null) {
            future.complete("Гость");
            return future;
        }

        String userId = currentUser.getUid();
        DatabaseReference userRef = database
                .getReference(USERS_PATH)
                .child(userId)
                .child("username");

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String username = dataSnapshot.getValue(String.class);
                future.complete(username != null ? username : "Пользователь");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error fetching username", databaseError.toException());
                future.complete("Пользователь");
            }
        });
        
        // Устанавливаем таймаут для запроса
        scheduleTimeout(future, 5, TimeUnit.SECONDS);
        
        return future;
    }
    
    // Метод для установки таймаута для CompletableFuture
    private static <T> void scheduleTimeout(CompletableFuture<T> future, long timeout, TimeUnit unit) {
        new Thread(() -> {
            try {
                unit.sleep(timeout);
                if (!future.isDone()) {
                    Log.w(TAG, "Firebase request timed out after " + timeout + " " + unit.name());
                    if (future.completeExceptionally(new Exception("Тайм-аут запроса к Firebase"))) {
                        Log.d(TAG, "Timeout applied successfully");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    public static JSONObject prepareTestDataForAI(List<TestResult> testResults, String username) {
        JSONObject data = new JSONObject();
        try {
            data.put("username", username);
            
            JSONArray testsArray = new JSONArray();
            for (TestResult test : testResults) {
                JSONObject testObj = new JSONObject();
                testObj.put("activityScore", test.getActivityScore());
                testObj.put("moodScore", test.getMoodScore());
                testObj.put("wellbeingScore", test.getWellbeingScore());
                testObj.put("timestamp", test.getTimestamp());
                testsArray.put(testObj);
            }
            data.put("tests", testsArray);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON for AI", e);
        }
        return data;
    }
} 
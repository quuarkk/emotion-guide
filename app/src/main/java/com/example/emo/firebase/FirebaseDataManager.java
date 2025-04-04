package com.example.emo.firebase;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.FirebaseApp;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.net.InetAddress;
import java.io.IOException;

public class FirebaseDataManager {
    private static final String TAG = "FirebaseDataManager";
    private static final String USERS_PATH = "Users";
    private static final String TEST_RESULTS_PATH = "TestResults";
    private static final String FIREBASE_URL = "https://emotions-guide-c173c-default-rtdb.europe-west1.firebasedatabase.app";

    private static FirebaseDatabase database;
    private static FirebaseAuth firebaseAuth;
    private static FirebaseUser firebaseUser;
    private static DatabaseReference databaseReference;
    private static Context context;
    
    // Вместо статического блока используем явную инициализацию
    public static void initialize() {
        Log.d(TAG, "Firebase initialization started");
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context);
                Log.d(TAG, "Firebase initialized");
            } else {
                Log.d(TAG, "Firebase already initialized");
            }
            
            // Инициализируем FirebaseAuth
            firebaseAuth = FirebaseAuth.getInstance();
            
            // Получаем экземпляр базы данных с указанием URL
            database = FirebaseDatabase.getInstance(FIREBASE_URL);
            
            // Настройка автономного режима
            try {
                database.setPersistenceEnabled(true);
                Log.d(TAG, "Firebase offline persistence enabled");
            } catch (Exception e) {
                Log.w(TAG, "Firebase offline persistence already enabled or could not be enabled: " + e.getMessage());
            }
            
            // Инициализируем ссылку на базу данных
            databaseReference = database.getReference();
            
            Log.d(TAG, "Firebase Database initialized with URL: " + FIREBASE_URL);
            
            // Проверяем соединение
            testConnection();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase", e);
        }
    }

    private static void testConnection() {
        if (database == null) {
            Log.e(TAG, "Не удается проверить соединение - база данных не инициализирована");
            return;
        }
        
        // Проверяем соединение через .info/connected
        DatabaseReference connectedRef = database.getReference(".info/connected");
        connectedRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class) != null && snapshot.getValue(Boolean.class);
                Log.d(TAG, "Firebase connection status: " + (connected ? "connected" : "disconnected"));
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase connection listener was cancelled", error.toException());
            }
        });
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

    // Метод для получения результатов тестов пользователя (только последние 5)
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

        testResultsRef.limitToLast(5)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        List<TestResult> results = new ArrayList<>();

                        for (DataSnapshot testSnapshot : dataSnapshot.getChildren()) {
                            try {
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

                        Collections.sort(results, Comparator.comparingLong(TestResult::getTimestamp));
                        future.complete(results);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(TAG, "Error fetching test results: " + databaseError.getMessage());
                        future.completeExceptionally(databaseError.toException());
                    }
                });

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

    // Метод для получения имени пользователя из Firebase с таймаутом
    public static CompletableFuture<String> getUserName() {
        CompletableFuture<String> future = new CompletableFuture<>();
        
        try {
            // Проверяем сеть
            if (!isNetworkAvailable()) {
                Log.w(TAG, "Нет сети для получения имени пользователя");
                future.complete("Пользователь");
                return future;
            }
            
            // Проверяем авторизацию
            firebaseUser = firebaseAuth.getCurrentUser();
            if (firebaseUser == null) {
                Log.w(TAG, "Пользователь не авторизован для получения имени");
                future.complete("Гость");
                return future;
            }
            
            String uid = firebaseUser.getUid();
            
            // Получаем SharedPreferences для кэширования
            SharedPreferences sharedPreferences = null;
            if (context != null) {
                try {
                    sharedPreferences = context.getSharedPreferences("firebase_prefs", Context.MODE_PRIVATE);
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка получения SharedPreferences: " + e.getMessage());
                }
            }
            
            // Если есть кэшированное имя, используем его пока загружаем новое
            final String cachedName = sharedPreferences != null ? 
                                     sharedPreferences.getString("username_" + uid, null) : null;
            if (cachedName != null && !cachedName.isEmpty()) {
                Log.d(TAG, "Используем кэшированное имя пользователя: " + cachedName);
                // Запускаем загрузку в фоне, но возвращаем кэш сразу
                loadUsernameFromDatabase(uid, future, sharedPreferences);
                return CompletableFuture.completedFuture(cachedName);
            }
            
            // Загружаем имя пользователя из базы данных
            final SharedPreferences finalSharedPreferences = sharedPreferences;
            loadUsernameFromDatabase(uid, future, finalSharedPreferences);
            
            // Устанавливаем таймаут
            scheduleTimeout(future, 10, TimeUnit.SECONDS);
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении имени пользователя: " + e.getMessage());
            future.complete("Пользователь");
        }
        
        return future;
    }
    
    // Загрузка имени пользователя из базы данных
    private static void loadUsernameFromDatabase(String uid, CompletableFuture<String> future, SharedPreferences prefs) {
        try {
            // Используем Firebase Database напрямую
            FirebaseDatabase db = FirebaseDatabase.getInstance(FIREBASE_URL);
            DatabaseReference nameRef = db.getReference("Users").child(uid).child("username");
            
            nameRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    try {
                        String username = dataSnapshot.getValue(String.class);
                        Log.d(TAG, "Получено имя пользователя: " + username);
                        
                        if (username == null || username.trim().isEmpty()) {
                            username = "Пользователь";
                            Log.w(TAG, "Имя пользователя пустое, используем значение по умолчанию");
                        }
                        
                        // Кэшируем имя пользователя
                        if (prefs != null) {
                            prefs.edit().putString("username_" + uid, username).apply();
                            Log.d(TAG, "Имя пользователя сохранено в кэше");
                        }
                        
                        if (!future.isDone()) {
                            future.complete(username);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при обработке имени пользователя: " + e.getMessage());
                        if (!future.isDone()) {
                            future.complete("Пользователь");
                        }
                    }
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Ошибка загрузки имени пользователя: " + databaseError.getMessage());
                    if (!future.isDone()) {
                        future.complete("Пользователь");
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при доступе к базе данных: " + e.getMessage());
            if (!future.isDone()) {
                future.complete("Пользователь");
            }
        }
    }

    // Вспомогательный метод для пинга хоста
    private static boolean isHostReachable(String host) {
        try {
            // Проверка доступности хоста
            InetAddress address = InetAddress.getByName(host);
            boolean reachable = address.isReachable(5000); // таймаут 5 секунд
            Log.d(TAG, "Пинг " + host + ": " + (reachable ? "успешно" : "неудачно") + ", IP: " + address.getHostAddress());
            return reachable;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при пинге " + host + ": " + e.getMessage(), e);
            return false;
        }
    }

    private static void updateUsernameInBackground(SharedPreferences sharedPreferences) {
        if (firebaseUser == null) {
            Log.w(TAG, "updateUsernameInBackground: пользователь не авторизован");
            return;
        }
        
        try {
            String uid = firebaseUser.getUid();
            Log.d(TAG, "Фоновое обновление имени пользователя для uid: " + uid);
            DatabaseReference nameRef = databaseReference.child("Users").child(uid).child("username");
            
            nameRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    try {
                        String username = dataSnapshot.getValue(String.class);
                        Log.d(TAG, "Получен ответ в фоновом обновлении, имя пользователя: " + username);
                        
                        if (username != null && !username.trim().isEmpty()) {
                            sharedPreferences.edit().putString("KEY_USERNAME", username).apply();
                            Log.d(TAG, "Имя пользователя обновлено в кэше: " + username);
                        } else {
                            Log.w(TAG, "Получено пустое имя пользователя в фоновом обновлении");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при фоновом обновлении имени пользователя: " + e.getMessage(), e);
                    }
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Ошибка при фоновом обновлении имени пользователя: " + databaseError.getMessage() + ", код: " + databaseError.getCode());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Исключение при фоновом обновлении имени пользователя: " + e.getMessage(), e);
        }
    }

    private static <T> void scheduleTimeout(CompletableFuture<T> future, long timeout, TimeUnit unit) {
        scheduleTimeout(future, unit.toMillis(timeout), "Превышено время ожидания запроса после " + timeout + " " + unit.name());
    }
    
    private static <T> void scheduleTimeout(CompletableFuture<T> future, long timeoutMs, String timeoutMessage) {
        if (future.isDone()) return;
        
        // Использование Executors для таймаута вместо создания нового потока
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(() -> {
            if (!future.isDone()) {
                Log.w(TAG, timeoutMessage + " (после " + timeoutMs + " мс)");
                future.completeExceptionally(new TimeoutException(timeoutMessage));
            }
            executor.shutdown();
        }, timeoutMs, TimeUnit.MILLISECONDS);
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

    /**
     * Устанавливает контекст приложения для работы с SharedPreferences
     * @param appContext Контекст приложения
     */
    public static void setApplicationContext(Context appContext) {
        context = appContext.getApplicationContext();
        Log.d(TAG, "Установлен контекст приложения для FirebaseDataManager");
    }

    /**
     * Получает данные пользователя из базы данных Firebase
     * @param userId ID пользователя
     * @return CompletableFuture с данными пользователя
     */
    public static CompletableFuture<Map<String, Object>> getUserData(String userId) {
        Log.d(TAG, "Запрос данных пользователя с userId: " + userId);
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "Получен пустой userId для запроса данных пользователя");
            future.complete(new HashMap<>());
            return future;
        }
        
        try {
            DatabaseReference userRef = databaseReference.child("Users").child(userId);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    try {
                        Map<String, Object> userData = new HashMap<>();
                        
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                                userData.put(childSnapshot.getKey(), childSnapshot.getValue());
                            }
                            Log.d(TAG, "Данные пользователя успешно загружены: " + userData.size() + " полей");
                        } else {
                            Log.w(TAG, "Данные пользователя не найдены");
                        }
                        
                        future.complete(userData);
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при обработке данных пользователя", e);
                        future.completeExceptionally(e);
                    }
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Ошибка получения данных пользователя: " + databaseError.getMessage());
                    future.completeExceptionally(databaseError.toException());
                }
            });
            
            // Устанавливаем таймаут для функции getUserData()
            scheduleTimeout(future, 10, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.e(TAG, "Исключение при получении данных пользователя", e);
            future.completeExceptionally(e);
        }
        
        return future;
    }

    // Метод для прямого чтения имени пользователя из Firebase
    public static void readUsernameDirectly(String uid, final OnUsernameLoadedListener listener) {
        Log.d(TAG, "Прямое чтение имени пользователя из Firebase для UID: " + uid);
        
        if (uid == null || uid.isEmpty()) {
            Log.e(TAG, "UID пользователя пустой или null");
            if (listener != null) {
                listener.onUsernameLoaded("Пользователь", false);
            }
            return;
        }
        
        // Проверка наличия сети
        if (!isNetworkAvailable()) {
            Log.w(TAG, "Нет подключения к интернету для прямого чтения");
            if (listener != null) {
                listener.onUsernameLoaded("Пользователь", false);
            }
            return;
        }
        
        Log.d(TAG, "Проверка сети: доступна");
        Log.d(TAG, "Статус сети для прямого чтения: доступна");
        
        // Получаем экземпляр базы данных
        FirebaseDatabase database = FirebaseDatabase.getInstance(FIREBASE_URL);
        final DatabaseReference usernameRef = database.getReference("Users/" + uid + "/username");
        
        Log.d(TAG, "Путь запроса для прямого чтения: " + usernameRef.toString());
        
        // Устанавливаем таймаут для операции
        final long timeoutMs = 10000; // 10 секунд
        final Handler handler = new Handler(Looper.getMainLooper());
        final Runnable timeoutRunnable = () -> {
            Log.w(TAG, "Таймаут при чтении имени пользователя напрямую");
            if (listener != null) {
                listener.onUsernameLoaded("Пользователь", false);
            }
        };
        
        // Запускаем таймер
        handler.postDelayed(timeoutRunnable, timeoutMs);
        
        // Слушаем однократное значение
        usernameRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Отменяем таймер, так как данные получены
                handler.removeCallbacks(timeoutRunnable);
                
                try {
                    String username = dataSnapshot.getValue(String.class);
                    Log.d(TAG, "Имя пользователя получено напрямую: " + username);
                    
                    if (username == null || username.trim().isEmpty()) {
                        Log.w(TAG, "Полученное имя пользователя пустое или null, используем значение по умолчанию");
                        username = "Пользователь";
                    }
                    
                    if (listener != null) {
                        listener.onUsernameLoaded(username, true);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при обработке полученных данных: " + e.getMessage());
                    if (listener != null) {
                        listener.onUsernameLoaded("Пользователь", false);
                    }
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Отменяем таймер, так как произошла ошибка
                handler.removeCallbacks(timeoutRunnable);
                
                Log.e(TAG, "Ошибка при прямом чтении имени пользователя: " + databaseError.getMessage() + ", код: " + databaseError.getCode());
                if (listener != null) {
                    listener.onUsernameLoaded("Пользователь", false);
                }
            }
        });
    }

    // Интерфейс для обратного вызова при загрузке имени пользователя
    public interface OnUsernameLoadedListener {
        void onUsernameLoaded(String username, boolean success);
    }

    // Проверка доступности сети
    private static boolean isNetworkAvailable() {
        if (context == null) {
            Log.e(TAG, "Ошибка: context == null");
            return false;
        }
        
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            Log.e(TAG, "Ошибка: connectivityManager == null");
            return false;
        }
        
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        boolean isAvailable = activeNetworkInfo != null && activeNetworkInfo.isConnected();
        Log.d(TAG, "Проверка сети: " + (isAvailable ? "доступна" : "недоступна"));
        return isAvailable;
    }

    // Метод для прямого тестирования конфигурации Firebase
    public static void testFirebaseConfiguration() {
        Log.d(TAG, "Начало тестирования конфигурации Firebase");
        
        // Проверка наличия сети
        boolean isNetworkAvailable = isNetworkAvailable();
        Log.d(TAG, "Проверка сети: " + (isNetworkAvailable ? "доступна" : "недоступна"));
        Log.d(TAG, "Сеть доступна: " + isNetworkAvailable);
        
        // Проверка авторизации пользователя
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "Пользователь авторизован: " + currentUser.getUid());
        } else {
            Log.d(TAG, "Пользователь не авторизован");
        }
        
        // Проверка инициализации базы данных
        if (databaseReference != null) {
            Log.d(TAG, "databaseReference инициализирован");
        } else {
            Log.d(TAG, "databaseReference не инициализирован, инициализирую с URL: " + FIREBASE_URL);
            try {
                database = FirebaseDatabase.getInstance(FIREBASE_URL);
                databaseReference = database.getReference();
            } catch (Exception e) {
                Log.e(TAG, "Ошибка инициализации databaseReference", e);
            }
        }
        
        // Вывод текущего URL базы данных
        Log.d(TAG, "URL базы данных: " + FIREBASE_URL);
        
        // Тестирование различных URL
        testDifferentFirebaseUrls();
    }

    // Тестирование различных URL для подключения к Firebase
    private static void testDifferentFirebaseUrls() {
        Log.d(TAG, "Тестирование URL: " + FIREBASE_URL);
        
        // Проверяем пинг до хоста
        pingFirebaseHost(FIREBASE_URL);
        
        // Проверяем соединение через DatabaseReference
        testFirebaseUrl(FIREBASE_URL);
    }
    
    // Тест соединения с конкретным URL
    private static void testFirebaseUrl(String url) {
        try {
            FirebaseDatabase db = FirebaseDatabase.getInstance(url);
            db.getReference(".info/connected").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Boolean connected = dataSnapshot.getValue(Boolean.class);
                    Log.d(TAG, "Соединение с URL " + url + ": " + (connected != null && connected ? "установлено" : "отсутствует"));
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Ошибка соединения с URL " + url + ": " + databaseError.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при тестировании URL " + url + ": " + e.getMessage());
        }
    }

    // Метод для получения URL базы данных
    public static String getDatabaseUrl() {
        return FIREBASE_URL;
    }

    // Метод для проверки доступности хоста
    private static void pingFirebaseHost(String url) {
        try {
            String host = url.replace("https://", "").split("/")[0];
            new Thread(() -> {
                try {
                    InetAddress address = InetAddress.getByName(host);
                    boolean reachable = address.isReachable(5000); // 5 секунд таймаут
                    String ip = address.getHostAddress();
                    Log.d(TAG, "Пинг " + host + ": " + (reachable ? "успешно" : "неудачно") + ", IP: " + ip);
                    Log.d(TAG, "Пинг хоста " + host + ": " + (reachable ? "успешно" : "неудачно"));
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при пинге хоста " + host + ": " + e.getMessage());
                }
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при подготовке пинга хоста: " + e.getMessage());
        }
    }
}
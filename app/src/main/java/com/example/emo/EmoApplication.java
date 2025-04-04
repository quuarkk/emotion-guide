package com.example.emo;

import android.app.Application;
import android.util.Log;

import com.example.emo.firebase.FirebaseDataManager;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import androidx.annotation.NonNull;

public class EmoApplication extends Application {
    private static final String TAG = "EmoApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        
        try {
            // Сначала передаем контекст в FirebaseDataManager
            FirebaseDataManager.setApplicationContext(getApplicationContext());
            
            // Затем инициализируем FirebaseDataManager, который настроит Firebase
            FirebaseDataManager.initialize();
            
            Log.d(TAG, "Firebase успешно инициализирован через FirebaseDataManager");
        } catch (Exception e) {
            Log.e(TAG, "Ошибка инициализации Firebase: " + e.getMessage(), e);
        }
    }
} 
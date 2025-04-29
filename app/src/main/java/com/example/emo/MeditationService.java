package com.example.emo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.IOException;

public class MeditationService extends Service {
    
    private static final String TAG = "MeditationService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "meditation_channel";
    
    private MediaPlayer voicePlayer;    // Для голоса
    private MediaPlayer musicPlayer;    // Для музыки
    private boolean isPlaying = false;
    private String currentTitle = "";
    private int voiceResourceId;
    private int musicResourceId;
    
    // Для контроля баланса между голосом и музыкой (0-100)
    // 0 - только голос, 100 - только музыка, 50 - баланс 50/50
    private int balanceValue = 50;
    
    // Binder для связи с активностью
    private final IBinder binder = new MeditationBinder();
    
    public class MeditationBinder extends Binder {
        public MeditationService getService() {
            return MeditationService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Сервис создан");
        
        // Создаем канал уведомлений для Android 8.0+
        createNotificationChannel();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand вызван");
        
        if (intent != null) {
            String title = intent.getStringExtra("title");
            
            // Получаем ID ресурсов для голоса и музыки
            voiceResourceId = intent.getIntExtra("voiceResourceId", R.raw.meditation_calm_v);
            musicResourceId = intent.getIntExtra("musicResourceId", R.raw.meditation_calm_a);
            
            // Начальный баланс
            balanceValue = intent.getIntExtra("balanceValue", 50);
            
            if (title != null) {
                currentTitle = title;
                
                // Запускаем сервис как foreground service с уведомлением
                startForeground(NOTIFICATION_ID, createNotification(title));
                
                // Если уже есть воспроизведение, останавливаем его
                if (voicePlayer != null || musicPlayer != null) {
                    stopPlayback();
                }
                
                // Запускаем новые треки
                playMeditation(voiceResourceId, musicResourceId);
            }
        }
        
        // Если сервис будет убит системой, он будет перезапущен с последним воспроизводимым треком
        return START_STICKY;
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Медитация",
                    NotificationManager.IMPORTANCE_LOW // LOW важность для отсутствия звука уведомления
            );
            channel.setDescription("Канал уведомлений для медитации");
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    private Notification createNotification(String title) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Воспроизведение медитации")
                .setContentText(title)
                .setSmallIcon(R.drawable.ic_play_circle)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
    
    private void playMeditation(int voiceResId, int musicResId) {
        try {
            // Инициализация плеера для голоса
            voicePlayer = new MediaPlayer();
            voicePlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
            );
            
            // Инициализация плеера для музыки
            musicPlayer = new MediaPlayer();
            musicPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
            );
            
            try {
                Log.d(TAG, "Используем ID ресурсов: голос=" + voiceResId + ", музыка=" + musicResId);
                
                // Подготовка голосового трека
                AssetFileDescriptor voiceAfd = getResources().openRawResourceFd(voiceResId);
                if (voiceAfd != null) {
                    voicePlayer.setDataSource(voiceAfd.getFileDescriptor(), voiceAfd.getStartOffset(), voiceAfd.getLength());
                    voiceAfd.close();
                    voicePlayer.prepare();
                    
                    // Установка слушателя завершения для голоса
                    voicePlayer.setOnCompletionListener(mp -> {
                        Log.d(TAG, "Воспроизведение голоса завершено");
                        checkBothPlayersCompleted();
                    });
                }
                
                // Подготовка музыкального трека
                AssetFileDescriptor musicAfd = getResources().openRawResourceFd(musicResId);
                if (musicAfd != null) {
                    musicPlayer.setDataSource(musicAfd.getFileDescriptor(), musicAfd.getStartOffset(), musicAfd.getLength());
                    musicAfd.close();
                    musicPlayer.prepare();
                    
                    // Установка слушателя завершения для музыки
                    musicPlayer.setOnCompletionListener(mp -> {
                        Log.d(TAG, "Воспроизведение музыки завершено");
                        checkBothPlayersCompleted();
                    });
                }
                
                // Проверяем валидность длительности
                if (voicePlayer.getDuration() <= 0 || musicPlayer.getDuration() <= 0) {
                    Log.w(TAG, "Обнаружена невалидная длительность, устанавливаем таймаут на получение длительности");
                    // Добавляем небольшую задержку для корректного получения длительности
                    new Handler().postDelayed(() -> {
                        Log.d(TAG, "Длительность после задержки: голос=" + voicePlayer.getDuration() + 
                                "мс, музыка=" + musicPlayer.getDuration() + "мс");
                        
                        // Информируем UI о готовности плееров с обновленной длительностью
                        broadcastPlaybackStatus(isPlaying);
                    }, 500);
                }
                
                // Применяем настройки баланса перед стартом
                applyBalanceSettings();
                
                // Запускаем оба трека
                voicePlayer.start();
                musicPlayer.start();
                isPlaying = true;
                
                // Уведомляем фрагмент о начале воспроизведения
                broadcastPlaybackStatus(true);
                
                Log.d(TAG, "Воспроизведение запущено успешно");
            } catch (IOException e) {
                Log.e(TAG, "Ошибка при подготовке медиаплееров: " + e.getMessage(), e);
            } catch (Resources.NotFoundException e) {
                Log.e(TAG, "Ресурс не найден: " + e.getMessage(), e);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при воспроизведении медитации: " + e.getMessage(), e);
        }
    }
    
    // Проверка, завершены ли оба трека
    private void checkBothPlayersCompleted() {
        boolean voiceCompleted = voicePlayer == null || !voicePlayer.isPlaying();
        boolean musicCompleted = musicPlayer == null || !musicPlayer.isPlaying();
        
        if (voiceCompleted && musicCompleted) {
            Log.d(TAG, "Оба трека завершены");
            isPlaying = false;
            
            // Уведомляем фрагмент о завершении воспроизведения
            broadcastPlaybackStatus(false);
            
            // Останавливаем foreground service после завершения трека
            stopForeground(true);
            stopSelf();
        }
    }
    
    // Применение настроек баланса между голосом и музыкой
    public void applyBalanceSettings() {
        if (voicePlayer != null && musicPlayer != null) {
            // Рассчитываем громкость для каждого трека на основе значения баланса
            // Балансируем по громкости звучания от 0 до 1 для каждого трека
            float voiceVolume = 1.0f - (balanceValue / 100.0f);
            float musicVolume = balanceValue / 100.0f;
            
            // Для лучшего звучания, используем нелинейную шкалу
            // Чтобы при balanceValue = 50, оба трека звучали примерно одинаково громко
            if (balanceValue < 50) {
                musicVolume = (balanceValue / 50.0f) * 0.7f;
            } else if (balanceValue > 50) {
                voiceVolume = ((100 - balanceValue) / 50.0f) * 0.7f;
            }
            
            // Применяем громкость к плеерам
            voicePlayer.setVolume(voiceVolume, voiceVolume);
            musicPlayer.setVolume(musicVolume, musicVolume);
            
            Log.d(TAG, "Баланс установлен: голос=" + voiceVolume + ", музыка=" + musicVolume);
        }
    }
    
    // Установка значения баланса (0-100)
    public void setBalance(int newBalance) {
        if (newBalance >= 0 && newBalance <= 100) {
            balanceValue = newBalance;
            applyBalanceSettings();
            
            // Уведомляем UI об изменении баланса
            broadcastPlaybackStatus(isPlaying);
        }
    }
    
    // Получение текущего значения баланса
    public int getBalance() {
        return balanceValue;
    }
    
    // Отправка широковещательного сообщения о статусе воспроизведения
    private void broadcastPlaybackStatus(boolean isPlaying) {
        Intent intent = new Intent("MEDITATION_PLAYBACK_STATUS");
        intent.putExtra("isPlaying", isPlaying);
        intent.putExtra("title", currentTitle);
        intent.putExtra("voiceResourceId", voiceResourceId);
        intent.putExtra("musicResourceId", musicResourceId);
        intent.putExtra("balanceValue", balanceValue);
        
        // Добавляем информацию о длительности и текущей позиции
        if (voicePlayer != null && musicPlayer != null) {
            try {
                // Используем максимальную длительность из двух треков
                int voiceDuration = voicePlayer.getDuration();
                int musicDuration = musicPlayer.getDuration();
                int duration = Math.max(voiceDuration, musicDuration);
                
                // Используем максимальную позицию из двух треков
                int voicePosition = voicePlayer.getCurrentPosition();
                int musicPosition = musicPlayer.getCurrentPosition();
                int position = Math.max(voicePosition, musicPosition);
                
                intent.putExtra("duration", duration);
                intent.putExtra("position", position);
                Log.d(TAG, "Широковещательное сообщение: позиция=" + position + ", длительность=" + duration);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при получении позиции/длительности: " + e.getMessage());
            }
        }
        
        sendBroadcast(intent);
    }
    
    public boolean isPlaying() {
        return isPlaying;
    }
    
    public int getCurrentPosition() {
        if (voicePlayer != null && musicPlayer != null) {
            // Возвращаем максимальную позицию из двух треков
            int voicePosition = voicePlayer.getCurrentPosition();
            int musicPosition = musicPlayer.getCurrentPosition();
            return Math.max(voicePosition, musicPosition);
        }
        return 0;
    }
    
    public int getDuration() {
        if (voicePlayer != null && musicPlayer != null) {
            // Возвращаем максимальную длительность из двух треков
            int voiceDuration = voicePlayer.getDuration();
            int musicDuration = musicPlayer.getDuration();
            int maxDuration = Math.max(voiceDuration, musicDuration);
            
            // Если получили некорректную длительность, возвращаем фиксированное значение
            if (maxDuration <= 0) {
                // Возвращаем статическую длительность 5 минут для корректной работы seekBar
                return 5 * 60 * 1000; // 5 минут в миллисекундах
            }
            return maxDuration;
        }
        return 0;
    }
    
    public void pausePlayback() {
        if (isPlaying) {
            if (voicePlayer != null && voicePlayer.isPlaying()) {
                voicePlayer.pause();
            }
            if (musicPlayer != null && musicPlayer.isPlaying()) {
                musicPlayer.pause();
            }
            isPlaying = false;
            
            // Уведомляем о паузе
            broadcastPlaybackStatus(false);
            
            // Обновляем уведомление
            NotificationManager notificationManager = 
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.notify(NOTIFICATION_ID, createNotification(currentTitle + " (пауза)"));
            }
        }
    }
    
    public void resumePlayback() {
        if (!isPlaying) {
            if (voicePlayer != null) {
                voicePlayer.start();
            }
            if (musicPlayer != null) {
                musicPlayer.start();
            }
            isPlaying = true;
            
            // Уведомляем о возобновлении
            broadcastPlaybackStatus(true);
            
            // Обновляем уведомление
            NotificationManager notificationManager = 
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.notify(NOTIFICATION_ID, createNotification(currentTitle));
            }
        }
    }
    
    public void seekTo(int position) {
        if (voicePlayer != null) {
            voicePlayer.seekTo(position);
        }
        if (musicPlayer != null) {
            musicPlayer.seekTo(position);
        }
    }
    
    private void stopPlayback() {
        if (voicePlayer != null) {
            try {
                voicePlayer.stop();
                voicePlayer.release();
                voicePlayer = null;
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при остановке голосового плеера: " + e.getMessage());
            }
        }
        
        if (musicPlayer != null) {
            try {
                musicPlayer.stop();
                musicPlayer.release();
                musicPlayer = null;
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при остановке музыкального плеера: " + e.getMessage());
            }
        }
        
        isPlaying = false;
        
        // Уведомляем фрагмент об остановке воспроизведения
        broadcastPlaybackStatus(false);
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    @Override
    public void onDestroy() {
        stopPlayback();
        super.onDestroy();
        Log.d(TAG, "Сервис уничтожен");
    }
} 
package com.example.emo;

import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.example.emo.databinding.FragmentMeditationBinding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MeditationFragment extends Fragment {

    private FragmentMeditationBinding binding;
    private LinearLayout meditationContainer;
    private List<Meditation> meditations;
    private MediaPlayer mediaPlayer;
    private Handler handler;
    private Runnable runnable;
    private boolean isPlaying = false;
    private int currentPlayingPosition = -1;
    private ConstraintLayout playerLayout;
    private TextView currentTrackTitle;
    private TextView timeElapsed;
    private TextView timeTotal;
    private SeekBar seekBar;
    private ImageButton playPauseButton;
    private ValueAnimator colorAnimator;
    private GradientDrawable gradientDrawable;
    private static final String TAG = "MeditationFragment";
    
    // Добавляем переменную для SeekBar баланса
    private SeekBar balanceSeekBar;
    
    // Добавляем переменную для регулировки громкости
    private SeekBar volumeSeekBar;
    
    // Добавляем переменные для сохранения состояния баланса
    private static final String PREFS_NAME = "MeditationPrefs";
    private static final String KEY_BALANCE_VALUE = "balanceValue";
    private SharedPreferences preferences;
    private int savedBalanceValue = 50; // Значение по умолчанию
    
    // Добавляем новые поля для работы с сервисом
    private MeditationService meditationService;
    private boolean serviceBound = false;
    private BroadcastReceiver playbackStatusReceiver;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentMeditationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Получаем сохраненное значение баланса
        preferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        savedBalanceValue = preferences.getInt(KEY_BALANCE_VALUE, 50);
        
        meditationContainer = binding.meditationContainer;
        playerLayout = binding.playerLayout;
        currentTrackTitle = binding.currentTrackTitle;
        timeElapsed = binding.timeElapsed;
        timeTotal = binding.timeTotal;
        seekBar = binding.seekBar;
        playPauseButton = binding.playPauseButton;
        balanceSeekBar = binding.balanceSeekBar; // Инициализируем ползунок баланса
        
        // Получаем громкость системы и устанавливаем в SeekBar
        volumeSeekBar = binding.volumeSeekBar;
        initVolumeControl();
        
        // Устанавливаем сохраненное значение баланса
        balanceSeekBar.setProgress(savedBalanceValue);
        
        // Начальные настройки
        playerLayout.setVisibility(View.GONE);
        handler = new Handler();
        
        // Инициализация медитаций
        initializeMeditations();
        
        // Настройка анимации фона
        setupBackgroundAnimation();
        
        // Отображение медитаций
        displayMeditations();
        
        // Настройка медиаплеера
        setupMediaPlayerControls();
        
        // Настройка ползунка баланса
        setupBalanceControl();
        
        // Регистрируем приемник для обновления UI из сервиса
        setupPlaybackStatusReceiver();
        
        // Проверяем, возможно сервис уже запущен
        bindToMeditationService();
    }
    
    private void setupBackgroundAnimation() {
        try {
            // Создаем градиентный фон с плавной анимацией множества цветов
            gradientDrawable = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR, // Диагональный градиент
                    new int[] {
                            ContextCompat.getColor(requireContext(), R.color.meditation_gradient_start),
                            ContextCompat.getColor(requireContext(), R.color.meditation_gradient_mid1),
                            ContextCompat.getColor(requireContext(), R.color.meditation_gradient_mid2),
                            ContextCompat.getColor(requireContext(), R.color.meditation_gradient_mid3),
                            ContextCompat.getColor(requireContext(), R.color.meditation_gradient_end)
                    }
            );
            gradientDrawable.setCornerRadius(0f);
            binding.meditationRoot.setBackground(gradientDrawable);
            
            // Расширенная анимация градиента с множеством цветов
            colorAnimator = ValueAnimator.ofFloat(0f, 1f);
            colorAnimator.setDuration(30000); // 30 секунд на полный цикл для более плавной анимации
            colorAnimator.setRepeatCount(ValueAnimator.INFINITE);
            colorAnimator.setRepeatMode(ValueAnimator.REVERSE);
            colorAnimator.setInterpolator(new LinearInterpolator());
            colorAnimator.addUpdateListener(animation -> {
                float position = (float) animation.getAnimatedValue();
                
                // Создаем массив из 9 цветов для более плавного градиента
                int[] colors = new int[9];
                
                // Вычисляем цвета для текущей позиции анимации
                if (position < 0.25f) {
                    // Первая четверть анимации
                    colors[0] = ContextCompat.getColor(requireContext(), R.color.meditation_gradient_start);
                    colors[1] = ContextCompat.getColor(requireContext(), R.color.meditation_gradient_mid1);
                    colors[2] = ContextCompat.getColor(requireContext(), R.color.meditation_gradient_mid2);
                    colors[3] = ContextCompat.getColor(requireContext(), R.color.meditation_gradient_mid3);
                    colors[4] = ContextCompat.getColor(requireContext(), R.color.meditation_gradient_mid4);
                    colors[5] = ContextCompat.getColor(requireContext(), R.color.meditation_gradient_mid5);
                    colors[6] = ContextCompat.getColor(requireContext(), R.color.meditation_gradient_mid6);
                    colors[7] = ContextCompat.getColor(requireContext(), R.color.meditation_gradient_mid7);
                    colors[8] = ContextCompat.getColor(requireContext(), R.color.meditation_gradient_end);
                } else if (position < 0.5f) {
                    // Вторая четверть анимации
                    colors[0] = ContextCompat.getColor(requireContext(), R.color.meditation_gradient_mid2);
                    colors[1] = ContextCompat.getColor(requireContext(), R.color.meditation_gradient_mid3);
                    colors[2] = ContextCompat.getColor(requireContext(), R.color.meditation_gradient_mid4);
                    colors[3] = ContextCompat.getColor(requireContext(), R.color.meditation_gradient_mid5);
                    colors[4] = ContextCompat.getColor(requireContext(), R.color.meditation_gradient_mid6);
                    colors[5] = ContextCompat.getColor(requireContext(), R.color.meditation_gradient_mid7);
                    colors[6] = ContextCompat.getColor(requireContext(), R.color.meditation_gradient_end);
                    colors[7] = ContextCompat.getColor(requireContext(), R.color.meditation_gradient_start);
                    colors[8] = ContextCompat.getColor(requireContext(), R.color.meditation_gradient_mid1);
                } else if (position < 0.75f) {
                    // Третья четверть анимации
                    colors[0] = ContextCompat.getColor(requireContext(), R.color.meditation_gradient_mid5);
                    colors[1] = ContextCompat.getColor(requireContext(), R.color.meditation_gradient_mid6);
                    colors[2] = ContextCompat.getColor(requireContext(), R.color.meditation_gradient_mid7);
                    colors[3] = ContextCompat.getColor(requireContext(), R.color.meditation_gradient_end);
                    colors[4] = ContextCompat.getColor(requireContext(), R.color.meditation_gradient_start);
                    colors[5] = ContextCompat.getColor(requireContext(), R.color.meditation_gradient_mid1);
                    colors[6] = ContextCompat.getColor(requireContext(), R.color.meditation_gradient_mid2);
                    colors[7] = ContextCompat.getColor(requireContext(), R.color.meditation_gradient_mid3);
                    colors[8] = ContextCompat.getColor(requireContext(), R.color.meditation_gradient_mid4);
                } else {
                    // Последняя четверть анимации
                    colors[0] = ContextCompat.getColor(requireContext(), R.color.meditation_gradient_mid7);
                    colors[1] = ContextCompat.getColor(requireContext(), R.color.meditation_gradient_end);
                    colors[2] = ContextCompat.getColor(requireContext(), R.color.meditation_gradient_start);
                    colors[3] = ContextCompat.getColor(requireContext(), R.color.meditation_gradient_mid1);
                    colors[4] = ContextCompat.getColor(requireContext(), R.color.meditation_gradient_mid2);
                    colors[5] = ContextCompat.getColor(requireContext(), R.color.meditation_gradient_mid3);
                    colors[6] = ContextCompat.getColor(requireContext(), R.color.meditation_gradient_mid4);
                    colors[7] = ContextCompat.getColor(requireContext(), R.color.meditation_gradient_mid5);
                    colors[8] = ContextCompat.getColor(requireContext(), R.color.meditation_gradient_mid6);
                }
                
                gradientDrawable.setColors(colors);
                binding.meditationRoot.invalidate();
            });
            
            colorAnimator.start();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при настройке анимации: " + e.getMessage());
        }
    }
    
    // Вспомогательный метод для плавного перехода между цветами
    private int interpolateColor(int colorStart, int colorEnd, float fraction) {
        int startA = (colorStart >> 24) & 0xff;
        int startR = (colorStart >> 16) & 0xff;
        int startG = (colorStart >> 8) & 0xff;
        int startB = colorStart & 0xff;
        
        int endA = (colorEnd >> 24) & 0xff;
        int endR = (colorEnd >> 16) & 0xff;
        int endG = (colorEnd >> 8) & 0xff;
        int endB = colorEnd & 0xff;
        
        int a = (int) (startA + fraction * (endA - startA));
        int r = (int) (startR + fraction * (endR - startR));
        int g = (int) (startG + fraction * (endG - startG));
        int b = (int) (startB + fraction * (endB - startB));
        
        return ((a & 0xff) << 24) | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
    }
    
    private void initializeMeditations() {
        meditations = new ArrayList<>();
        
        // Добавляем медитацию для спокойствия с эстетичным описанием
        meditations.add(
            new Meditation(
                "Медитация для спокойствия",
                "Эта медитация поможет тебе сбросить напряжение, успокоить ум и вернуть ощущение гармонии. " +
                "Ты постепенно погрузишься в состояние мягкого расслабления, освобождаясь от стресса и тревог, " +
                "которые накапливаются в теле и сознании.",
                "meditation_calm.mp3", // MP3 файл
                R.raw.meditation_calm_v, // Голос
                R.raw.meditation_calm_a  // Музыкальное сопровождение
            )
        );
        
        // Добавляем медитацию Medula
        meditations.add(
            new Meditation(
                "Медитация для гармонии",
                "Медитация поможет тебе восстановить энергетический баланс и глубоко расслабиться. " +
                "Почувствуй, как с каждым вдохом твое тело наполняется жизненной силой, а с каждым выдохом " +
                "уходит усталость и напряжение, накопившееся за день.",
                "medula.mp3", // MP3 файл
                R.raw.medula_v, // Голос
                R.raw.medula_a  // Музыкальное сопровождение
            )
        );
        

    }
    
    private void displayMeditations() {
        // Очищаем контейнер
        meditationContainer.removeAllViews();
        
        // Создаем карточки для каждой медитации
        for (int i = 0; i < meditations.size(); i++) {
            final int position = i;
            Meditation meditation = meditations.get(i);
            
            // Создаем карточку
            CardView cardView = new CardView(getContext());
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 16, 0, 16);
            cardView.setLayoutParams(cardParams);
            cardView.setCardElevation(8);
            cardView.setRadius(16);
            
            // Создаем градиентный фон для карточки, соответствующий анимированному фону
            GradientDrawable cardGradient = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[] {
                            ContextCompat.getColor(requireContext(), R.color.meditation_gradient_start),
                            ContextCompat.getColor(requireContext(), R.color.meditation_gradient_mid3),
                            ContextCompat.getColor(requireContext(), R.color.meditation_gradient_mid6),
                    }
            );
            cardGradient.setCornerRadius(16f);
            cardView.setBackground(cardGradient);
            
            // Контейнер для содержимого карточки
            LinearLayout cardContent = new LinearLayout(getContext());
            LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardContent.setLayoutParams(contentParams);
            cardContent.setOrientation(LinearLayout.VERTICAL);
            cardContent.setPadding(24, 24, 24, 24);
            cardView.addView(cardContent);
            
            // Заголовок медитации
            TextView titleView = new TextView(getContext());
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            titleView.setLayoutParams(titleParams);
            titleView.setText(meditation.getTitle());
            titleView.setTextColor(ContextCompat.getColor(requireContext(), R.color.meditation_text_color));
            titleView.setTextSize(18);
            titleView.setTypeface(null, android.graphics.Typeface.BOLD);
            titleView.setPadding(0, 0, 0, 16);
            cardContent.addView(titleView);
            
            // Эстетичное описание медитации, стилизованное в соответствии с градиентом
            TextView descriptionView = new TextView(getContext());
            LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            descriptionView.setLayoutParams(descParams);
            descriptionView.setText(meditation.getDescription());
            descriptionView.setTextColor(ContextCompat.getColor(requireContext(), R.color.meditation_text_color));
            descriptionView.setTextSize(14);
            descriptionView.setLineSpacing(8, 1.2f); // Увеличиваем межстрочный интервал для эстетичности
            descriptionView.setPadding(0, 0, 0, 24);
            // Добавляем тень к тексту для лучшей видимости на градиентном фоне
            descriptionView.setShadowLayer(3, 1, 1, Color.parseColor("#80000000"));
            cardContent.addView(descriptionView);
            
            // Кнопка воспроизведения
            Button playButton = new Button(getContext());
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            buttonParams.gravity = android.view.Gravity.END;
            playButton.setLayoutParams(buttonParams);
            playButton.setText("Слушать");
            playButton.setTextColor(Color.WHITE);
            playButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.meditation_accent)));
            playButton.setOnClickListener(v -> playMeditation(position));
            cardContent.addView(playButton);
            
            // Добавляем карточку в контейнер
            meditationContainer.addView(cardView);
        }
    }
    
    private void setupMediaPlayerControls() {
        // Настройка кнопки воспроизведения/паузы
        playPauseButton.setOnClickListener(v -> {
            if (serviceBound && meditationService != null) {
                if (meditationService.isPlaying()) {
                    meditationService.pausePlayback();
                    playPauseButton.setImageResource(R.drawable.ic_play_circle);
                } else {
                    meditationService.resumePlayback();
                    playPauseButton.setImageResource(R.drawable.ic_pause_circle);
                }
                isPlaying = meditationService.isPlaying();
            }
        });
        
        // Настройка seekBar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (serviceBound && meditationService != null && fromUser) {
                    meditationService.seekTo(progress);
                    updateTime(progress, meditationService.getDuration());
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Останавливаем обновление seekBar при перемещении пользователем
                handler.removeCallbacks(runnable);
            }
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Возобновляем обновление seekBar
                if (serviceBound && meditationService != null && isPlaying) {
                    handler.postDelayed(runnable, 1000);
                }
            }
        });
    }
    
    // Настройка управления балансом между голосом и музыкой
    private void setupBalanceControl() {
        balanceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (serviceBound && meditationService != null && fromUser) {
                    meditationService.setBalance(progress);
                    // Сохраняем новое значение баланса
                    saveBalanceValue(progress);
                    Log.d(TAG, "Баланс установлен и сохранен: " + progress);
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Ничего не делаем
            }
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Сохраняем значение при прекращении взаимодействия
                saveBalanceValue(seekBar.getProgress());
            }
        });
    }
    
    // Метод для сохранения значения баланса
    private void saveBalanceValue(int value) {
        savedBalanceValue = value;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(KEY_BALANCE_VALUE, value);
        editor.apply();
    }
    
    private void setupPlaybackStatusReceiver() {
        playbackStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() != null && intent.getAction().equals("MEDITATION_PLAYBACK_STATUS")) {
                    boolean playingStatus = intent.getBooleanExtra("isPlaying", false);
                    String title = intent.getStringExtra("title");
                    
                    // Получаем информацию о длительности и позиции из сообщения
                    int duration = intent.getIntExtra("duration", 0);
                    int position = intent.getIntExtra("position", 0);
                    
                    // Получаем значение баланса
                    int balanceValue = intent.getIntExtra("balanceValue", savedBalanceValue);
                    
                    Log.d(TAG, "Получено сообщение: isPlaying=" + playingStatus + 
                            ", position=" + position + ", duration=" + duration + 
                            ", balance=" + balanceValue);
                    
                    // Обновляем UI
                    isPlaying = playingStatus;
                    if (title != null && !title.isEmpty()) {
                        currentTrackTitle.setText(title);
                    }
                    
                    // Обновляем иконку воспроизведения/паузы
                    playPauseButton.setImageResource(isPlaying ? 
                            R.drawable.ic_pause_circle : R.drawable.ic_play_circle);
                    
                    // Показываем плеер
                    playerLayout.setVisibility(View.VISIBLE);
                    
                    // Обновляем ползунок баланса и сохраняем значение
                    if (balanceValue != balanceSeekBar.getProgress()) {
                        balanceSeekBar.setProgress(balanceValue);
                        saveBalanceValue(balanceValue);
                    }
                    
                    // Устанавливаем максимальное значение для seekBar и текущую позицию
                    if (duration > 0) {
                        seekBar.setMax(duration);
                        if (position >= 0 && position <= duration) {
                            seekBar.setProgress(position);
                            updateTime(position, duration);
                        }
                    }
                    
                    // Обновляем seekBar если медитация воспроизводится
                    if (isPlaying) {
                        updateSeekBar();
                    }
                }
            }
        };
        
        // Регистрируем приемник
        IntentFilter filter = new IntentFilter("MEDITATION_PLAYBACK_STATUS");
        requireActivity().registerReceiver(playbackStatusReceiver, filter);
    }
    
    private void bindToMeditationService() {
        Intent intent = new Intent(requireContext(), MeditationService.class);
        requireActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
    
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MeditationService.MeditationBinder binder = (MeditationService.MeditationBinder) service;
            meditationService = binder.getService();
            serviceBound = true;
            
            Log.d(TAG, "Сервис медитации подключен");
            
            // Если сервис уже воспроизводит медитацию, обновляем UI
            if (meditationService.isPlaying()) {
                isPlaying = true;
                playerLayout.setVisibility(View.VISIBLE);
                playPauseButton.setImageResource(R.drawable.ic_pause_circle);
                
                // Устанавливаем сохраненное значение баланса в сервисе
                meditationService.setBalance(savedBalanceValue);
                
                // Обновляем seekBar
                int duration = meditationService.getDuration();
                seekBar.setMax(duration);
                updateSeekBar();
            }
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            meditationService = null;
            Log.d(TAG, "Сервис медитации отключен");
        }
    };
    
    private void playMeditation(int position) {
        try {
            // Отображаем плеер
            playerLayout.setVisibility(View.VISIBLE);
            currentPlayingPosition = position;
            
            // Устанавливаем название трека
            Meditation meditation = meditations.get(position);
            currentTrackTitle.setText(meditation.getTitle());
            
            // Сбрасываем seekBar для предотвращения скачка в конец
            seekBar.setProgress(0);
            updateTime(0, 0);
            
            // Устанавливаем значение баланса из сохраненных настроек
            balanceSeekBar.setProgress(savedBalanceValue);
            
            // Отображаем иконку загрузки в ожидании инициализации медиаплеера
            playPauseButton.setImageResource(R.drawable.ic_pause_circle);
            
            // Запускаем сервис для фонового воспроизведения
            Intent serviceIntent = new Intent(requireContext(), MeditationService.class);
            serviceIntent.putExtra("title", meditation.getTitle());
            serviceIntent.putExtra("voiceResourceId", meditation.getVoiceResourceId());
            serviceIntent.putExtra("musicResourceId", meditation.getMusicResourceId());
            serviceIntent.putExtra("balanceValue", savedBalanceValue); // Используем сохраненное значение баланса
            
            requireActivity().startService(serviceIntent);
            
            // Подключаемся к сервису, если еще не подключены
            if (!serviceBound) {
                bindToMeditationService();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при воспроизведении медитации: " + e.getMessage(), e);
        }
    }
    
    private void updateSeekBar() {
        if (getActivity() == null || !serviceBound || meditationService == null) return;
        
        try {
            // Обновляем позицию seekBar
            int currentPosition = meditationService.getCurrentPosition();
            int duration = meditationService.getDuration();
            
            // Проверяем валидность значений
            if (currentPosition >= 0 && duration > 0 && currentPosition <= duration) {
                seekBar.setMax(duration);
                seekBar.setProgress(currentPosition);
                updateTime(currentPosition, duration);
                
                // Логируем данные для отладки
                Log.d(TAG, "Позиция: " + currentPosition + " / " + duration);
            }
            
            // Настраиваем повторяющееся обновление
            runnable = () -> {
                if (serviceBound && meditationService != null && isPlaying) {
                    updateSeekBar();
                }
            };
            handler.postDelayed(runnable, 1000);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при обновлении seekBar: " + e.getMessage());
        }
    }
    
    private void updateTime(int currentPosition, int duration) {
        // Форматируем время в формат мм:сс
        timeElapsed.setText(formatTime(currentPosition));
        timeTotal.setText(formatTime(duration));
    }
    
    private String formatTime(int timeMs) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeMs);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeMs) - 
                TimeUnit.MINUTES.toSeconds(minutes);
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    private void releaseMediaPlayer() {
        // Освобождение ресурсов теперь не нужно, так как управление передано сервису
        handler.removeCallbacks(runnable);
    }
    
    @Override
    public void onPause() {
        super.onPause();
        
        // Останавливаем только анимацию градиента, но НЕ останавливаем воспроизведение медитации
        if (colorAnimator != null) {
            colorAnimator.pause();
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (colorAnimator != null && !colorAnimator.isRunning()) {
            colorAnimator.resume();
        }
        
        // Подключаемся к сервису при возвращении в приложение
        if (!serviceBound) {
            bindToMeditationService();
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Отсоединяемся от сервиса
        if (serviceBound) {
            requireActivity().unbindService(serviceConnection);
            serviceBound = false;
        }
        
        // Отменяем регистрацию ресивера
        if (playbackStatusReceiver != null) {
            requireActivity().unregisterReceiver(playbackStatusReceiver);
        }
        
        // Останавливаем обновление seekBar
        handler.removeCallbacks(runnable);
        
        if (colorAnimator != null) {
            colorAnimator.cancel();
            colorAnimator = null;
        }
        
        binding = null;
    }
    
    // Настройка управления громкостью
    private void initVolumeControl() {
        try {
            // Получаем AudioManager для управления громкостью
            final android.media.AudioManager audioManager = 
                    (android.media.AudioManager) requireActivity().getSystemService(Context.AUDIO_SERVICE);
            
            // Устанавливаем максимальное значение
            int maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC);
            volumeSeekBar.setMax(maxVolume);
            
            // Устанавливаем текущее значение
            int currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC);
            volumeSeekBar.setProgress(currentVolume);
            
            // Обработчик изменения громкости
            volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        audioManager.setStreamVolume(
                                android.media.AudioManager.STREAM_MUSIC,
                                progress,
                                0);
                    }
                }
                
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    // Ничего не делаем
                }
                
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    // Ничего не делаем
                }
            });
            
            // Иконки громкости меняют цвет в зависимости от уровня громкости
            updateVolumeIcons(currentVolume, maxVolume);
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при настройке контроля громкости: " + e.getMessage());
        }
    }
    
    // Обновление иконок громкости
    private void updateVolumeIcons(int currentVolume, int maxVolume) {
        ImageView lowIcon = binding.volumeLowIcon;
        ImageView highIcon = binding.volumeHighIcon;
        
        float volume = (float) currentVolume / maxVolume;
        
        if (volume < 0.3f) {
            lowIcon.setAlpha(1.0f);
            highIcon.setAlpha(0.3f);
        } else if (volume < 0.7f) {
            lowIcon.setAlpha(0.7f);
            highIcon.setAlpha(0.7f);
        } else {
            lowIcon.setAlpha(0.3f);
            highIcon.setAlpha(1.0f);
        }
    }
    
    // Класс для хранения информации о медитации
    private static class Meditation {
        private final String title;
        private final String description;
        private final String audioPath;
        private final int voiceResourceId;
        private final int musicResourceId;
        
        public Meditation(String title, String description, String audioPath, 
                         int voiceResourceId, int musicResourceId) {
            this.title = title;
            this.description = description;
            this.audioPath = audioPath;
            this.voiceResourceId = voiceResourceId;
            this.musicResourceId = musicResourceId;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getAudioPath() {
            return audioPath;
        }
        
        public int getVoiceResourceId() {
            return voiceResourceId;
        }
        
        public int getMusicResourceId() {
            return musicResourceId;
        }
    }
} 
package com.example.emo;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emo.databinding.FragmentAiPsychologistBinding;

import java.util.ArrayList;
import java.util.List;

public class AiPsychologistFragment extends Fragment {

    private FragmentAiPsychologistBinding binding;
    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private Button sendButton;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private static final String TAG = "AiPsychologistFragment";

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentAiPsychologistBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        Log.d(TAG, "onViewCreated вызван");

        chatRecyclerView = binding.chatRecyclerView;
        messageInput = binding.messageInput;
        sendButton = binding.sendButton;

        // Инициализация списка сообщений
        chatMessages = new ArrayList<>();
        chatMessages.add(new ChatMessage("Здравствуйте! Я ваш ИИ-психолог. Расскажите, что вас беспокоит?", false));

        // Настройка RecyclerView
        chatAdapter = new ChatAdapter(chatMessages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true); // Прокрутка к последнему сообщению
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);

        // Обработчик нажатия на кнопку отправки
        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
                messageInput.setText("");
                hideKeyboard();
            }
        });
        
        // Настройка быстрых ответов
        setupQuickReplies();
        
        // Упрощаем обработку клавиатуры
        messageInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // Прокручиваем к последнему сообщению с задержкой
                new Handler().postDelayed(() -> 
                    chatRecyclerView.scrollToPosition(chatMessages.size() - 1), 
                    300);
            }
        });
        
        // Добавляем слушатель изменения размера для корневого представления
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                view.getWindowVisibleDisplayFrame(r);
                int screenHeight = view.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;
                
                if (keypadHeight > screenHeight * 0.15) { // Клавиатура видна
                    // Прокручиваем к последнему сообщению
                    chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                }
            }
        });
    }

    private void setupQuickReplies() {
        // Настраиваем обработчики для кнопок быстрых ответов
        binding.quickReply1.setOnClickListener(v -> {
            sendMessage("Мне грустно");
            hideKeyboard();
        });
        
        binding.quickReply2.setOnClickListener(v -> {
            sendMessage("Я чувствую тревогу");
            hideKeyboard();
        });
        
        binding.quickReply3.setOnClickListener(v -> {
            sendMessage("Мне нужен совет");
            hideKeyboard();
        });
        
        binding.quickReply4.setOnClickListener(v -> {
            sendMessage("Расскажи о себе");
            hideKeyboard();
        });
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(messageInput.getWindowToken(), 0);
    }

    private void sendMessage(String message) {
        // Добавляем сообщение пользователя
        chatMessages.add(new ChatMessage(message, true));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);

        // Имитация ответа ИИ с задержкой
        new Handler().postDelayed(() -> {
            String response = generateResponse(message);
            chatMessages.add(new ChatMessage(response, false));
            chatAdapter.notifyItemInserted(chatMessages.size() - 1);
            chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
        }, 1000);
    }

    private String generateResponse(String message) {
        // Простая логика для генерации ответов
        message = message.toLowerCase();
        
        if (message.contains("грустно") || message.contains("печаль") || message.contains("депрессия")) {
            return "Я понимаю, что вам сейчас тяжело. Расскажите подробнее, что вызывает у вас такие чувства?";
        } else if (message.contains("страх") || message.contains("боюсь") || message.contains("тревога")) {
            return "Тревога и страх - это нормальные эмоции. Давайте разберемся, что именно вызывает у вас беспокойство.";
        } else if (message.contains("счастлив") || message.contains("радость") || message.contains("хорошо")) {
            return "Я рад, что у вас хорошее настроение! Что именно вызывает у вас такие положительные эмоции?";
        } else if (message.contains("злость") || message.contains("гнев") || message.contains("раздражение")) {
            return "Гнев - это сильная эмоция. Важно понять его причины. Что вызывает у вас такую реакцию?";
        } else if (message.contains("спасибо") || message.contains("благодарю")) {
            return "Всегда рад помочь! Есть ли что-то еще, о чем вы хотели бы поговорить?";
        } else {
            return "Спасибо, что поделились этим. Расскажите, пожалуйста, подробнее о своих чувствах в этой ситуации.";
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    
    // Класс для представления сообщения в чате
    public static class ChatMessage {
        private final String text;
        private final boolean isUser;

        public ChatMessage(String text, boolean isUser) {
            this.text = text;
            this.isUser = isUser;
        }

        public String getText() {
            return text;
        }

        public boolean isUser() {
            return isUser;
        }
    }
    
    // Адаптер для RecyclerView
    private class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
        private final List<ChatMessage> messages;

        public ChatAdapter(List<ChatMessage> messages) {
            this.messages = messages;
        }

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_message, parent, false);
            return new ChatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            ChatMessage message = messages.get(position);
            holder.messageText.setText(message.getText());
            
            // Настройка внешнего вида в зависимости от отправителя
            if (message.isUser()) {
                holder.messageText.setBackgroundResource(R.drawable.bg_user_message);
                holder.messageText.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
            } else {
                holder.messageText.setBackgroundResource(R.drawable.bg_ai_message);
                holder.messageText.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            }
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        class ChatViewHolder extends RecyclerView.ViewHolder {
            TextView messageText;

            ChatViewHolder(View itemView) {
                super(itemView);
                messageText = itemView.findViewById(R.id.message_text);
            }
        }
    }
} 
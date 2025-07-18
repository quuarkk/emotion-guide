package com.example.emo.viewmodels;

import android.os.Looper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.emo.models.ChatMessage;
import java.util.ArrayList;
import java.util.List;

public class AiPsychologistViewModel extends ViewModel {
    private final MutableLiveData<List<ChatMessage>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isRequestActive = new MutableLiveData<>(false);

    public LiveData<List<ChatMessage>> getMessages() {
        return messages;
    }

    public LiveData<Boolean> getIsRequestActive() {
        return isRequestActive;
    }

    public void setMessages(List<ChatMessage> newMessages) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            messages.setValue(new ArrayList<>(newMessages));
        } else {
            throw new IllegalStateException("Cannot invoke setValue on a background thread");
        }
    }

    public void addMessage(ChatMessage message) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            List<ChatMessage> currentMessages = messages.getValue();
            if (currentMessages != null) {
                currentMessages.add(message);
                messages.setValue(currentMessages);
            }
        } else {
            throw new IllegalStateException("Cannot invoke setValue on a background thread");
        }
    }

    public void setRequestActive(boolean active) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            isRequestActive.setValue(active);
        } else {
            throw new IllegalStateException("Cannot invoke setValue on a background thread");
        }
    }

    public void updateLastMessage(String content, boolean isLoading) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            List<ChatMessage> currentMessages = messages.getValue();
            if (currentMessages != null && !currentMessages.isEmpty()) {
                ChatMessage lastMessage = currentMessages.get(currentMessages.size() - 1);
                if (lastMessage.getType() == ChatMessage.TYPE_AI) {
                    lastMessage.setContent(content);
                    lastMessage.setLoading(isLoading);
                    messages.setValue(currentMessages);
                }
            }
        } else {
            throw new IllegalStateException("Cannot invoke setValue on a background thread");
        }
    }

    public void removeLastMessage() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            List<ChatMessage> currentMessages = messages.getValue();
            if (currentMessages != null && !currentMessages.isEmpty()) {
                currentMessages.remove(currentMessages.size() - 1);
                messages.setValue(currentMessages);
            }
        } else {
            throw new IllegalStateException("Cannot invoke setValue on a background thread");
        }
    }
} 
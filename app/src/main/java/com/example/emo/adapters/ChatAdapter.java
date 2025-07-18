package com.example.emo.ai;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emo.R;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_AI = 1;
    private static final int VIEW_TYPE_USER = 2;

    private List<ChatMessage> messages = new ArrayList<>();

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_AI) {
            View view = inflater.inflate(R.layout.item_chat_message, parent, false);
            return new AiMessageViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_user_message, parent, false);
            return new UserMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (holder instanceof AiMessageViewHolder) {
            ((AiMessageViewHolder) holder).bind(message);
        } else if (holder instanceof UserMessageViewHolder) {
            ((UserMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        return message.isFromAi() ? VIEW_TYPE_AI : VIEW_TYPE_USER;
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void updateLastMessage(String text) {
        if (!messages.isEmpty()) {
            int lastIndex = messages.size() - 1;
            ChatMessage lastMessage = messages.get(lastIndex);
            lastMessage.setText(text);
            notifyItemChanged(lastIndex);
        }
    }

    static class AiMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageText;
        private final ProgressBar progressBar;

        AiMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            progressBar = itemView.findViewById(R.id.message_progress);
        }

        void bind(ChatMessage message) {
            messageText.setText(message.getText());
            progressBar.setVisibility(message.isLoading() ? View.VISIBLE : View.GONE);
        }
    }

    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageText;

        UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
        }

        void bind(ChatMessage message) {
            messageText.setText(message.getText());
        }
    }
} 
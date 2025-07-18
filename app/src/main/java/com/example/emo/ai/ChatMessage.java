package com.example.emo.ai;

public class ChatMessage {
    private String text;
    private boolean isFromAi;
    private boolean isLoading;

    public ChatMessage(String text, boolean isFromAi) {
        this.text = text;
        this.isFromAi = isFromAi;
        this.isLoading = false;
    }

    public ChatMessage(String text, boolean isFromAi, boolean isLoading) {
        this.text = text;
        this.isFromAi = isFromAi;
        this.isLoading = isLoading;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isFromAi() {
        return isFromAi;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public void setLoading(boolean loading) {
        isLoading = loading;
    }
} 
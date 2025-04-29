package com.example.emo.openai;

import java.util.List;

public class ChatRequest {
    private String model;
    private List<Message> messages;
    private double temperature;

    public ChatRequest(String model, List<Message> messages, double temperature) {
        this.model = model;
        this.messages = messages;
        this.temperature = temperature;
    }

    public static class Message {
        private String role;
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
} 
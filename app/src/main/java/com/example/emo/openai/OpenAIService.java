package com.example.emo.openai;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface OpenAIService {
    @POST("v1/chat/completions")
    Call<ChatResponse> createChatCompletion(
            @Header("Authorization") String authorization,
            @Body ChatRequest request
    );
} 
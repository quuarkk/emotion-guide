package com.example.emo.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT :limit")
    List<ChatMessageEntity> getLastMessages(int limit);

    @Insert
    void insert(ChatMessageEntity message);

    @Query("DELETE FROM chat_messages WHERE id NOT IN (SELECT id FROM chat_messages ORDER BY timestamp DESC LIMIT :limit)")
    void deleteOldMessages(int limit);

    @Transaction
    default void insertAndMaintainLimit(ChatMessageEntity message, int limit) {
        insert(message);
        deleteOldMessages(limit);
    }

    @Query("DELETE FROM chat_messages")
    void deleteAll();
} 
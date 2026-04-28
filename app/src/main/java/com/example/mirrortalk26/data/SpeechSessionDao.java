package com.example.mirrortalk26.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SpeechSessionDao {
@Insert
    long insert(SpeechSession session);
@Query("SELECT * FROM speech_sessions ORDER BY timestamp DESC")
    LiveData<List<SpeechSession>> getAllSessions();
@Query("SELECT * FROM speech_sessions WHERE id=:id")
    SpeechSession getSessionById(long id);
@Delete
    void delete(SpeechSession session);
@Query("SELECT COUNT(*) FROM speech_sessions")
    int getSessionCount();
}

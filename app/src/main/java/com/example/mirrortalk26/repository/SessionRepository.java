package com.example.mirrortalk26.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.mirrortalk26.data.AppDatabase;
import com.example.mirrortalk26.data.SpeechSession;
import com.example.mirrortalk26.data.SpeechSessionDao;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SessionRepository {
    private final SpeechSessionDao dao;
    private final ExecutorService executor;
    public final LiveData<List<SpeechSession>> allSessions;

    public SessionRepository(Application application){
        AppDatabase db = AppDatabase.getInstance(application);
        dao = db.sessionDao();
        allSessions = dao.getAllSessions();
        executor = Executors.newSingleThreadExecutor();
    }
    public void insert(SpeechSession session){
        executor.execute(() -> dao.insert(session));
    }
    public void delete(SpeechSession session){
        executor.execute(() -> dao.delete(session));
    }
    public LiveData<List<SpeechSession>> getAllSessions(){
        return allSessions;
    }
}

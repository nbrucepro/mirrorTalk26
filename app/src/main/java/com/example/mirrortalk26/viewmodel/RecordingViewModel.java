package com.example.mirrortalk26.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mirrortalk26.analysis.SpeechAnalyzer;
import com.example.mirrortalk26.data.SpeechSession;
import com.example.mirrortalk26.repository.SessionRepository;

public class RecordingViewModel extends AndroidViewModel {
    // ── Live data the UI observes ──────────────────────────
    private final MutableLiveData<Float> _wpm           = new MutableLiveData<>(0f);
    private final MutableLiveData<Integer> _fillerCount = new MutableLiveData<>(0);
    private final MutableLiveData<Float> _eyeContact    = new MutableLiveData<>(0f);
    private final MutableLiveData<String> _partialText =  new MutableLiveData<>("");

    public LiveData<Float>   wpm         =  _wpm;
    public  LiveData<Integer> fillerCount =  _fillerCount;
    public  LiveData<Float>   eyeContact  =  _eyeContact;
    public  LiveData<String>  partialText =  _partialText;

    // ── Internal ───────────────────────────────────────────
    private final SpeechAnalyzer    analyzer = new SpeechAnalyzer();
    private final SessionRepository repository;

    private int totalFrames = 0;
    private int contactFrames = 0;
    private long sessionId = -1;

    public RecordingViewModel(@NonNull Application application){
        super(application);
        repository = new SessionRepository(application);
    }
    // Called when recording starts
    public void startSession(){
        analyzer.start();
        totalFrames = 0;
        contactFrames = 0;
        _wpm.setValue(0f);
        _fillerCount.setValue(0);
        _eyeContact.setValue(0f);
    }
    // Called every time SpeechRecognizer returns partial text
    public void onPartialTranscript(String text){
        analyzer.processText(text);
        _partialText.postValue(text);
        _wpm.postValue(analyzer.getCurrentWpm());
        _fillerCount.postValue(analyzer.getFillerWordCount());
    }
    // Called every time ML Kit analyses a frame
    public void updateEyeContact(boolean isContact){
        totalFrames ++;
        if (isContact) contactFrames++;
        float pct = totalFrames > 0 ? (contactFrames * 100f / totalFrames) : 0f;
        _eyeContact.postValue(pct);
    }

    // Called when recording stops — saves session to Room
    public long saveSession(int durationSeconds,String version){
        SpeechSession session = new SpeechSession(
                System.currentTimeMillis(),
                durationSeconds,
                analyzer.getFillerWordCount(),
                analyzer.getCurrentWpm(),
                totalFrames > 0 ? (contactFrames * 100f / totalFrames) : 0f,
                analyzer.getTranscript(),
                version
        );
        repository.insert(session);
        return session.id;
    }

}

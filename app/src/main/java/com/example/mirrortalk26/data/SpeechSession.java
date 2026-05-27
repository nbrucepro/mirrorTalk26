package com.example.mirrortalk26.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "speech_sessions")
public class SpeechSession {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long  timestamp;
    public int   durationSeconds;
    public int   fillerWordCount;
    public float averageWpm;
    public float eyeContactPercent;

    // Removed @NonNull to match V1 database schema
    public String transcript  = "";
    public String version     = "A";
    public String videoPath   = "";

    public SpeechSession(long timestamp, int durationSeconds,
                         int fillerWordCount, float averageWpm,
                         float eyeContactPercent,
                         String transcript,
                         String version,
                         String videoPath) {
        this.timestamp          = timestamp;
        this.durationSeconds    = durationSeconds;
        this.fillerWordCount    = fillerWordCount;
        this.averageWpm         = averageWpm;
        this.eyeContactPercent  = eyeContactPercent;
        this.transcript         = transcript;
        this.version            = version;
        this.videoPath          = videoPath;
    }
}
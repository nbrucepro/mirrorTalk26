package com.example.mirrortalk26.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "speech_sessions")
public class SpeechSession {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long timestamp;        // when the session happened
    public int durationSeconds;   // how long they spoke
    public int fillerWordCount;   // how many "um", "uh", "like"
    public float averageWpm;      // words per minute
    public float eyeContactPercent; // % of time looking at camera
    public String transcript;     // full speech text
    public String version;        // "A" (live overlay) or "B" (no overlay)
    public String videoPath;
    public SpeechSession(long timestamp, int durationSeconds,
                         int fillerWordCount, float averageWpm,
                         float eyeContactPercent, String transcript,
                         String version,String videoPath){
        this.timestamp=timestamp;
        this.durationSeconds = durationSeconds;
        this.fillerWordCount = fillerWordCount;
        this.averageWpm = averageWpm;
        this.eyeContactPercent = eyeContactPercent;
        this.transcript = transcript;
        this.version = version;
        this.videoPath          = videoPath;
    }
}

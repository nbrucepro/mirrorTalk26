package com.example.mirrortalk26.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {SpeechSession.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract SpeechSessionDao sessionDao();
    private static volatile AppDatabase INSTANCE;
    // Migration from version 1 → 2: add videoPath column
    static final Migration MIGRATION_1_2 = new Migration(1,2){
        @Override
        public void migrate(SupportSQLiteDatabase database){
            database.execSQL(
                    "ALTER TABLE speech_sessions ADD COLUMN videoPath TEXT DEFAULT ''"
            );
        }
    };
    public static AppDatabase getInstance(Context context){
        if (INSTANCE == null){
            synchronized (AppDatabase.class){
                if(INSTANCE == null){
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "mirrortalk_db"
                    )
                            .addMigrations(MIGRATION_1_2)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}

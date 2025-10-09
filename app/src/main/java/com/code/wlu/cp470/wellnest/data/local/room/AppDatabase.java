package com.code.wlu.cp470.wellnest.data.local.room;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

// DAOs
import com.code.wlu.cp470.wellnest.data.local.room.dao.ActivityJarDao;
import com.code.wlu.cp470.wellnest.data.local.room.dao.RoamioDao;
import com.code.wlu.cp470.wellnest.data.local.room.dao.SnapTaskDao;
import com.code.wlu.cp470.wellnest.data.local.room.dao.UserLocalDao;

// UserLocal entities
import com.code.wlu.cp470.wellnest.data.local.room.entities.BadgeLocal;
import com.code.wlu.cp470.wellnest.data.local.room.entities.FriendLocal;

// ActivityJar entities
import com.code.wlu.cp470.wellnest.data.local.room.entities.ActivityJar;
import com.code.wlu.cp470.wellnest.data.local.room.entities.ActivityCategory;
import com.code.wlu.cp470.wellnest.data.local.room.entities.ActivityTag;
import com.code.wlu.cp470.wellnest.data.local.room.entities.ActivityTagXref;
import com.code.wlu.cp470.wellnest.data.local.room.entities.Completion;
import com.code.wlu.cp470.wellnest.data.local.room.entities.CompletionTag;

// SnapTask entities
import com.code.wlu.cp470.wellnest.data.local.room.entities.TaskLocal;
import com.code.wlu.cp470.wellnest.data.local.room.entities.PendingVerify;

// Roamio entities
import com.code.wlu.cp470.wellnest.data.local.room.entities.WalkSessionLocal;
import com.code.wlu.cp470.wellnest.data.local.room.entities.CurrentWalkLocal;

@Database(
        entities = {
                // UserLocal
                FriendLocal.class,
                BadgeLocal.class,

                // ActivityJar
                ActivityJar.class,
                ActivityCategory.class,
                ActivityTag.class,
                ActivityTagXref.class,
                Completion.class,
                CompletionTag.class,

                // SnapTask
                TaskLocal.class,
                PendingVerify.class,

                // Roamio
                WalkSessionLocal.class,
                CurrentWalkLocal.class
        },
        version = 2,               // bump when schema changes
        exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {

    // DAOs
    public abstract UserLocalDao userLocalDao();
    public abstract ActivityJarDao activityJarDao();
    public abstract SnapTaskDao taskDao();
    public abstract RoamioDao roamioDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase get(Context ctx) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    ctx.getApplicationContext(),
                                    AppDatabase.class,
                                    "wellnest.db"
                                     )
                    // During active development enable this to skip writing migrations:
                    // .fallbackToDestructiveMigration()
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}

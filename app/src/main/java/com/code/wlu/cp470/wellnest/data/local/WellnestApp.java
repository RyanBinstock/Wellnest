package com.code.wlu.cp470.wellnest;

import android.app.Application;

import androidx.datastore.rxjava3.RxDataStore;
import androidx.datastore.rxjava3.RxDataStoreBuilder;

import com.code.wlu.cp470.wellnest.data.local.datastore.GlobalScoreSerializer;
import com.code.wlu.cp470.wellnest.data.local.datastore.StreakSerializer;
import com.code.wlu.cp470.wellnest.data.local.datastore.UserProfileSerializer;
import com.code.wlu.cp470.wellnest.proto.GlobalScore;
import com.code.wlu.cp470.wellnest.proto.Streak;
import com.code.wlu.cp470.wellnest.proto.UserProfile;

public class WellnestApp extends Application {

    private RxDataStore<UserProfile> userProfileStore;
    private RxDataStore<GlobalScore> globalScoreStore;
    private RxDataStore<Streak> streakStore;
    private static WellnestApp INSTANCE;

    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;

        // File names live under getFilesDir() automatically
        userProfileStore = new RxDataStoreBuilder<>(
                this,
                "user_profile.pb",
                new UserProfileSerializer()
        ).build();

        globalScoreStore = new RxDataStoreBuilder<>(
                this,
                "global_score.pb",
                new GlobalScoreSerializer()
        ).build();

        streakStore = new RxDataStoreBuilder<>(
                this,
                "streak.pb",
                new StreakSerializer()
        ).build();
    }

    public static WellnestApp get() { return INSTANCE; }

    public RxDataStore<UserProfile> userProfileStore() { return userProfileStore; }
    public RxDataStore<GlobalScore> globalScoreStore() { return globalScoreStore; }
    public RxDataStore<Streak> streakStore() { return streakStore; }
}

// Typical read/write
//// WRITE: set display name
//app.userProfileStore().updateDataAsync(current ->
//        io.reactivex.rxjava3.core.Single.fromCallable(() ->
//        current.toBuilder().setDisplayName("Cameron").setUpdatedAtMs(System.currentTimeMillis()).build()
//    )
//            );
//
//// READ: observe profile
//            app.userProfileStore().data()
//   .map(UserProfile::getDisplayName)
//// .observeOn(AndroidSchedulers.mainThread()) // if using RxAndroid
//   .subscribe(name -> { /* update UI */ });


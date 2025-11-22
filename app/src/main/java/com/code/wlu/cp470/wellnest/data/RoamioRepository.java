//package com.code.wlu.cp470.wellnest.data;
//
//import android.content.Context;
//import android.content.SharedPreferences;
//
//public class RoamioRepository {
//
//    private static final String PREFS = "roamio_repo_prefs";
//    private final SharedPreferences prefs;
//    private final Context context;
//    private final FirebaseRoamioManager remote;
//    private final RoamioManager local;

//    public RoamioRepository(Context context, RoamioManager localManager, FirebaseRoamioManager remoteManager) {
//        if (context == null) throw new IllegalArgumentException("context == null");
//        if (localManager == null) throw new IllegalArgumentException("localManager == null");
//        if (remoteManager == null) throw new IllegalArgumentException("remoteManager == null");
//        this.context = context;
//        this.local = localManager;
//        this.remote = remoteManager;
//        this.prefs = context.getApplicationContext()
//                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
//    }
//
//    // ------------------------------------------------------------
//    // Sync helpers
//    // ------------------------------------------------------------
//
//    // TODO: add checker for if we've completed today's walk
//    // ------------------------------------------------------------
//    // Method delegation
//    // ------------------------------------------------------------
//
//
//}

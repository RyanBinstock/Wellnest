//package com.code.wlu.cp470.wellnest.viewmodel;
//
//import android.app.Application;
//import android.database.sqlite.SQLiteDatabase;
//
//import androidx.annotation.NonNull;
//import androidx.lifecycle.AndroidViewModel;
//
//import com.code.wlu.cp470.wellnest.data.RoamioRepository;
//import com.code.wlu.cp470.wellnest.data.local.WellnestDatabaseHelper;
//
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//public class RoamioViewModel extends AndroidViewModel {
//
//    private final RoamioRepository repo;
//    private final WellnestDatabaseHelper dbHelper;
//    private final SQLiteDatabase db;
//
//    // Background executor so we don't hit DB on the main thread
//    private final ExecutorService io = Executors.newSingleThreadExecutor();
//
//    public RoamioViewModel(@NonNull Application application) {
//        super(application);
//        dbHelper = new WellnestDatabaseHelper(application);
//        db = dbHelper.getWritableDatabase();
//
//        RoamioManager local = new RoamioManager(db);
//        FirebaseRoamioManager remote = new FirebaseRoamioManager();
//        repo = new RoamioRepository(application, local, remote);
//    }
//}

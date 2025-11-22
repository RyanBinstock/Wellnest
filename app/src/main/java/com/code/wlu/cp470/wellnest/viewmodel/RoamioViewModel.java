package com.code.wlu.cp470.wellnest.viewmodel;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.code.wlu.cp470.wellnest.data.RoamioRepository;
import com.code.wlu.cp470.wellnest.data.local.WellnestDatabaseHelper;
import com.code.wlu.cp470.wellnest.data.local.managers.RoamioManager;
import com.code.wlu.cp470.wellnest.data.remote.managers.FirebaseRoamioManager;

public class RoamioViewModel extends AndroidViewModel {

    private final RoamioRepository repo;
    private final WellnestDatabaseHelper dbHelper;
    private final SQLiteDatabase db;

    public RoamioViewModel(@NonNull Application application) {
        super(application);
        dbHelper = new WellnestDatabaseHelper(application);
        db = dbHelper.getWritableDatabase();

        RoamioManager local = new RoamioManager(db);
        FirebaseRoamioManager remote = new FirebaseRoamioManager();
        repo = new RoamioRepository(application, local, remote);
    }
}

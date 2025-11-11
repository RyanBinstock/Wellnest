package com.code.wlu.cp470.wellnest;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;

import com.code.wlu.cp470.wellnest.data.local.WellnestDatabaseHelper;
import com.code.wlu.cp470.wellnest.data.local.managers.SnapTaskManager;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SnapTaskManagerInstrumentedTest {

    private Context context;
    private WellnestDatabaseHelper helper;
    private SQLiteDatabase db;
    private SnapTaskManager snapTaskManager;

    // ------------------------------------------------------------
    // lifecycle
    // ------------------------------------------------------------

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        helper = new WellnestDatabaseHelper(context);
        db = helper.getWritableDatabase();
        helper.cleanDatabase(db);

        // UserManager constructor should also ensure singleton rows exist
        snapTaskManager = new SnapTaskManager(db);
    }

    @After
    public void tearDown() {
        helper.cleanDatabase(db);
        if (db != null && db.isOpen()) db.close();
        if (helper != null) helper.close();
    }

    // ------------------------------------------------------------
    // user_profile
    // ------------------------------------------------------------

}

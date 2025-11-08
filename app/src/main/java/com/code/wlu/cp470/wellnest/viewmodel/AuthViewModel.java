package com.code.wlu.cp470.wellnest.viewmodel;

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.code.wlu.cp470.wellnest.data.UserInterface;
import com.code.wlu.cp470.wellnest.data.UserRepository;
import com.code.wlu.cp470.wellnest.data.auth.AuthRepository;
import com.code.wlu.cp470.wellnest.data.local.WellnestDatabaseHelper;
import com.code.wlu.cp470.wellnest.data.local.managers.UserManager;
import com.code.wlu.cp470.wellnest.data.remote.managers.FirebaseUserManager;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.Executors;

public class AuthViewModel extends AndroidViewModel {
    private final AuthRepository repo;
    private final WellnestDatabaseHelper dbHelper;
    private final SQLiteDatabase db;
    private final MutableLiveData<FirebaseUser> user = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> loginResult = new MutableLiveData<>();

    public AuthViewModel(Application app) {
        super(app);
        Context context = app.getApplicationContext();

        this.dbHelper = new WellnestDatabaseHelper(context);
        this.db = dbHelper.getWritableDatabase();

        UserInterface local = new UserManager(db);
        UserInterface remote = new FirebaseUserManager();

        UserRepository userRepo = new UserRepository(context, local, remote);

        repo = new AuthRepository(context, userRepo);
    }

    public LiveData<Boolean> getLoginResult() {
        return loginResult;
    }

    public LiveData<FirebaseUser> user() {
        return user;
    }

    public LiveData<String> error() {
        return error;
    }

    public LiveData<Boolean> loading() {
        return loading;
    }

    public void signIn(String email, String password) {
        loading.setValue(true);
        Executors.newSingleThreadExecutor().execute(() -> {
            repo.signIn(email, password, (u, e) -> {
                loading.postValue(false);
                if (e != null) error.postValue(e.getMessage());
                else user.postValue(u);
            });
        });

    }

    public void signUp(String name, String email, String password) {
        loading.setValue(true);
        Executors.newSingleThreadExecutor().execute(() -> {
            repo.signUp(name, email, password, (u, e) -> {
                loading.postValue(false);
                if (e != null) error.postValue(e.getMessage());
                else user.postValue(u);
            });
        });
    }

    public void signOut() {
        loading.setValue(true);
        Executors.newSingleThreadExecutor().execute(() -> {
            repo.signOut();
            loading.postValue(false);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (db != null && db.isOpen()) db.close();
        if (dbHelper != null) dbHelper.close();
    }
}

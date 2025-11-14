package com.code.wlu.cp470.wellnest.viewmodel;

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

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
    Context context;
    UserManager local;
    FirebaseUserManager remote;

    public AuthViewModel(Application app) {
        super(app);
        context = app.getApplicationContext();

        this.dbHelper = new WellnestDatabaseHelper(context);
        this.db = dbHelper.getWritableDatabase();

        this.local = new UserManager(db);
        this.remote = new FirebaseUserManager();

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
        // Fix: Use postValue instead of setValue since this method can be called from background threads
        loading.postValue(true);
        Executors.newSingleThreadExecutor().execute(() -> {
            repo.signOut();
            loading.postValue(false);
        });
    }

    public boolean deleteAccount() {
        loading.setValue(true);
        UserRepository userRepo = new UserRepository(context, local, remote);
        try {
            Executors.newSingleThreadExecutor().execute(() -> {
                userRepo.deleteUserProfile();
                repo.deleteAccount();
                signOut();
                loading.postValue(false);
            });
        } catch (Exception e) {
            error.postValue(e.getMessage());
            loading.postValue(false);
            return false;
        }
        return true;
    }
    
    public void deleteAccountWithPassword(String password, DeleteAccountCallback callback) {
        loading.postValue(true);
        UserRepository userRepo = new UserRepository(context, local, remote);
        
        Executors.newSingleThreadExecutor().execute(() -> {
            // Use the proper deleteAccountWithPassword method from AuthRepository
            repo.deleteAccountWithPassword(password, (result, error) -> {
                if (error != null) {
                    loading.postValue(false);
                    callback.onComplete(false, error);
                } else {
                    // Successfully deleted from Firebase, now clean up local data
                    userRepo.deleteUserProfile();
                    loading.postValue(false);
                    callback.onComplete(true, null);
                }
            });
        });
    }
    
    public interface DeleteAccountCallback {
        void onComplete(boolean success, Exception error);
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        if (db != null && db.isOpen()) db.close();
        if (dbHelper != null) dbHelper.close();
    }
}

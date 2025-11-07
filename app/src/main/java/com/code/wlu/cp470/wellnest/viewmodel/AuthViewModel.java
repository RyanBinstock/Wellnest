package com.code.wlu.cp470.wellnest.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.code.wlu.cp470.wellnest.data.auth.AuthRepository;
import com.google.firebase.auth.FirebaseUser;

public class AuthViewModel extends AndroidViewModel {
    private final AuthRepository repo;
    private final MutableLiveData<FirebaseUser> user = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    public AuthViewModel(Application app) {
        super(app);
        repo = new AuthRepository(app.getApplicationContext());
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
        repo.signIn(email, password, (u, e) -> {
            loading.postValue(false);
            if (e != null) error.postValue(e.getMessage());
            else user.postValue(u);
        });
    }

    public void signUp(String name, String email, String password) {
        loading.setValue(true);
        repo.signUp(name, email, password, (u, e) -> {
            loading.postValue(false);
            if (e != null) error.postValue(e.getMessage());
            else user.postValue(u);
        });
    }
}

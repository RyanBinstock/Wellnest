package com.code.wlu.cp470.wellnest.viewmodel;

import androidx.lifecycle.*;
import com.code.wlu.cp470.wellnest.data.auth.AuthRepository;  // <-- fix package spacing if editor auto-wraps
import com.google.firebase.auth.FirebaseUser;

public class AuthViewModel extends ViewModel {
    private final AuthRepository repo = new AuthRepository();
    private final MutableLiveData<FirebaseUser> user = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    public LiveData<FirebaseUser> user() { return user; }
    public LiveData<String> error() { return error; }
    public LiveData<Boolean> loading() { return loading; }

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

package com.example.chatfirebase.ui.main;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.chatfirebase.data.repository.AuthRepository;
import com.example.chatfirebase.data.repository.FcmTokenRepository;
import com.example.chatfirebase.model.User;

/** Sesión del usuario actual, compartida por MainActivity y sus fragments. */
public class MainViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final MutableLiveData<User> currentUser;

    public MainViewModel() {
        this(new AuthRepository(), new FcmTokenRepository());
    }

    public MainViewModel(AuthRepository authRepository, FcmTokenRepository fcmTokenRepository) {
        this.authRepository = authRepository;
        this.fcmTokenRepository = fcmTokenRepository;
        this.currentUser = new MutableLiveData<>(authRepository.getCurrentUser());
    }

    /** Usuario con sesión iniciada; pasa a null al cerrar sesión. */
    public LiveData<User> getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser.getValue() != null;
    }

    public void logout() {
        fcmTokenRepository.clearToken();
        authRepository.logout();
        currentUser.setValue(null);
    }
}

package com.example.chatfirebase.ui.main;

import androidx.lifecycle.ViewModel;

import com.example.chatfirebase.data.repository.AuthRepository;

public class MainViewModel extends ViewModel {

    private final AuthRepository authRepository;

    public MainViewModel() {
        this(new AuthRepository());
    }

    public MainViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public void logout() {
        authRepository.logout();
    }
}

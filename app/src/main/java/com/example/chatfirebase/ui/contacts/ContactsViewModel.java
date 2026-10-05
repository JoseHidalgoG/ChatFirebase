package com.example.chatfirebase.ui.contacts;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.chatfirebase.data.repository.UserRepository;
import com.example.chatfirebase.model.User;

import java.util.List;

public class ContactsViewModel extends ViewModel {

    private final UserRepository repository;

    public ContactsViewModel() {
        repository = new UserRepository();
        repository.startListeningContacts();
    }

    public LiveData<List<User>> getContacts() {
        return repository.getContacts();
    }

    public LiveData<String> getError() {
        return repository.getError();
    }

    @Override
    protected void onCleared() {
        repository.stopListeningContacts();
        super.onCleared();
    }
}
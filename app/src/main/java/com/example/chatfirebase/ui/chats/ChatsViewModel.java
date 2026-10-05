package com.example.chatfirebase.ui.chats;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.chatfirebase.data.repository.ChatRepository;
import com.example.chatfirebase.model.Chat;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class ChatsViewModel extends ViewModel {

    private final MutableLiveData<List<Chat>> chats =
            new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<String> error =
            new MutableLiveData<>();

    private final ChatRepository repository;
    private final ListenerRegistration registration;

    public ChatsViewModel() {
        repository = new ChatRepository();
        registration = repository.listenToChats(chats, error);
    }

    public LiveData<List<Chat>> getChats() {
        return chats;
    }

    public LiveData<String> getError() {
        return error;
    }

    @Override
    protected void onCleared() {
        if (registration != null) {
            registration.remove();
        }
    }
}
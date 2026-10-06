package com.example.chatfirebase.ui.chats;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.chatfirebase.data.repository.ChatRepository;
import com.example.chatfirebase.model.Chat;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class ChatListViewModel extends ViewModel {

    private final ChatRepository repository;
    private final MutableLiveData<List<Chat>> chats =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> errorMessage =
            new MutableLiveData<>();
    private ListenerRegistration chatsListener;

    public ChatListViewModel() {
        repository = new ChatRepository();
    }

    public LiveData<List<Chat>> getChats() {
        return chats;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void startListening() {
        if (chatsListener != null) { return; }
        chatsListener = repository.listenToChats(chats, errorMessage);
    }

    public void clearErrorMessage() {
        errorMessage.setValue(null);
    }

    @Override
    protected void onCleared() {
        if (chatsListener != null) {
            chatsListener.remove();
            chatsListener = null;
        }

        super.onCleared();
    }
}
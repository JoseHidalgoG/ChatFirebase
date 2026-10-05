package com.example.chatfirebase.ui.chats;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.chatfirebase.data.repository.MessageRepository;
import com.example.chatfirebase.model.Message;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class ChatViewModel extends ViewModel {

    private final MessageRepository messageRepository;
    private final MutableLiveData<List<Message>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> sending = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> messageSent = new MutableLiveData<>(false);
    private ListenerRegistration messagesListener;
    private String currentChatId;

    public ChatViewModel() {
        messageRepository = new MessageRepository();
    }

    public LiveData<List<Message>> getMessages() {
        return messages;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getSending() {
        return sending;
    }

    public LiveData<Boolean> getMessageSent() {
        return messageSent;
    }

    public void loadMessages(String chatId) {
        if (chatId == null || chatId.trim().isEmpty()) {
            errorMessage.setValue("No se ha especificado una conversación válida.");
            return;
        }

        // Evita registrar el mismo listener más de una vez.
        if (chatId.equals(currentChatId) && messagesListener != null) {
            return;
        }

        removeMessagesListener();
        currentChatId = chatId;

        messagesListener = messageRepository.listenMessages(
                chatId,
                new MessageRepository.MessagesCallback() {
                    @Override
                    public void onSuccess(List<Message> result) {
                        messages.postValue(result);
                    }

                    @Override
                    public void onError(Exception error) {
                        errorMessage.postValue(
                                getReadableError(error)
                        );
                    }
                }
        );
    }

    public void sendTextMessage(String text) {
        if (currentChatId == null || currentChatId.trim().isEmpty()) {
            errorMessage.setValue("No se ha cargado una conversación.");
            return;
        }

        if (text == null || text.trim().isEmpty()) {
            errorMessage.setValue("Escribe un mensaje antes de enviarlo.");
            return;
        }

        if (Boolean.TRUE.equals(sending.getValue())) {
            return;
        }

        messageSent.setValue(false);
        sending.setValue(true);

        messageRepository.sendTextMessage(
                currentChatId,
                text,
                new MessageRepository.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        sending.postValue(false);
                        messageSent.postValue(true);
                    }

                    @Override
                    public void onError(Exception error) {
                        sending.postValue(false);
                        errorMessage.postValue(
                                getReadableError(error)
                        );
                    }
                }
        );
    }

    public void clearErrorMessage() {
        errorMessage.setValue(null);
    }

    private void removeMessagesListener() {
        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }
    }

    private String getReadableError(Exception error) {
        if (error == null || error.getMessage() == null) {
            return "Ocurrió un error inesperado.";
        }

        return error.getMessage();
    }

    @Override
    protected void onCleared() {
        removeMessagesListener();
        super.onCleared();
    }
}
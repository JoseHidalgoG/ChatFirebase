package com.example.chatfirebase.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.chatfirebase.model.Chat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ChatRepository {

    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;

    public ChatRepository() {
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public ListenerRegistration listenToChats(
            MutableLiveData<List<Chat>> chats,
            MutableLiveData<String> error) {

        if (auth.getCurrentUser() == null) {
            error.setValue("Debes iniciar sesión para ver tus chats.");
            chats.setValue(new ArrayList<>());
            return null;
        }

        String currentUid = auth.getCurrentUser().getUid();

        return firestore.collection("chats")
                .whereArrayContains("participants", currentUid)
                .orderBy("lastMessageAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, exception) -> {

                    if (exception != null) {
                        error.setValue(
                                "No se pudieron cargar los chats: "
                                        + exception.getLocalizedMessage()
                        );
                        return;
                    }

                    List<Chat> result = new ArrayList<>();

                    if (snapshot != null) {
                        for (var document : snapshot.getDocuments()) {
                            Chat chat = document.toObject(Chat.class);

                            if (chat != null) {
                                chat.setId(document.getId());
                                result.add(chat);
                            }
                        }
                    }

                    chats.setValue(result);
                });
    }
}

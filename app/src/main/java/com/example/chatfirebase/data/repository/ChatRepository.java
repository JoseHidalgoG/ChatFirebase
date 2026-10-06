package com.example.chatfirebase.data.repository;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.example.chatfirebase.model.Chat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatRepository {

    public interface Callback {
        void onSuccess(String chatId);
        void onError(Exception error);
    }

    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;

    public ChatRepository() {
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public void getOrCreatePrivateChat(String otherUserId, Callback callback) {
        if (auth.getCurrentUser() == null) {
            callback.onError(
                    new IllegalStateException("No hay una sesión iniciada.")
            );
            return;
        }

        String currentUserId = auth.getCurrentUser().getUid();

        if (otherUserId == null
                || otherUserId.trim().isEmpty()
                || currentUserId.equals(otherUserId)) {
            callback.onError(
                    new IllegalArgumentException("El contacto no es válido.")
            );
            return;
        }

        String firstId = currentUserId.compareTo(otherUserId) < 0
                ? currentUserId : otherUserId;

        String secondId = currentUserId.compareTo(otherUserId) < 0
                ? otherUserId : currentUserId;

        String chatId = "private_" + firstId + "_" + secondId;
        callback.onSuccess(chatId);
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
                        for (DocumentSnapshot document : snapshot.getDocuments()) {

                            Chat chat = new Chat();

                            chat.setId(document.getId());
                            chat.setParticipants((List<String>) document.get("participants"));
                            chat.setType(document.getString("type"));
                            chat.setName(document.getString("name"));
                            chat.setCreatedBy(document.getString("createdBy"));
                            chat.setLastMessage(document.getString("lastMessage"));
                            chat.setLastMessageSenderId(document.getString("lastMessageSenderId"));

                            com.google.firebase.Timestamp createdAt = document.getTimestamp("createdAt");

                            if (createdAt != null) {
                                chat.setCreatedAt(createdAt.toDate().getTime());
                            } else {
                                chat.setCreatedAt(0L);
                            }

                            com.google.firebase.Timestamp lastMessageAt = document.getTimestamp("lastMessageAt");

                            if (lastMessageAt != null) {
                                chat.setLastMessageAt(lastMessageAt.toDate().getTime());
                            } else {
                                chat.setLastMessageAt(0L);
                            }

                            result.add(chat);
                        }
                    }

                    loadChatProfiles(result, chats);
                });
    }

    private void loadChatProfiles(List<Chat> chats, MutableLiveData<List<Chat>> liveChats) {
        if (chats.isEmpty()) {
            liveChats.setValue(chats);
            return;
        }

        final int[] pending = {chats.size()};

        for (Chat chat : chats) {
            // Para un grupo, el nombre está en el propio documento.
            if ("group".equals(chat.getType())) {
                finishProfileLoad(chats, pending, liveChats);
                continue;
            }

            String currentUid = auth.getCurrentUser() != null
                    ? auth.getCurrentUser().getUid()
                    : null;

            String otherUserId = null;

            if (chat.getParticipants() != null) {
                for (String uid : chat.getParticipants()) {
                    if (uid != null && !uid.equals(currentUid)) {
                        otherUserId = uid;
                        break;
                    }
                }
            }

            if (otherUserId == null) {
                finishProfileLoad(chats, pending, liveChats);
                continue;
            }

            firestore.collection("users")
                    .document(otherUserId)
                    .get()
                    .addOnSuccessListener(userDocument -> {
                        // nombre y foto para la interfaz, no se escriben en el documento del chat
                        chat.setContactName(userDocument.getString("name"));
                        chat.setContactPhotoUrl(userDocument.getString("photoUrl"));
                        finishProfileLoad(chats, pending, liveChats);
                    })
                    .addOnFailureListener(exception -> {
                        Log.w(
                                "ChatRepository",
                                "No se pudo cargar el perfil del participante",
                                exception
                        );

                        finishProfileLoad(chats, pending, liveChats);
                    });
        }
    }

    private void finishProfileLoad(
            List<Chat> chats,
            int[] pending,
            MutableLiveData<List<Chat>> liveChats
    ) {
        pending[0]--;

        if (pending[0] == 0) {
            liveChats.setValue(chats);
        }
    }

}
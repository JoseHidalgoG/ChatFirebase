package com.example.chatfirebase.data.repository;

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

        // Ambos usuarios generan el mismo ID, sin importar quién inicia.
        List<String> participants = Arrays.asList(
                currentUserId,
                otherUserId
        );

        String firstId = currentUserId.compareTo(otherUserId) < 0
                ? currentUserId : otherUserId;

        String secondId = currentUserId.compareTo(otherUserId) < 0
                ? otherUserId : currentUserId;

        String chatId = "private_" + firstId + "_" + secondId;

        DocumentReference chatRef = firestore.collection("chats").document(chatId);

        firestore.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(chatRef);

                    if (!snapshot.exists()) {
                        Map<String, Object> chatData = new HashMap<>();
                        chatData.put("participants", participants);
                        chatData.put("type", "private");
                        chatData.put("name", null);
                        chatData.put("createdBy", currentUserId);
                        chatData.put("createdAt", FieldValue.serverTimestamp());
                        chatData.put("lastMessage", "");
                        chatData.put("lastMessageAt", FieldValue.serverTimestamp());
                        chatData.put("lastMessageSenderId", "");

                        transaction.set(chatRef, chatData);
                    } else {
                        // Verificar que el documento corresponda a esta pareja.
                        List<?> existingParticipants =
                                (List<?>) snapshot.get("participants");

                        if (existingParticipants == null
                                || !existingParticipants.contains(currentUserId)
                                || !existingParticipants.contains(otherUserId)
                                || !"private".equals(snapshot.getString("type"))) {
                            throw new IllegalStateException(
                                    "El chat existente no corresponde a esta conversación."
                            );
                        }
                    }

                    return chatId;
                }).addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(callback::onError);
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
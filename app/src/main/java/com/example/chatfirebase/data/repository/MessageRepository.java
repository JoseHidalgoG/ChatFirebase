package com.example.chatfirebase.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.chatfirebase.model.Message;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageRepository {
    public interface MessagesCallback {
        void onSuccess(List<Message> messages);
        void onError(Exception error);
    }

    public interface OperationCallback {
        void onSuccess();
        void onError(Exception error);
    }

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public MessageRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    /**
     * Escucha los mensajes de una conversación en tiempo real.
     *
     * Devuelve un ListenerRegistration para poder detener
     * la escucha cuando ya no sea necesaria.
     */
    public ListenerRegistration listenMessages(
            String chatId,
            MessagesCallback callback
    ) {
        if (chatId == null || chatId.trim().isEmpty()) {
            callback.onError(
                    new IllegalArgumentException("El chatId no es válido.")
            );
            return null;
        }

        return db.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        callback.onError(error);
                        return;
                    }

                    if (snapshots == null) {
                        callback.onSuccess(new ArrayList<>());
                        return;
                    }

                    List<Message> messages = new ArrayList<>();

                    for (DocumentSnapshot document : snapshots.getDocuments()) {
                        Message message = document.toObject(Message.class);

                        if (message != null) {
                            // El ID del documento no necesita guardarse
                            // como un campo adicional en Firestore.
                            message.setMessageId(document.getId());
                            messages.add(message);
                        }
                    }

                    callback.onSuccess(messages);
                });
    }

    /**
     * Envía un mensaje de texto a una conversación.
     */
    public void sendTextMessage(
            String chatId,
            String text,
            OperationCallback callback
    ) {
        if (auth.getCurrentUser() == null) {
            callback.onError(
                    new IllegalStateException(
                            "Debes iniciar sesión para enviar mensajes."
                    )
            );
            return;
        }

        if (chatId == null || chatId.trim().isEmpty()) {
            callback.onError(
                    new IllegalArgumentException("El chatId no es válido.")
            );
            return;
        }

        if (text == null || text.trim().isEmpty()) {
            callback.onError(
                    new IllegalArgumentException(
                            "El mensaje no puede estar vacío."
                    )
            );
            return;
        }

        String senderId = auth.getCurrentUser().getUid();
        String cleanText = text.trim();

        var chatRef = db.collection("chats").document(chatId);
        var messageRef = chatRef.collection("messages").document();

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("senderId", senderId);
        messageData.put("text", cleanText);
        messageData.put("type", "text");
        messageData.put("imageUrl", null);
        messageData.put("createdAt", FieldValue.serverTimestamp());

        // Actualiza el historial de mensajes y la información
        // de la conversación en una misma operación atómica.
        db.runTransaction(transaction -> {
                    DocumentSnapshot chatSnapshot = transaction.get(chatRef);

                    if (!chatSnapshot.exists()) {
                        throw new IllegalStateException(
                                "La conversación no existe."
                        );
                    }

                    Object participantsObject = chatSnapshot.get("participants");

                    if (!(participantsObject instanceof List)) {
                        throw new IllegalStateException(
                                "La conversación no tiene participantes válidos."
                        );
                    }

                    List<?> participants = (List<?>) participantsObject;

                    if (!participants.contains(senderId)) {
                        throw new SecurityException(
                                "No perteneces a esta conversación."
                        );
                    }

                    transaction.set(messageRef, messageData);

                    Map<String, Object> chatUpdates = new HashMap<>();
                    chatUpdates.put("lastMessage", cleanText);
                    chatUpdates.put("lastMessageAt", FieldValue.serverTimestamp());
                    chatUpdates.put("lastMessageSenderId", senderId);

                    transaction.update(chatRef, chatUpdates);

                    return null;
                }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }
}

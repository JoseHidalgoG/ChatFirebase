package com.example.chatfirebase.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.chatfirebase.model.Message;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessageRepository {
    public interface MessagesCallback {
        void onSuccess(List<Message> messages);
        void onError(Exception error);
    }

    public interface OperationCallback {
        void onSuccess();
        void onError(Exception error);
    }

    private final Context context;

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    private final ExecutorService imageExecutor = Executors.newSingleThreadExecutor();
    // Poniendo limites para que no cargue tanto el firestore
    private static final int MAX_IMAGE_SIDE = 512;
    private static final int MAX_BASE64_CHARS = 700 * 1024;

    public MessageRepository(Context context) {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        this.context = context.getApplicationContext();
    }

    /**
     * Escucha los mensajes de una conversación en tiempo real.
     *
     * Devuelve un ListenerRegistration para poder detener
     * la escucha cuando ya no sea necesaria.
     */
    public ListenerRegistration listenMessages(String chatId, MessagesCallback callback) {
        if (chatId == null || chatId.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("El chatId no es válido."));
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
    public void sendTextMessage(String chatId, String otherUserId, String text, OperationCallback callback) {
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

        if (otherUserId == null || otherUserId.trim().isEmpty() || senderId.equals(otherUserId)) {
            callback.onError(new IllegalArgumentException("El contacto no es válido."));
            return;
        }

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

                    List<String> participants = new ArrayList<>();
                    participants.add(senderId);
                    participants.add(otherUserId);

                    if (chatSnapshot.exists()) {
                        Object participantsObject = chatSnapshot.get("participants");

                        if (!(participantsObject instanceof List)) {
                            throw new IllegalStateException(
                                    "La conversación no tiene participantes válidos."
                            );
                        }

                        List<?> existingParticipants = (List<?>) participantsObject;

                        if (!existingParticipants.contains(senderId)
                                || !existingParticipants.contains(otherUserId)
                                || !"private".equals(chatSnapshot.getString("type"))) {
                            throw new SecurityException(
                                    "La conversación no corresponde a estos participantes."
                            );
                        }
                    }

                    transaction.set(messageRef, messageData);

                    Map<String, Object> chatUpdates = new HashMap<>();
                    chatUpdates.put("participants", participants);
                    chatUpdates.put("type", "private");
                    chatUpdates.put("createdBy", senderId);
                    chatUpdates.put("lastMessage", cleanText);
                    chatUpdates.put("lastMessageAt", FieldValue.serverTimestamp());
                    chatUpdates.put("lastMessageSenderId", senderId);

                    if (!chatSnapshot.exists()) {
                        chatUpdates.put("name", null);
                        chatUpdates.put("createdAt", FieldValue.serverTimestamp());

                        transaction.set(chatRef, chatUpdates);
                    } else {
                        transaction.update(chatRef, chatUpdates);
                    }

                    return null;
                }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    /***
     * ENVIAR IMAGENES
     * ***/

    public void sendImageMessage(String chatId, String otherUserId, Uri imageUri, OperationCallback callback) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            callback.onError(new IllegalStateException("Debes iniciar sesión."));
            return;
        }

        if (otherUserId == null || otherUserId.trim().isEmpty() || currentUser.getUid().equals(otherUserId)) {
            callback.onError(new IllegalArgumentException("El contacto no es válido."));
            return;
        }

        if (chatId == null || chatId.trim().isEmpty() || imageUri == null) {
            callback.onError(new IllegalArgumentException("El chat o la imagen no son válidos."));
            return;
        }

        final String senderId = currentUser.getUid();

        imageExecutor.execute(() -> {
            try {
                ContentResolver resolver = context.getContentResolver();

                Bitmap bitmap;
                try (InputStream input =
                             resolver.openInputStream(imageUri)) {

                    if (input == null) {
                        throw new IOException("No se pudo abrir la imagen seleccionada.");
                    }

                    BitmapFactory.Options options = new BitmapFactory.Options();

                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeStream(input, null, options);

                    int width = options.outWidth;
                    int height = options.outHeight;

                    if (width <= 0 || height <= 0) {
                        throw new IOException("El archivo seleccionado no es una imagen válida.");
                    }

                    int sampleSize = 1;

                    while (width / sampleSize > MAX_IMAGE_SIDE * 2 || height / sampleSize > MAX_IMAGE_SIDE * 2) {
                        sampleSize *= 2;
                    }

                    options.inJustDecodeBounds = false;
                    options.inSampleSize = sampleSize;
                }

                try (InputStream input = resolver.openInputStream(imageUri)) {
                    if (input == null) {
                        throw new IOException("No se pudo leer la imagen.");
                    }

                    BitmapFactory.Options options = new BitmapFactory.Options();

                    options.inSampleSize = calculateSampleSize(resolver, imageUri, MAX_IMAGE_SIDE);

                    bitmap = BitmapFactory.decodeStream(input, null, options);
                }

                if (bitmap == null) {
                    throw new IOException("No se pudo procesar la imagen.");
                }

                Bitmap resized = resizeBitmap(bitmap, MAX_IMAGE_SIDE);

                if (resized != bitmap) { bitmap.recycle(); }

                ByteArrayOutputStream output = new ByteArrayOutputStream();

                boolean compressed = resized.compress(
                        Bitmap.CompressFormat.JPEG,
                        55,
                        output
                );

                resized.recycle();

                if (!compressed) {
                    throw new IOException("No se pudo comprimir la imagen.");
                }

                String imageBase64 = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP);

                if (imageBase64.length() > MAX_BASE64_CHARS) {
                    throw new IOException(
                            "La imagen sigue siendo demasiado grande. "
                                    + "Selecciona una imagen más pequeña."
                    );
                }

                saveImageMessage(chatId, senderId, otherUserId, imageBase64, callback);

            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    private int calculateSampleSize(
            ContentResolver resolver,
            Uri uri,
            int maxSide
    ) throws IOException {

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;

        try (InputStream input = resolver.openInputStream(uri)) {
            if (input == null) {
                throw new IOException("No se pudo abrir la imagen.");
            }

            BitmapFactory.decodeStream(input, null, bounds);
        }

        int sample = 1;

        while (bounds.outWidth / sample > maxSide * 2
                || bounds.outHeight / sample > maxSide * 2) {
            sample *= 2;
        }

        return sample;
    }

    private Bitmap resizeBitmap(Bitmap source, int maxSide) {
        int width = source.getWidth();
        int height = source.getHeight();

        float scale = Math.min(
                1f,
                (float) maxSide / Math.max(width, height)
        );

        int newWidth = Math.max(1, Math.round(width * scale));
        int newHeight = Math.max(1, Math.round(height * scale));

        if (newWidth == width && newHeight == height) {
            return source;
        }

        return Bitmap.createScaledBitmap(
                source, newWidth, newHeight, true
        );
    }


    private void saveImageMessage(String chatId, String senderId, String otherUserId, String imageBase64, OperationCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference chatRef = db.collection("chats").document(chatId);

        DocumentReference messageRef = chatRef.collection("messages").document();

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("senderId", senderId);
        messageData.put("text", "");
        messageData.put("type", "image");
        messageData.put("imageUrl", null);
        messageData.put("imageBase64", imageBase64);
        messageData.put("createdAt", FieldValue.serverTimestamp());

        db.runTransaction(transaction -> {
                    DocumentSnapshot chatSnapshot = transaction.get(chatRef);

                    List<String> participants = new ArrayList<>();
                    participants.add(senderId);
                    participants.add(otherUserId);

                    if (chatSnapshot.exists()) {
                        Object participantsObject = chatSnapshot.get("participants");

                        if (!(participantsObject instanceof List)) {
                            throw new IllegalStateException(
                                    "La conversación no tiene participantes válidos."
                            );
                        }

                        List<?> existingParticipants = (List<?>) participantsObject;

                        if (!existingParticipants.contains(senderId)
                                || !existingParticipants.contains(otherUserId)
                                || !"private".equals(chatSnapshot.getString("type"))) {
                            throw new SecurityException(
                                    "La conversación no corresponde a estos participantes."
                            );
                        }
                    }

                    transaction.set(messageRef, messageData);

                    Map<String, Object> chatUpdates = new HashMap<>();
                    chatUpdates.put("participants", participants);
                    chatUpdates.put("type", "private");
                    chatUpdates.put("createdBy", senderId);
                    chatUpdates.put("lastMessage", "📷 Imagen");
                    chatUpdates.put("lastMessageAt", FieldValue.serverTimestamp());
                    chatUpdates.put("lastMessageSenderId", senderId);

                    if (!chatSnapshot.exists()) {
                        chatUpdates.put("name", null);
                        chatUpdates.put("createdAt", FieldValue.serverTimestamp());

                        transaction.set(chatRef, chatUpdates);
                    } else {
                        transaction.update(chatRef, chatUpdates);
                    }

                    return null;
                }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }
}
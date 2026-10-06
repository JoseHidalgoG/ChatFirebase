
package com.example.chatfirebase.data.repository;

import androidx.annotation.NonNull;

import com.example.chatfirebase.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

public class ProfileRepository {

    public interface Callback {
        void onSuccess();
        void onError(Exception error);
    }

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public ProfileRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public void updateProfile(String name, String photoBase64, Callback callback) {
        if (auth.getCurrentUser() == null) {
            callback.onError(
                    new IllegalStateException("Debes iniciar sesión.")
            );
            return;
        }

        if (name == null || name.trim().isEmpty()) {
            callback.onError(
                    new IllegalArgumentException("El nombre no puede estar vacío.")
            );
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name.trim());

        if (photoBase64 != null) {
            updates.put("photoBase64", photoBase64);
        }

        db.collection("users")
                .document(uid)
                .update(updates)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public void updatePresence(boolean online) {
        if (auth.getCurrentUser() == null) {
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        Map<String, Object> updates = new HashMap<>();
        updates.put("online", online);

        if (!online) {
            updates.put("lastSeen", System.currentTimeMillis());
        }

        db.collection("users")
                .document(uid)
                .update(updates);
    }

    public ListenerRegistration listenToUser(String uid,
            com.google.firebase.firestore.EventListener<com.google.firebase.firestore.DocumentSnapshot> listener
    ) {
        return db.collection("users")
                .document(uid)
                .addSnapshotListener(listener);
    }
}
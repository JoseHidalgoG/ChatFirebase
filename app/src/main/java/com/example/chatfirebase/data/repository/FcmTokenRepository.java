package com.example.chatfirebase.data.repository;

import android.util.Log;

import com.example.chatfirebase.util.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;


public class FcmTokenRepository {

    private static final String TAG = "FcmTokenRepository";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public FcmTokenRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }


    public void registerCurrentToken() {
        if (auth.getCurrentUser() == null) {
            return;
        }

        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(this::saveToken)
                .addOnFailureListener(e -> Log.w(TAG, "No se pudo obtener el token FCM", e));
    }

    public void saveToken(String token) {
        if (auth.getCurrentUser() == null || token == null || token.isEmpty()) {
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        Map<String, Object> updates = new HashMap<>();
        updates.put(Constants.FIELD_FCM_TOKEN, token);

        // merge y no update: al iniciar sesión el documento puede no existir todavía.
        db.collection(Constants.COLLECTION_USERS)
                .document(uid)
                .set(updates, SetOptions.merge())
                .addOnFailureListener(e -> Log.w(TAG, "No se pudo guardar el token FCM", e));
    }


    public void clearToken() {
        if (auth.getCurrentUser() != null) {
            db.collection(Constants.COLLECTION_USERS)
                    .document(auth.getCurrentUser().getUid())
                    .update(Constants.FIELD_FCM_TOKEN, FieldValue.delete())
                    .addOnFailureListener(e -> Log.w(TAG, "No se pudo borrar el token FCM", e));
        }

        FirebaseMessaging.getInstance().deleteToken()
                .addOnFailureListener(e -> Log.w(TAG, "No se pudo invalidar el token FCM", e));
    }
}

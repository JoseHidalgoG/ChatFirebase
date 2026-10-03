package com.example.chatfirebase.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.example.chatfirebase.R;
import com.example.chatfirebase.model.User;
import com.example.chatfirebase.util.Constants;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Repositorio de autenticación en Firebase(users/{uid}).
 */
public class AuthRepository {

    public interface Callback<T> {
        void onSuccess(T result);

        void onError(@StringRes int messageRes);
    }

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    public AuthRepository() {
        this(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance());
    }

    public AuthRepository(FirebaseAuth auth, FirebaseFirestore firestore) {
        this.auth = auth;
        this.firestore = firestore;
    }

    /** crea la cuenta, establece el nombre y escribe en users/{uid}. */
    public void register(String name, String email, String password, Callback<User> callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) {
                        callback.onError(R.string.error_auth_generic);
                        return;
                    }
                    UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build();
                    // El documento de Firestore es la fuente de verdad del nombre, así que
                    // si falla la actualización del perfil no es grave.
                    firebaseUser.updateProfile(profile);

                    User user = new User(firebaseUser.getUid(), name, email);
                    userDoc(user.getUid()).set(user)
                            .addOnSuccessListener(unused -> callback.onSuccess(user))
                            .addOnFailureListener(e -> {
                                // No se deja con sesión a un usuario registrado a medias; el
                                // documento se vuelve a crear en su próximo inicio de sesión.
                                auth.signOut();
                                callback.onError(R.string.error_profile_save);
                            });
                })
                .addOnFailureListener(e -> callback.onError(mapError(e)));
    }

    /** Inicia sesión con email y retorna el perfil del usuario */
    public void login(String email, String password, Callback<User> callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) {
                        callback.onError(R.string.error_auth_generic);
                        return;
                    }
                    loadOrCreateProfile(firebaseUser, callback);
                })
                .addOnFailureListener(e -> callback.onError(mapError(e)));
    }

    /** Retorna el usuario loggeado (persistido por FirebaseAuth a través de restarts), o nulo. */
    @Nullable
    public User getCurrentUser() {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        return firebaseUser != null ? fromFirebaseUser(firebaseUser) : null;
    }

    public void logout() {
        auth.signOut();
    }

    private void loadOrCreateProfile(FirebaseUser firebaseUser, Callback<User> callback) {
        DocumentReference doc = userDoc(firebaseUser.getUid());
        doc.get()
                .addOnSuccessListener(snapshot -> {
                    User user = snapshot.exists() ? snapshot.toObject(User.class) : null;
                    if (user != null) {
                        callback.onSuccess(user);
                        return;
                    }
                    User created = fromFirebaseUser(firebaseUser);
                    doc.set(created);
                    callback.onSuccess(created);
                })
                // El inicio de sesión sí funcionó; se usan los datos que tiene FirebaseAuth.
                .addOnFailureListener(e -> callback.onSuccess(fromFirebaseUser(firebaseUser)));
    }

    private DocumentReference userDoc(String uid) {
        return firestore.collection(Constants.COLLECTION_USERS).document(uid);
    }

    @NonNull
    private static User fromFirebaseUser(@NonNull FirebaseUser firebaseUser) {
        String email = firebaseUser.getEmail();
        String name = firebaseUser.getDisplayName();
        if ((name == null || name.isEmpty()) && email != null) {
            name = email.substring(0, email.indexOf('@') > 0 ? email.indexOf('@') : email.length());
        }
        User user = new User(firebaseUser.getUid(), name, email);
        if (firebaseUser.getPhotoUrl() != null) {
            user.setPhotoUrl(firebaseUser.getPhotoUrl().toString());
        }
        return user;
    }

    @StringRes
    private static int mapError(Exception e) {
        if (e instanceof FirebaseAuthUserCollisionException) {
            return R.string.error_email_in_use;
        } else if (e instanceof FirebaseAuthWeakPasswordException) {
            return R.string.error_password_weak;
        } else if (e instanceof FirebaseAuthInvalidUserException
                || e instanceof FirebaseAuthInvalidCredentialsException) {
            // Mismo mensaje a propósito: no revela si el correo está registrado.
            return R.string.error_invalid_credentials;
        } else if (e instanceof FirebaseNetworkException) {
            return R.string.error_network;
        } else if (e instanceof FirebaseTooManyRequestsException) {
            return R.string.error_too_many_requests;
        }
        return R.string.error_auth_generic;
    }
}

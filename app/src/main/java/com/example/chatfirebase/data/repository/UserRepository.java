
package com.example.chatfirebase.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.chatfirebase.model.User;
import com.example.chatfirebase.util.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class UserRepository {

    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;

    private final MutableLiveData<List<User>> contacts = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private ListenerRegistration contactsListener;

    public UserRepository() {
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public LiveData<List<User>> getContacts() { return contacts; }

    public LiveData<String> getError() { return error; }

    public void startListeningContacts() {
        if (contactsListener != null) { return; }

        if (auth.getCurrentUser() == null) {
            error.setValue("Debes iniciar sesión para ver los contactos.");
            return;
        }

        String currentUid = auth.getCurrentUser().getUid();

        contactsListener = firestore
                .collection(Constants.COLLECTION_USERS)
                .addSnapshotListener((snapshot, exception) -> {

                    if (exception != null) {
                        error.setValue(
                                "No se pudieron cargar los contactos: "
                                        + exception.getLocalizedMessage()
                        );
                        return;
                    }

                    if (snapshot == null) {
                        return;
                    }

                    List<User> result = new ArrayList<>();

                    for (QueryDocumentSnapshot document : snapshot) {
                        User user = document.toObject(User.class);
                        String documentUid = document.getId();

                        if (currentUid.equals(documentUid)) {continue;}
                        user.setUid(documentUid);
                        if (user.getPhotoUrl() == null) {
                            user.setPhotoUrl(
                                    document.getString("photoUrl")
                            );
                        }

                        result.add(user);
                    }

                    contacts.setValue(result);
                    error.setValue(null);
                });
    }

    public void stopListeningContacts() {
        if (contactsListener != null) {
            contactsListener.remove();
            contactsListener = null;
        }
    }
}
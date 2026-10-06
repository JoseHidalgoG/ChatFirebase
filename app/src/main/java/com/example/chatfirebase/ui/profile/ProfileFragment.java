package com.example.chatfirebase.ui.profile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.chatfirebase.data.repository.ProfileRepository;
import com.example.chatfirebase.databinding.FragmentProfileBinding;
import com.example.chatfirebase.utils.UserPhotoUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.Date;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private final ProfileRepository repository = new ProfileRepository();

    private ListenerRegistration userListener;
    private String selectedPhotoBase64;
    private boolean loadingProfile = true;
    private boolean ownProfile;
    private String profileUid;

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    this::onImageSelected
            );

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentProfileBinding.inflate(
                inflater, container, false
        );
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseUser currentUser =
                FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            showMessage("Debes iniciar sesión.");
            return;
        }

        String requestedUid = getArguments() != null
                ? getArguments().getString("userId", "")
                : "";

        ownProfile = requestedUid == null
                || requestedUid.trim().isEmpty()
                || requestedUid.equals(currentUser.getUid());

        profileUid = ownProfile
                ? currentUser.getUid()
                : requestedUid;

        // Solo el propietario puede editar su perfil.
        binding.nameInput.setEnabled(ownProfile);
        binding.nameInput.setFocusable(ownProfile);
        binding.nameInput.setFocusableInTouchMode(ownProfile);

        binding.changePhotoButton.setVisibility(
                ownProfile ? View.VISIBLE : View.GONE
        );
        binding.saveProfileButton.setVisibility(
                ownProfile ? View.VISIBLE : View.GONE
        );

        if (ownProfile) {
            binding.changePhotoButton.setOnClickListener(
                    v -> imagePicker.launch("image/*")
            );

            binding.saveProfileButton.setOnClickListener(
                    v -> saveProfile()
            );
        }

        userListener = repository.listenToUser(
                profileUid,
                (snapshot, error) -> {
                    if (!isAdded() || binding == null) return;

                    if (error != null) {
                        showMessage("No se pudo cargar el perfil.");
                        return;
                    }

                    if (snapshot == null || !snapshot.exists()) {
                        showMessage("No se encontró el perfil.");
                        return;
                    }

                    showProfile(snapshot);
                }
        );
    }

    private void showProfile(DocumentSnapshot snapshot) {
        if (loadingProfile) {
            String name = snapshot.getString("name");
            binding.nameInput.setText(name != null ? name : "");

            String email = snapshot.getString("email");
            binding.emailText.setText(email != null ? email : "");

            loadingProfile = false;
        }

        Boolean online = snapshot.getBoolean("online");
        Long lastSeen = snapshot.getLong("lastSeen");

        if (Boolean.TRUE.equals(online)) {
            binding.presenceText.setText("En línea");
        } else if (lastSeen != null && lastSeen > 0) {
            String date = DateFormat.getDateTimeInstance(
                    DateFormat.SHORT,
                    DateFormat.SHORT
            ).format(new Date(lastSeen));

            binding.presenceText.setText("Última vez: " + date);
        } else {
            binding.presenceText.setText("Desconectado");
        }

        UserPhotoUtils.loadPhoto(
                binding.profileImage,
                snapshot.getString("photoBase64"),
                snapshot.getString("photoUrl")
        );
    }

    private void onImageSelected(Uri uri) {
        if (uri == null || binding == null || !ownProfile) return;

        try (InputStream input = requireContext()
                .getContentResolver().openInputStream(uri)) {

            if (input == null) {
                showMessage("No se pudo abrir la imagen.");
                return;
            }

            Bitmap original = BitmapFactory.decodeStream(input);

            if (original == null) {
                showMessage("La imagen seleccionada no es válida.");
                return;
            }

            int maxSide = 256;
            float scale = Math.min(
                    1f,
                    (float) maxSide / Math.max(
                            original.getWidth(),
                            original.getHeight()
                    )
            );

            int width = Math.max(
                    1, Math.round(original.getWidth() * scale)
            );
            int height = Math.max(
                    1, Math.round(original.getHeight() * scale)
            );

            Bitmap resized = Bitmap.createScaledBitmap(
                    original, width, height, true
            );

            if (resized != original) {
                original.recycle();
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();

            boolean compressed = resized.compress(
                    Bitmap.CompressFormat.JPEG, 60, output
            );
            resized.recycle();

            if (!compressed) {
                showMessage("No se pudo comprimir la imagen.");
                return;
            }

            selectedPhotoBase64 = Base64.encodeToString(
                    output.toByteArray(),
                    Base64.NO_WRAP
            );

            if (selectedPhotoBase64.length() > 700 * 1024) {
                selectedPhotoBase64 = null;
                showMessage("Selecciona una imagen más pequeña.");
                return;
            }

            byte[] bytes = Base64.decode(
                    selectedPhotoBase64, Base64.DEFAULT
            );

            Bitmap preview = BitmapFactory.decodeByteArray(
                    bytes, 0, bytes.length
            );

            if (preview != null) {
                binding.profileImage.setImageBitmap(preview);
            }

        } catch (Exception e) {
            showMessage("No se pudo procesar la imagen.");
        }
    }

    private void saveProfile() {
        if (binding == null || !ownProfile) return;

        String name = binding.nameInput.getText()
                .toString().trim();

        if (name.isEmpty()) {
            binding.nameInput.setError("Escribe tu nombre.");
            return;
        }

        binding.saveProfileButton.setEnabled(false);
        binding.profileProgress.setVisibility(View.VISIBLE);

        repository.updateProfile(
                name,
                selectedPhotoBase64,
                new ProfileRepository.Callback() {
                    @Override
                    public void onSuccess() {
                        if (binding == null) return;

                        binding.saveProfileButton.setEnabled(true);
                        binding.profileProgress.setVisibility(View.GONE);
                        selectedPhotoBase64 = null;

                        showMessage("Perfil actualizado.");
                    }

                    @Override
                    public void onError(Exception error) {
                        if (binding == null) return;

                        binding.saveProfileButton.setEnabled(true);
                        binding.profileProgress.setVisibility(View.GONE);

                        String detail = error != null
                                ? error.getLocalizedMessage()
                                : null;

                        showMessage(
                                detail != null
                                        ? detail
                                        : "No se pudo guardar el perfil."
                        );
                    }
                }
        );
    }

    private void showMessage(String message) {
        if (isAdded()) {
            Toast.makeText(
                    requireContext(),
                    message,
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    @Override
    public void onDestroyView() {
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }

        binding = null;
        super.onDestroyView();
    }
}
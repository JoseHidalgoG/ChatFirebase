package com.example.chatfirebase.ui.auth;

import android.util.Patterns;

import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.chatfirebase.R;
import com.example.chatfirebase.data.repository.AuthRepository;
import com.example.chatfirebase.model.User;
import com.example.chatfirebase.util.Resource;

public class AuthViewModel extends ViewModel {

    private static final int MIN_PASSWORD_LENGTH = 6;

    private final AuthRepository repository;

    private final MutableLiveData<Resource<User>> authState = new MutableLiveData<>();
    private final MutableLiveData<Integer> nameError = new MutableLiveData<>();
    private final MutableLiveData<Integer> emailError = new MutableLiveData<>();
    private final MutableLiveData<Integer> passwordError = new MutableLiveData<>();
    private final MutableLiveData<Integer> confirmPasswordError = new MutableLiveData<>();

    public AuthViewModel() {
        this(new AuthRepository());
    }

    public AuthViewModel(AuthRepository repository) {
        this.repository = repository;
    }

    public LiveData<Resource<User>> getAuthState() {
        return authState;
    }

    public LiveData<Integer> getNameError() {
        return nameError;
    }

    public LiveData<Integer> getEmailError() {
        return emailError;
    }

    public LiveData<Integer> getPasswordError() {
        return passwordError;
    }

    public LiveData<Integer> getConfirmPasswordError() {
        return confirmPasswordError;
    }

    public boolean isLoggedIn() {
        return repository.getCurrentUser() != null;
    }

    public void login(String email, String password) {
        if (isLoading()) {
            return;
        }
        email = email.trim();
        boolean valid = validateEmail(email);
        valid &= setError(passwordError, password.isEmpty() ? R.string.error_field_required : 0);
        if (!valid) {
            return;
        }
        authState.setValue(Resource.loading());
        repository.login(email, password, callback());
    }

    public void register(String name, String email, String password, String confirmPassword) {
        if (isLoading()) {
            return;
        }
        name = name.trim();
        email = email.trim();
        boolean valid = setError(nameError, name.isEmpty() ? R.string.error_field_required : 0);
        valid &= validateEmail(email);
        valid &= validatePassword(password);
        valid &= setError(confirmPasswordError,
                password.equals(confirmPassword) ? 0 : R.string.error_password_mismatch);
        if (!valid) {
            return;
        }
        authState.setValue(Resource.loading());
        repository.register(name, email, password, callback());
    }

    private AuthRepository.Callback<User> callback() {
        return new AuthRepository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                authState.setValue(Resource.success(user));
            }

            @Override
            public void onError(int messageRes) {
                authState.setValue(Resource.error(messageRes));
            }
        };
    }

    private boolean isLoading() {
        Resource<User> state = authState.getValue();
        return state != null && state.status == Resource.Status.LOADING;
    }

    private boolean validateEmail(String email) {
        int error = 0;
        if (email.isEmpty()) {
            error = R.string.error_field_required;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            error = R.string.error_email_invalid;
        }
        return setError(emailError, error);
    }

    private boolean validatePassword(String password) {
        int error = 0;
        if (password.isEmpty()) {
            error = R.string.error_field_required;
        } else if (password.length() < MIN_PASSWORD_LENGTH) {
            error = R.string.error_password_short;
        }
        return setError(passwordError, error);
    }

    /** Envía el error del campo (0 lo quita) y devuelve si el campo es válido. */
    private static boolean setError(MutableLiveData<Integer> field, @StringRes int error) {
        field.setValue(error != 0 ? error : null);
        return error == 0;
    }
}

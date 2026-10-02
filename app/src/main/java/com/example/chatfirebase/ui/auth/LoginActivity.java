package com.example.chatfirebase.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.example.chatfirebase.MainActivity;
import com.example.chatfirebase.databinding.ActivityLoginBinding;
import com.example.chatfirebase.model.User;
import com.example.chatfirebase.util.Resource;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // FirebaseAuth persists the session, so returning users skip this screen.
        if (viewModel.isLoggedIn()) {
            openMain();
            return;
        }

        EdgeToEdge.enable(this);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.loginButton.setOnClickListener(v -> submit());
        binding.passwordInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit();
                return true;
            }
            return false;
        });
        binding.goToRegisterButton.setOnClickListener(
                v -> startActivity(new Intent(this, RegisterActivity.class)));

        bindFieldError(viewModel.getEmailError(), binding.emailLayout);
        bindFieldError(viewModel.getPasswordError(), binding.passwordLayout);
        viewModel.getAuthState().observe(this, this::render);
    }

    private void submit() {
        viewModel.login(
                String.valueOf(binding.emailInput.getText()),
                String.valueOf(binding.passwordInput.getText()));
    }

    private void render(Resource<User> state) {
        boolean loading = state.status == Resource.Status.LOADING;
        binding.progress.setVisibility(loading ? View.VISIBLE : View.INVISIBLE);
        binding.loginButton.setEnabled(!loading);
        binding.goToRegisterButton.setEnabled(!loading);

        if (state.status == Resource.Status.SUCCESS) {
            openMain();
        } else if (state.status == Resource.Status.ERROR) {
            Snackbar.make(binding.getRoot(), state.getMessage(this), Snackbar.LENGTH_LONG).show();
        }
    }

    private void bindFieldError(LiveData<Integer> error, TextInputLayout layout) {
        error.observe(this, res -> layout.setError(res != null ? getString(res) : null));
    }

    private void openMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

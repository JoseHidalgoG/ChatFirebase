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
import com.example.chatfirebase.databinding.ActivityRegisterBinding;
import com.example.chatfirebase.model.User;
import com.example.chatfirebase.util.Resource;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        binding.registerButton.setOnClickListener(v -> submit());
        binding.confirmPasswordInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit();
                return true;
            }
            return false;
        });
        binding.goToLoginButton.setOnClickListener(v -> finish());

        bindFieldError(viewModel.getNameError(), binding.nameLayout);
        bindFieldError(viewModel.getEmailError(), binding.emailLayout);
        bindFieldError(viewModel.getPasswordError(), binding.passwordLayout);
        bindFieldError(viewModel.getConfirmPasswordError(), binding.confirmPasswordLayout);
        viewModel.getAuthState().observe(this, this::render);
    }

    private void submit() {
        viewModel.register(
                String.valueOf(binding.nameInput.getText()),
                String.valueOf(binding.emailInput.getText()),
                String.valueOf(binding.passwordInput.getText()),
                String.valueOf(binding.confirmPasswordInput.getText()));
    }

    private void render(Resource<User> state) {
        boolean loading = state.status == Resource.Status.LOADING;
        binding.progress.setVisibility(loading ? View.VISIBLE : View.INVISIBLE);
        binding.registerButton.setEnabled(!loading);
        binding.goToLoginButton.setEnabled(!loading);

        if (state.status == Resource.Status.SUCCESS) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else if (state.status == Resource.Status.ERROR) {
            Snackbar.make(binding.getRoot(), state.getMessage(this), Snackbar.LENGTH_LONG).show();
        }
    }

    private void bindFieldError(LiveData<Integer> error, TextInputLayout layout) {
        error.observe(this, res -> layout.setError(res != null ? getString(res) : null));
    }
}

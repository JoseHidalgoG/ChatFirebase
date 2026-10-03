package com.example.chatfirebase.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.chatfirebase.databinding.FragmentProfileBinding;
import com.example.chatfirebase.ui.main.MainViewModel;


public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Mismo ViewModel que MainActivity, así ambos ven la misma sesión.
        MainViewModel viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                binding.nameText.setText(user.getName());
                binding.emailText.setText(user.getEmail());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Al cambiar de pestaña el fragment sobrevive sin vista; no hay que retenerla.
        binding = null;
    }
}

package com.example.chatfirebase;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.chatfirebase.databinding.FragmentFirstBinding;
import com.example.chatfirebase.model.User;
import com.example.chatfirebase.ui.contacts.ContactsAdapter;
import com.example.chatfirebase.ui.contacts.ContactsViewModel;

public class FirstFragment extends Fragment {
    private FragmentFirstBinding binding;
    private ContactsAdapter adapter;
    private ContactsViewModel viewModel;

    public FirstFragment() {
        super(R.layout.fragment_first);
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull android.view.LayoutInflater inflater,
            @Nullable android.view.ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentFirstBinding.inflate(
                inflater, container, false
        );

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new ContactsAdapter(this::onContactSelected);

        binding.recyclerContacts.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        binding.recyclerContacts.setAdapter(adapter);

        viewModel = new ViewModelProvider(this)
                .get(ContactsViewModel.class);

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getContacts().observe(
                getViewLifecycleOwner(),
                users -> {
                    adapter.setContacts(users);

                    boolean empty = users == null || users.isEmpty();

                    binding.textEmpty.setVisibility(
                            empty ? View.VISIBLE : View.GONE
                    );

                    binding.recyclerContacts.setVisibility(
                            empty ? View.GONE : View.VISIBLE
                    );
                }
        );

        viewModel.getError().observe(
                getViewLifecycleOwner(),
                message -> {
                    if (message != null && !message.isEmpty()) {
                        Toast.makeText(
                                requireContext(),
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void onContactSelected(User user) {
        Toast.makeText(
                requireContext(),
                "Seleccionaste a " + user.getName()
                        + ". Próximamente abriremos el chat.",
                Toast.LENGTH_SHORT
        ).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        binding = null;
        adapter = null;
    }
}
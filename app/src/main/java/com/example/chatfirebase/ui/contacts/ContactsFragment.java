package com.example.chatfirebase.ui.contacts;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.chatfirebase.R;
import com.example.chatfirebase.databinding.FragmentContactsBinding;
import com.example.chatfirebase.model.User;

import android.os.Bundle;
import android.widget.Toast;

import androidx.navigation.fragment.NavHostFragment;

import com.example.chatfirebase.data.repository.ChatRepository;
import com.example.chatfirebase.model.User;

public class ContactsFragment extends Fragment {

    private FragmentContactsBinding binding;
    private ContactsAdapter adapter;
    private ContactsViewModel viewModel;

    public ContactsFragment() {
        super(R.layout.fragment_contacts);
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull android.view.LayoutInflater inflater,
            @Nullable android.view.ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentContactsBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
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

                    binding.emptyState.setVisibility(
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

    private void onContactSelected(User contact) {
        viewModel.openPrivateChat(
                contact.getUid(),
                new ChatRepository.Callback() {
                    @Override
                    public void onSuccess(String chatId) {
                        Bundle args = new Bundle();
                        args.putString("chatId", chatId);
                        args.putString("otherUserId", contact.getUid());
                        args.putString("otherUserName", contact.getName());

                        NavHostFragment.findNavController(
                                ContactsFragment.this
                        ).navigate(
                                R.id.action_contactsFragment_to_chatFragment,
                                args
                        );
                    }

                    @Override
                    public void onError(Exception error) {
                        if (getContext() != null) {
                            Toast.makeText(
                                    requireContext(),
                                    "No se pudo abrir el chat: "
                                            + error.getLocalizedMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
                }
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        binding = null;
        adapter = null;
    }
}
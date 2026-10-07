package com.example.chatfirebase.ui.chats;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatfirebase.R;
import com.example.chatfirebase.model.Chat;

public class ChatsFragment extends Fragment {

    private ChatListViewModel viewModel;
    private ChatListAdapter adapter;
    private RecyclerView recyclerChats;
    private LinearLayout emptyState;

    public ChatsFragment() {
        super(R.layout.fragment_chats);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        recyclerChats = view.findViewById(R.id.recycler_chats);
        emptyState = view.findViewById(R.id.empty_state);

        recyclerChats.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        adapter = new ChatListAdapter(this::openChat);
        recyclerChats.setAdapter(adapter);

        viewModel = new ViewModelProvider(this)
                .get(ChatListViewModel.class);

        viewModel.getChats().observe(getViewLifecycleOwner(), chats -> {
            adapter.submitList(chats);

            boolean isEmpty = chats == null || chats.isEmpty();

            emptyState.setVisibility(
                    isEmpty ? View.VISIBLE : View.GONE
            );

            recyclerChats.setVisibility(
                    isEmpty ? View.GONE : View.VISIBLE
            );
        });

        viewModel.getErrorMessage().observe(
                getViewLifecycleOwner(),
                error -> {
                    if (error != null && !error.isEmpty()) {
                        Toast.makeText(
                                requireContext(),
                                error,
                                Toast.LENGTH_LONG
                        ).show();

                        viewModel.clearErrorMessage();
                    }
                }
        );

        viewModel.startListening();
    }

    private void openChat(Chat chat) {
        if (chat == null || chat.getId() == null) {
            return;
        }

        String otherUserId = "";

        if (chat.getParticipants() != null) {
            String currentUid =
                    com.google.firebase.auth.FirebaseAuth
                            .getInstance()
                            .getCurrentUser() != null
                            ? com.google.firebase.auth.FirebaseAuth
                            .getInstance()
                            .getCurrentUser()
                            .getUid()
                            : null;

            for (String participantId : chat.getParticipants()) {
                if (participantId != null
                        && !participantId.equals(currentUid)) {
                    otherUserId = participantId;
                    break;
                }
            }
        }

        String otherUserName =
                "group".equals(chat.getType())
                        ? chat.getName()
                        : chat.getContactName();

        if (otherUserName == null || otherUserName.trim().isEmpty()) {
            otherUserName = "Usuario";
        }

        Bundle args = new Bundle();
        args.putString("chatId", chat.getId());
        args.putString("otherUserId", otherUserId);
        args.putString("otherUserName", otherUserName);

        NavController navController =
                Navigation.findNavController(requireView());

        navController.navigate(
                R.id.action_chatsFragment_to_chatFragment,
                args
        );
    }
}

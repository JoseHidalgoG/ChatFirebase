
package com.example.chatfirebase.ui.chats;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatfirebase.R;
import com.google.firebase.auth.FirebaseAuth;

public class ChatFragment extends Fragment {

    private ChatViewModel viewModel;
    private RecyclerView recyclerMessages;
    private TextView textEmptyMessages;
    private EditText editMessage;
    private ImageButton btnSendMessage;
    private ImageButton btnAttachImage;
    private Toolbar toolbarChat;

    private MessageAdapter messageAdapter;

    private String chatId;
    private String otherUserId;
    private String otherUserName;

    public ChatFragment() {
        super(R.layout.fragment_chat);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);
        readArguments();
        updateMainToolbarTitle();
        bindViews(view);
        setupRecyclerView();
        setupViewModel();
        setupListeners();
    }

    private void readArguments() {
        Bundle args = getArguments();

        if (args != null) {
            chatId = args.getString("chatId");
            otherUserId = args.getString("otherUserId");
            otherUserName = args.getString("otherUserName");
        }
    }

    private void bindViews(View view) {
       // toolbarChat = view.findViewById(R.id.toolbar_chat);
        recyclerMessages = view.findViewById(R.id.recycler_messages);
        textEmptyMessages = view.findViewById(R.id.text_empty_messages);
        editMessage = view.findViewById(R.id.edit_message);
        btnSendMessage = view.findViewById(R.id.btn_send_message);
        btnAttachImage = view.findViewById(R.id.btn_attach_image);
    }

    private void updateMainToolbarTitle() {
        androidx.appcompat.widget.Toolbar toolbar = requireActivity().findViewById(R.id.toolbar);

        if (toolbar != null) {
            if (otherUserName != null && !otherUserName.trim().isEmpty()) {
                toolbar.setTitle(otherUserName);
            } else {
                toolbar.setTitle("Conversación");
            }
        }
    }

    private void setupRecyclerView() {
        String currentUserId = FirebaseAuth.getInstance()
                .getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        messageAdapter = new MessageAdapter(currentUserId);

        LinearLayoutManager layoutManager =
                new LinearLayoutManager(requireContext());

        // Los mensajes más recientes se mostrarán abajo.
        layoutManager.setStackFromEnd(true);

        recyclerMessages.setLayoutManager(layoutManager);
        recyclerMessages.setAdapter(messageAdapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this)
                .get(ChatViewModel.class);

        viewModel.getMessages().observe(
                getViewLifecycleOwner(),
                messages -> {
                    messageAdapter.setMessages(messages);

                    boolean isEmpty = messages == null || messages.isEmpty();

                    textEmptyMessages.setVisibility(
                            isEmpty ? View.VISIBLE : View.GONE
                    );

                    if (!isEmpty) {
                        recyclerMessages.scrollToPosition(
                                messages.size() - 1
                        );
                    }
                }
        );

        viewModel.getErrorMessage().observe(
                getViewLifecycleOwner(),
                error -> {
                    if (error != null && !error.trim().isEmpty()) {
                        Toast.makeText(
                                requireContext(),
                                error,
                                Toast.LENGTH_LONG
                        ).show();

                        viewModel.clearErrorMessage();
                    }
                }
        );

        viewModel.getSending().observe(
                getViewLifecycleOwner(),
                isSending -> {
                    boolean sending = Boolean.TRUE.equals(isSending);
                    btnSendMessage.setEnabled(!sending);
                }
        );

        viewModel.getMessageSent().observe(
                getViewLifecycleOwner(),
                sent -> {
                    if (Boolean.TRUE.equals(sent)) {
                        editMessage.setText("");
                    }
                }
        );

        if (chatId != null && !chatId.trim().isEmpty()) {
            viewModel.loadMessages(chatId);
        } else {
            Toast.makeText(
                    requireContext(),
                    "No se recibió el identificador de la conversación.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void setupListeners() {
        btnSendMessage.setOnClickListener(v -> sendCurrentMessage());

        editMessage.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendCurrentMessage();
                return true;
            }

            return false;
        });

        // conectar al selector de imagenes!!!.
        btnAttachImage.setOnClickListener(v ->
                Toast.makeText(
                        requireContext(),
                        "La opción de imágenes se implementará después.",
                        Toast.LENGTH_SHORT
                ).show()
        );
    }

    private void sendCurrentMessage() {
        String text = editMessage.getText().toString();

        if (text.trim().isEmpty()) {
            editMessage.setError("Escribe un mensaje");
            return;
        }

        viewModel.sendTextMessage(text);
    }
}
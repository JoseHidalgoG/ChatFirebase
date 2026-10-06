package com.example.chatfirebase.ui.contacts;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatfirebase.R;
import com.example.chatfirebase.model.User;
import com.example.chatfirebase.utils.UserPhotoUtils;

import java.util.ArrayList;
import java.util.List;

public class ContactsAdapter
        extends RecyclerView.Adapter<ContactsAdapter.ContactViewHolder> {

    public interface OnContactClickListener {
        void onContactClick(User user);
        void onChatClick(User user);
    }

    private final List<User> contacts = new ArrayList<>();
    private final OnContactClickListener listener;

    public ContactsAdapter(OnContactClickListener listener) {
        this.listener = listener;
    }

    public void setContacts(List<User> newContacts) {
        contacts.clear();

        if (newContacts != null) {
            contacts.addAll(newContacts);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact, parent, false);

        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ContactViewHolder holder,
            int position
    ) {
        User user = contacts.get(position);

        String name = user.getName();
        holder.name.setText(
                name == null || name.trim().isEmpty()
                        ? "Usuario"
                        : name
        );

        holder.email.setText(
                user.getEmail() == null ? "" : user.getEmail()
        );

        UserPhotoUtils.loadPhoto(
                holder.avatar,
                user.getPhotoBase64(),
                user.getPhotoUrl()
        );

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onContactClick(user);
            }
        });

        holder.openChatButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChatClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        ImageView avatar;
        TextView name;
        TextView email;
        Button openChatButton;

        ContactViewHolder(@NonNull View itemView) {
            super(itemView);

            avatar = itemView.findViewById(R.id.image_contact_avatar);
            name = itemView.findViewById(R.id.text_contact_name);
            email = itemView.findViewById(R.id.text_contact_email);
            openChatButton = itemView.findViewById(R.id.button_open_chat);
        }
    }
}
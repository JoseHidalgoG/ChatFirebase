package com.example.chatfirebase.ui.contacts;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatfirebase.R;
import com.example.chatfirebase.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactViewHolder> {
    public interface OnContactClickListener {
        void onContactClick(User user);
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
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact, parent, false);

        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder( @NonNull ContactViewHolder holder, int position) {
        User user = contacts.get(position);

        String name = user.getName();
        String email = user.getEmail();

        holder.name.setText(
                name == null || name.trim().isEmpty()
                        ? "Usuario"
                        : name
        );

        holder.email.setText(
                email == null ? "" : email
        );

        String initial = (name == null || name.trim().isEmpty())
                ? "U"
                : name.trim().substring(0, 1).toUpperCase(Locale.ROOT);

        holder.avatar.setText(initial);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onContactClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView avatar;
        TextView name;
        TextView email;

        ContactViewHolder(@NonNull View itemView) {
            super(itemView);

            avatar = itemView.findViewById(R.id.text_avatar);
            name = itemView.findViewById(R.id.text_contact_name);
            email = itemView.findViewById(R.id.text_contact_email);
        }
    }
}
package com.example.chatfirebase.ui.chats;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatfirebase.R;
import com.example.chatfirebase.model.Chat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {
    public interface OnChatClickListener {
        void onChatClick(Chat chat);
    }

    private final List<Chat> chats = new ArrayList<>();
    private final OnChatClickListener listener;

    public ChatListAdapter(OnChatClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Chat> newChats) {
        chats.clear();

        if (newChats != null) {
            chats.addAll(newChats);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat, parent, false);

        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = chats.get(position);

        String displayName;

        if ("group".equals(chat.getType())) {
            displayName = chat.getName();
        } else {
            displayName = chat.getContactName();
        }

        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = "Usuario";
        }

        holder.name.setText(displayName);

        String lastMessage = chat.getLastMessage();

        if (lastMessage == null || lastMessage.trim().isEmpty()) {
            lastMessage = "Inicia la conversación";
        }

        holder.lastMessage.setText(lastMessage);

        long timestamp = chat.getLastMessageAt();

        if (timestamp > 0) {
            Date date = new Date(timestamp);
            Date today = new Date();

            SimpleDateFormat formatter;

            if (isSameDay(date, today)) {
                formatter = new SimpleDateFormat(
                        "HH:mm", Locale.getDefault()
                );
            } else {
                formatter = new SimpleDateFormat(
                        "dd/MM/yy", Locale.getDefault()
                );
            }

            holder.time.setText(formatter.format(date));
            holder.time.setVisibility(View.VISIBLE);
        } else {
            holder.time.setText("");
            holder.time.setVisibility(View.GONE);
        }

        String photoUrl = chat.getContactPhotoUrl();

        if (!"group".equals(chat.getType())
                && photoUrl != null
                && !photoUrl.trim().isEmpty()) {

            Glide.with(holder.itemView)
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_chat)
                    .error(R.drawable.ic_chat)
                    .circleCrop()
                    .into(holder.avatar);

        } else {
            Glide.with(holder.itemView).clear(holder.avatar);
            holder.avatar.setImageResource(R.drawable.ic_chat);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChatClick(chat);
            }
        });
    }

    private boolean isSameDay(Date first, Date second) {
        SimpleDateFormat formatter =
                new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

        return formatter.format(first).equals(formatter.format(second));
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {

        ImageView avatar;
        TextView name;
        TextView lastMessage;
        TextView time;

        ChatViewHolder(@NonNull View itemView) {
            super(itemView);

            avatar = itemView.findViewById(R.id.image_chat_avatar);
            name = itemView.findViewById(R.id.text_chat_name);
            lastMessage = itemView.findViewById(
                    R.id.text_chat_last_message
            );
            time = itemView.findViewById(R.id.text_chat_time);
        }
    }
}
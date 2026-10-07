package com.example.chatfirebase.ui.chats;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatfirebase.R;
import com.example.chatfirebase.model.Message;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter
        extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    private final String currentUserId;
    private final List<Message> messages = new ArrayList<>();

    public MessageAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);

        if (currentUserId != null
                && currentUserId.equals(message.getSenderId())) {
            return TYPE_SENT;
        }

        return TYPE_RECEIVED;
    }

    //           REVISAR EL EXPOSURE!!!!!!!!!!!
    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = viewType == TYPE_SENT
                ? R.layout.item_message_sent
                : R.layout.item_message_received;

        View view = LayoutInflater.from(parent.getContext())
                .inflate(layoutId, parent, false);

        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);

        String type = message.getType();

        if ("image".equals(type)) {
            String encoded = message.getImageBase64();

            if (encoded != null && !encoded.isEmpty()) {
                holder.imageMessage.setVisibility(View.VISIBLE);
                holder.textMessage.setVisibility(View.GONE);

                try {
                    byte[] imageBytes = Base64.decode(encoded, Base64.DEFAULT);

                    Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

                    if (bitmap != null) {
                        holder.imageMessage.setImageBitmap(bitmap);
                    } else {
                        holder.imageMessage.setImageResource(
                                android.R.drawable.ic_menu_report_image
                        );
                    }

                } catch (IllegalArgumentException e) {
                    holder.imageMessage.setImageResource(
                            android.R.drawable.ic_menu_report_image
                    );
                }

            } else {
                holder.imageMessage.setVisibility(View.GONE);
                holder.textMessage.setVisibility(View.VISIBLE);
                holder.textMessage.setText("Imagen no disponible");
            }
        } else {
            // Limpiar imágenes recicladas al mostrar un mensaje
            holder.imageMessage.setImageDrawable(null);
            holder.imageMessage.setVisibility(View.GONE);
            holder.textMessage.setVisibility(View.VISIBLE);
            holder.textMessage.setText(message.getText());
        }

        holder.textMessageTime.setText(
                formatTime(message.getCreatedAt())
        );
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void setMessages(List<Message> newMessages) {
        messages.clear();

        if (newMessages != null) {
            messages.addAll(newMessages);
        }

        notifyDataSetChanged();
    }

    private String formatTime(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }

        Date date = timestamp.toDate();

        SimpleDateFormat formatter =
                new SimpleDateFormat("HH:mm", Locale.getDefault());

        return formatter.format(date);
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        final TextView textMessage;
        final TextView textMessageTime;
        final ImageView imageMessage;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);

            textMessage = itemView.findViewById(R.id.text_message);
            textMessageTime = itemView.findViewById(R.id.text_message_time);
            imageMessage = itemView.findViewById(R.id.image_message);
        }
    }
}
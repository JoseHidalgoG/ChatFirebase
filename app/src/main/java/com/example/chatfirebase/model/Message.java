package com.example.chatfirebase.model;

import com.google.firebase.Timestamp;

public class Message {

    private String messageId;
    private String senderId;
    private String text;
    private String type;
    private String imageUrl;
    private Timestamp createdAt;
    public Message() {}

    public Message(String messageId, String senderId, String text, String type,
            String imageUrl, Timestamp createdAt) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.text = text;
        this.type = type;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}

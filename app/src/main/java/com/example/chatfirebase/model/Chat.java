package com.example.chatfirebase.model;

import java.util.ArrayList;
import java.util.List;

public class Chat {

    private String id;
    private List<String> participants;
    private String type;
    private String name;
    private String lastMessage;
    private long lastMessageAt;
    private String lastMessageSenderId;
    private String createdBy;
    private long createdAt;

    public Chat() { }

    public Chat(String id, List<String> participants, String type,
                String name, String createdBy) {
        this.id = id;
        this.participants = participants != null
                ? participants : new ArrayList<>();
        this.type = type;
        this.name = name;
        this.createdBy = createdBy;
        this.createdAt = System.currentTimeMillis();
        this.lastMessageAt = this.createdAt;
        this.lastMessage = "";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(long lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public String getLastMessageSenderId() {
        return lastMessageSenderId;
    }

    public void setLastMessageSenderId(String lastMessageSenderId) {
        this.lastMessageSenderId = lastMessageSenderId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
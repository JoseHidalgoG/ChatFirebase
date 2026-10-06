package com.example.chatfirebase.model;

/**
 * Usuario, guardado en user/{uid}
 * Necesita un constructor vacio y getters/setters para Firestore
 */
public class User {

    private String uid;
    private String name;
    private String email;
    private String photoUrl; //dejar por compatibildad
    private String fcmToken;
    private long lastSeen;
    private String photoBase64;
    private boolean online;

    public User() {
    }

    public User(String uid, String name, String email) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.lastSeen = System.currentTimeMillis();
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public String getPhotoBase64() { return photoBase64; }

    public void setPhotoBase64(String photoBase64) { this.photoBase64 = photoBase64; }

    public boolean isOnline() { return online; }

    public void setOnline(boolean online) { this.online = online; }
}

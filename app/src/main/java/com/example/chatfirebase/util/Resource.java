package com.example.chatfirebase.util;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;


public class Resource<T> {

    public enum Status { LOADING, SUCCESS, ERROR }

    @NonNull
    public final Status status;
    @Nullable
    public final T data;

    @Nullable
    public final String message;

    @StringRes
    public final int messageRes;

    private Resource(@NonNull Status status, @Nullable T data,
                     @Nullable String message, @StringRes int messageRes) {
        this.status = status;
        this.data = data;
        this.message = message;
        this.messageRes = messageRes;
    }

    public static <T> Resource<T> loading() {
        return new Resource<>(Status.LOADING, null, null, 0);
    }

    public static <T> Resource<T> success(@Nullable T data) {
        return new Resource<>(Status.SUCCESS, data, null, 0);
    }

    public static <T> Resource<T> error(@NonNull String message) {
        return new Resource<>(Status.ERROR, null, message, 0);
    }

    public static <T> Resource<T> error(@StringRes int messageRes) {
        return new Resource<>(Status.ERROR, null, null, messageRes);
    }


    @NonNull
    public String getMessage(@NonNull Context context) {
        if (messageRes != 0) {
            return context.getString(messageRes);
        }
        return message != null ? message : "";
    }
}

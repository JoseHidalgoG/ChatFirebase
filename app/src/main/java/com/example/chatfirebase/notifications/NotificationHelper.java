package com.example.chatfirebase.notifications;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.chatfirebase.MainActivity;
import com.example.chatfirebase.R;
import com.example.chatfirebase.util.Constants;

/** Crea el canal de mensajes y publica las notificaciones de la app. */
public final class NotificationHelper {

    // Los avisos de la consola de FCM se reemplazan entre sí.
    private static final int GENERIC_NOTIFICATION_ID = 1;

    // Chat abierto en pantalla; sus mensajes no generan notificación.
    private static volatile String activeChatId;

    private NotificationHelper() {
    }

    public static void setActiveChatId(@Nullable String chatId) {
        activeChatId = chatId;
    }

    /** Solo lo limpia si sigue siendo ese chat, por si el siguiente ya se registró. */
    public static void clearActiveChatId(@Nullable String chatId) {
        if (chatId != null && chatId.equals(activeChatId)) {
            activeChatId = null;
        }
    }

    /** Crea el canal de mensajes; repetirlo no tiene efecto. */
    public static void ensureChannel(@NonNull Context context) {
        NotificationChannelCompat channel = new NotificationChannelCompat.Builder(
                context.getString(R.string.notification_channel_id),
                NotificationManagerCompat.IMPORTANCE_HIGH
        )
                .setName(context.getString(R.string.notification_channel_name))
                .setDescription(context.getString(R.string.notification_channel_description))
                .build();

        NotificationManagerCompat.from(context).createNotificationChannel(channel);
    }

    /** Aviso de un mensaje nuevo; al tocarlo se abre esa conversación. */
    public static void showMessage(
            @NonNull Context context,
            @NonNull String chatId,
            @NonNull String senderId,
            @Nullable String senderName,
            @Nullable String text
    ) {
        // El usuario ya está viendo esa conversación.
        if (chatId.equals(activeChatId)) {
            return;
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(Constants.EXTRA_CHAT_ID, chatId);
        intent.putExtra(Constants.EXTRA_OTHER_USER_ID, senderId);
        intent.putExtra(Constants.EXTRA_OTHER_USER_NAME, senderName);

        // Un id por chat: un mensaje nuevo reemplaza al aviso anterior de la misma conversación.
        int notificationId = chatId.hashCode();

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        post(context, notificationId, senderName, text, pendingIntent);
    }

    /** Aviso sin conversación asociada, p. ej. un mensaje de prueba de la consola de FCM. */
    public static void showGeneric(
            @NonNull Context context,
            @Nullable String title,
            @Nullable String body
    ) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                GENERIC_NOTIFICATION_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        post(context, GENERIC_NOTIFICATION_ID, title, body, pendingIntent);
    }

    public static void cancel(@NonNull Context context, @NonNull String chatId) {
        NotificationManagerCompat.from(context).cancel(chatId.hashCode());
    }

    private static void post(
            Context context,
            int notificationId,
            @Nullable String title,
            @Nullable String text,
            PendingIntent contentIntent
    ) {
        // Sin permiso (Android 13+) no se muestra nada; el resto de la app sigue igual.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        ensureChannel(context);

        String safeTitle = title != null && !title.trim().isEmpty()
                ? title
                : context.getString(R.string.notification_default_title);

        Notification notification = new NotificationCompat.Builder(
                context,
                context.getString(R.string.notification_channel_id)
        )
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(safeTitle)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                // Para Android 7, donde aún no hay canales.
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build();

        NotificationManagerCompat.from(context).notify(notificationId, notification);
    }
}

package com.example.chatfirebase.notifications;

import androidx.annotation.NonNull;

import com.example.chatfirebase.data.repository.FcmTokenRepository;
import com.example.chatfirebase.util.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Servicio de FCM: mantiene el token al día y muestra los pushes de mensajes nuevos
 * que envía la Cloud Function notifyNewMessage.
 */
public class ChatMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        new FcmTokenRepository().saveToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();
        String chatId = data.get(Constants.DATA_CHAT_ID);

        // Push de datos de la Cloud Function: llega aquí siempre, con la app abierta o cerrada.
        if (chatId != null && !chatId.isEmpty()) {
            showChatMessage(chatId, data);
            return;
        }

        // Mensaje de prueba de la consola con la app en primer plano
        // (en segundo plano el sistema lo muestra solo).
        RemoteMessage.Notification notification = remoteMessage.getNotification();

        if (notification != null) {
            NotificationHelper.showGeneric(
                    this,
                    notification.getTitle(),
                    notification.getBody()
            );
        }
    }

    private void showChatMessage(String chatId, Map<String, String> data) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String senderId = data.get(Constants.DATA_SENDER_ID);

        // El token pudo quedar en el perfil de otra cuenta que usó este dispositivo.
        if (user == null || senderId == null
                || !user.getUid().equals(data.get(Constants.DATA_RECIPIENT_ID))) {
            return;
        }

        NotificationHelper.showMessage(
                this,
                chatId,
                senderId,
                data.get(Constants.DATA_SENDER_NAME),
                data.get(Constants.DATA_TEXT)
        );
    }
}

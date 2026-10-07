/**
 * Cloud Functions de ChatFirebase.
 *
 * notifyNewMessage: cuando se crea un mensaje en chats/{chatId}/messages, envía un push
 * FCM de solo datos a cada participante que no sea el remitente. La app lo muestra en
 * ChatMessagingService; las claves deben coincidir con Constants.DATA_* en Android.
 */
const {setGlobalOptions} = require("firebase-functions/v2");
const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const logger = require("firebase-functions/logger");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore, FieldValue} = require("firebase-admin/firestore");
const {getMessaging} = require("firebase-admin/messaging");

initializeApp();

// Misma región que la base de datos de Firestore (nam5 → us-central1), para no sumar latencia.
setGlobalOptions({region: "us-central1", maxInstances: 10});

// El payload de datos de FCM admite 4 KB, así que el texto se recorta.
const MAX_TEXT_LENGTH = 200;
const IMAGE_TEXT = "📷 Imagen";

// Errores que indican que el token ya no sirve y se puede borrar del perfil.
const INVALID_TOKEN_ERRORS = new Set([
  "messaging/registration-token-not-registered",
  "messaging/invalid-registration-token",
]);

exports.notifyNewMessage = onDocumentCreated(
    "chats/{chatId}/messages/{messageId}",
    async (event) => {
      const message = event.data && event.data.data();
      if (!message || !message.senderId) {
        return;
      }

      const {chatId} = event.params;
      const senderId = message.senderId;
      const db = getFirestore();

      // La app escribe el chat en la misma transacción que el mensaje, así que ya existe.
      const [chatSnapshot, senderSnapshot] = await Promise.all([
        db.collection("chats").doc(chatId).get(),
        db.collection("users").doc(senderId).get(),
      ]);

      const participants = chatSnapshot.get("participants") || [];
      const recipients = participants.filter((uid) => uid && uid !== senderId);

      if (recipients.length === 0) {
        return;
      }

      const payload = {
        chatId,
        senderId,
        senderName: senderSnapshot.get("name") || "",
        text: messagePreview(message),
      };

      await Promise.all(recipients.map((uid) => sendToUser(db, uid, payload)));
    },
);

/** Texto que se muestra en la notificación; las imágenes base64 nunca viajan en el push. */
function messagePreview(message) {
  if (message.type === "image") {
    return IMAGE_TEXT;
  }

  // Array.from separa por caracteres, así no se corta un emoji por la mitad.
  const characters = Array.from(typeof message.text === "string" ? message.text : "");

  return characters.length > MAX_TEXT_LENGTH ?
    characters.slice(0, MAX_TEXT_LENGTH - 1).join("") + "…" :
    characters.join("");
}

async function sendToUser(db, uid, payload) {
  const userRef = db.collection("users").doc(uid);
  const token = (await userRef.get()).get("fcmToken");

  if (!token) {
    return;
  }

  try {
    await getMessaging().send({
      token,
      data: {...payload, recipientId: uid},
      android: {
        // Alta prioridad para que llegue con la app en segundo plano o cerrada.
        priority: "high",
        // Si el dispositivo está sin conexión, solo se entrega el último de cada chat.
        collapseKey: payload.chatId,
      },
    });
  } catch (error) {
    if (INVALID_TOKEN_ERRORS.has(error.code)) {
      await removeToken(db, userRef, token);
    } else {
      logger.error("No se pudo enviar el push", {
        uid,
        chatId: payload.chatId,
        code: error.code,
        message: error.message,
      });
    }
  }
}

/** Borra un token caducado, solo si el usuario no ha registrado ya otro. */
async function removeToken(db, userRef, token) {
  await db.runTransaction(async (transaction) => {
    const snapshot = await transaction.get(userRef);

    if (snapshot.get("fcmToken") === token) {
      transaction.update(userRef, {fcmToken: FieldValue.delete()});
    }
  });

  logger.info("Token FCM caducado eliminado", {uid: userRef.id});
}

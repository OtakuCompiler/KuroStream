import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();

const db = admin.firestore();
const auth = admin.auth();

// ── User Management ──────────────────────────────────────────────────────

export const onUserCreated = functions.auth.user().onCreate(async (user) => {
  const userRef = db.collection("users").doc(user.uid);
  await userRef.set({
    id: user.uid,
    email: user.email,
    display_name: user.displayName || null,
    photo_url: user.photoURL || null,
    created_at: admin.firestore.FieldValue.serverTimestamp(),
    updated_at: admin.firestore.FieldValue.serverTimestamp(),
  });

  await userRef.collection("sync").doc("meta").set({
    last_sync_at: null,
    devices: [],
  });

  return { success: true };
});

export const onUserDeleted = functions.auth.user().onDelete(async (user) => {
  const batch = db.batch();
  const userRef = db.collection("users").doc(user.uid);

  batch.delete(userRef);

  const subcollections = ["sync", "favorites", "watchHistory", "settings", "profiles"];
  for (const col of subcollections) {
    const snapshot = await userRef.collection(col).get();
    snapshot.forEach((doc) => batch.delete(doc.ref));
  }

  await batch.commit();
  return { success: true };
});

// ── FCM Notifications ────────────────────────────────────────────────────

export const sendFCMNotification = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "User must be authenticated");
  }

  const { userId, title, body, data: payload } = data;

  if (userId !== context.auth.uid) {
    throw new functions.https.HttpsError("permission-denied", "Cannot send notifications for other users");
  }

  const userDoc = await db.collection("users").doc(userId).get();
  const fcmToken = userDoc.data()?.fcm_token;

  if (!fcmToken) {
    return { success: false, reason: "no_fcm_token" };
  }

  const message = {
    notification: { title, body },
    data: payload || {},
    token: fcmToken,
  };

  try {
    const response = await admin.messaging().send(message);
    return { success: true, messageId: response };
  } catch (error) {
    const msg = error instanceof Error ? error.message : String(error);
    console.error("FCM send error:", error);
    return { success: false, error: msg };
  }
});

export const onNewEpisode = functions.firestore
  .document("shows/{showId}/episodes/{episodeId}")
  .onCreate(async (snap, context) => {
    const episode = snap.data();
    const showId = context.params.showId;

    const showDoc = await db.collection("shows").doc(showId).get();
    const show = showDoc.data();

    if (!show?.title || !show?.subscribers) return null;

    const batch = db.batch();
    for (const subscriberId of show.subscribers) {
      const tokenDoc = await db.collection("users").doc(subscriberId).get();
      const token = tokenDoc.data()?.fcm_token;
      if (token) {
        batch.set(db.collection("notifications").doc(), {
          user_id: subscriberId,
          title: `New episode: ${show.title}`,
          body: episode.title || "A new episode is now available",
          data: { showId, episodeId: context.params.episodeId },
          created_at: admin.firestore.FieldValue.serverTimestamp(),
          read: false,
        });
      }
    }

    await batch.commit();
    return null;
  });

// ── Watch Progress Sync ──────────────────────────────────────────────────

export const syncWatchProgress = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "User must be authenticated");
  }

  const { mediaId, episodeId, progressMs, durationMs, completed } = data;
  const userId = context.auth.uid;

  const progressRef = db
    .collection("users")
    .doc(userId)
    .collection("watchHistory")
    .doc(`${mediaId}_${episodeId || "movie"}`);

  await progressRef.set({
    media_id: mediaId,
    episode_id: episodeId,
    progress_ms: progressMs,
    duration_ms: durationMs,
    completed: completed || false,
    updated_at: admin.firestore.FieldValue.serverTimestamp(),
  });

  return { success: true };
});

// ── App Check Verification ───────────────────────────────────────────────

export const verifyAppCheck = functions.https.onCall(async (data, context) => {
  if (!context.app) {
    throw new functions.https.HttpsError("unauthenticated", "App Check token required");
  }

  const appCheckToken = data.token;
  if (!appCheckToken) {
    throw new functions.https.HttpsError("invalid-argument", "App Check token missing");
  }

  try {
    const decodedToken = await admin.appCheck().verifyToken(appCheckToken);
    return { valid: true, appId: decodedToken.appId, token: decodedToken.token };
  } catch (error) {
    console.error("App Check verification failed:", error);
    throw new functions.https.HttpsError("invalid-argument", "Invalid App Check token");
  }
});

// ── Health Check ─────────────────────────────────────────────────────────

export const healthCheck = functions.https.onRequest(async (_req, res) => {
  res.set("Cache-Control", "no-store");
  res.set("Content-Type", "application/json");
  res.send(JSON.stringify({
    status: "ok",
    timestamp: new Date().toISOString(),
    project: "kurostream13",
    region: process.env.FUNCTION_REGION || "unknown",
  }));
});

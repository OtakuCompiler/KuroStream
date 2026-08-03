const express = require('express');
const {GoogleAuth} = require('google-auth-library');
const axios = require('axios');
const admin = require('firebase-admin');

const app = express();
app.use(express.json());

// Initialize Firebase Admin
if (!admin.apps.length) {
    admin.initializeApp({
        credential: admin.credential.applicationDefault(),
        projectId: process.env.FIREBASE_PROJECT_ID || 'kurostream-app',
    });
}

const PROJECT_NUMBER = process.env.GOOGLE_CLOUD_PROJECT_NUMBER || 'CHANGE_ME_PROJECT_NUMBER';

/**
 * Verify Play Integrity token
 * POST /verify-integrity
 * Body: { token: string, nonce: string }
 */
app.post('/verify-integrity', async (req, res) => {
    const { token, nonce } = req.body;
    if (!token || !nonce) {
        return res.status(400).json({ error: 'Missing token or nonce' });
    }

    try {
        const auth = new GoogleAuth();
        const client = await auth.getClient();
        const accessToken = await client.fetchAccessToken();

        const response = await axios.post(
            `https://playintegrity.googleapis.com/v1/${PROJECT_NUMBER}:decodeIntegrityToken`,
            { integrity_token: token },
            { headers: { Authorization: `Bearer ${accessToken.token}` } }
        );

        const result = response.data;
        
        // Verify nonce matches
        if (result.tokenPayloadExternal.requestDetails.nonce !== nonce) {
            return res.status(400).json({ error: 'Nonce mismatch' });
        }

        // Check device integrity
        const deviceIntegrity = result.tokenPayloadExternal.deviceIntegrity;
        const appIntegrity = result.tokenPayloadExternal.appIntegrity;
        
        const isValid = 
            deviceIntegrity.deviceRecognitionVerdict.includes('MEETS_DEVICE_INTEGRITY') &&
            appIntegrity.appRecognitionVerdict === 'PLAY_RECOGNIZED' &&
            appIntegrity.packageName === 'com.kurostream.app';

        res.json({
            valid: isValid,
            deviceRecognition: deviceIntegrity.deviceRecognitionVerdict,
            appRecognition: appIntegrity.appRecognitionVerdict,
            packageName: appIntegrity.packageName,
            accountDetails: result.tokenPayloadExternal.accountDetails,
        });
    } catch (error) {
        console.error('Verification failed:', error);
        res.status(500).json({ error: 'Verification failed', details: error.message });
    }
});

/**
 * Send FCM notification
 * POST /send-notification
 * Body: { token: string, title: string, body: string, data: object }
 */
app.post('/send-notification', async (req, res) => {
    const { token, title, body, data } = req.body;
    if (!token || !title) {
        return res.status(400).json({ error: 'Missing token or title' });
    }

    try {
        const message = {
            token,
            notification: { title, body },
            data: data || {},
            android: {
                priority: 'high',
                notification: {
                    channelId: 'new_episode',
                    icon: 'ic_notification',
                },
            },
        };

        const response = await admin.messaging().send(message);
        res.json({ success: true, messageId: response });
    } catch (error) {
        console.error('FCM send failed:', error);
        res.status(500).json({ error: 'Failed to send notification', details: error.message });
    }
});

/**
 * Send new episode notification to subscribed users
 * POST /notify-new-episode
 * Body: { showTitle: string, episodeTitle: string, episodeNumber: string, mediaId: string, topic: string }
 */
app.post('/notify-new-episode', async (req, res) => {
    const { showTitle, episodeTitle, episodeNumber, mediaId, topic } = req.body;
    if (!showTitle || !topic) {
        return res.status(400).json({ error: 'Missing required fields' });
    }

    try {
        const message = {
            topic,
            data: {
                type: 'NEW_EPISODE',
                show_title: showTitle,
                episode_title: episodeTitle,
                episode_number: episodeNumber,
                media_id: mediaId,
            },
            android: {
                priority: 'high',
                notification: {
                    channelId: 'new_episode',
                    icon: 'ic_notification',
                    color: '#1E90FF',
                },
            },
            apns: {
                payload: {
                    aps: {
                        alert: {
                            title: `New Episode: ${showTitle}`,
                            body: `${episodeNumber} - ${episodeTitle} is now available`,
                        },
                        badge: 1,
                    },
                },
            },
        };

        const response = await admin.messaging().send(message);
        res.json({ success: true, messageId: response });
    } catch (error) {
        console.error('Episode notification failed:', error);
        res.status(500).json({ error: 'Failed to send notification', details: error.message });
    }
});

/**
 * Subscribe user to show topic
 * POST /subscribe
 * Body: { token: string, topic: string }
 */
app.post('/subscribe', async (req, res) => {
    const { token, topic } = req.body;
    if (!token || !topic) {
        return res.status(400).json({ error: 'Missing token or topic' });
    }

    try {
        await admin.messaging().subscribeToTopic(token, topic);
        res.json({ success: true, message: `Subscribed to ${topic}` });
    } catch (error) {
        console.error('Subscribe failed:', error);
        res.status(500).json({ error: 'Failed to subscribe', details: error.message });
    }
});

/**
 * Unsubscribe user from show topic
 * POST /unsubscribe
 * Body: { token: string, topic: string }
 */
app.post('/unsubscribe', async (req, res) => {
    const { token, topic } = req.body;
    if (!token || !topic) {
        return res.status(400).json({ error: 'Missing token or topic' });
    }

    try {
        await admin.messaging().unsubscribeFromTopic(token, topic);
        res.json({ success: true, message: `Unsubscribed from ${topic}` });
    } catch (error) {
        console.error('Unsubscribe failed:', error);
        res.status(500).json({ error: 'Failed to unsubscribe', details: error.message });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`KuroStream backend server running on port ${PORT}`);
});

module.exports = app;
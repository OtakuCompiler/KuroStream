const express = require('express');
const {GoogleAuth} = require('google-auth-library');
const axios = require('axios');

const app = express();
app.use(express.json());

const PROJECT_NUMBER = 'YOUR_PROJECT_NUMBER';
const PROJECT_ID = 'YOUR_PROJECT_ID';

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
        });
    } catch (error) {
        console.error('Verification failed:', error);
        res.status(500).json({ error: 'Verification failed' });
    }
});

exports.verifyIntegrity = app;
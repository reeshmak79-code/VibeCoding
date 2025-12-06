# Webhook Setup - Next Steps

## ✅ You've Set Up ngrok

Your webhook URL: `https://philanthropically-finical-david.ngrok-free.dev/api/signatures/webhook`

## Next Steps:

### 1. Configure in PandaDoc Dashboard

1. Go to your PandaDoc dashboard
2. Navigate to **Settings** → **Webhooks** (or **Developer** → **Webhooks`)
3. Add/Edit your webhook with:
   - **Webhook Name**: `TrialSite_SignatureWebhook` (or any name you prefer)
   - **Webhook Endpoint URL**: `https://philanthropically-finical-david.ngrok-free.dev/api/signatures/webhook`
   - **Subscribe to Events**: Select these events:
     - ✅ `document.viewed`
     - ✅ `document.completed` (this is when document is signed)
     - ✅ `document.declined`
     - ✅ `document.expired`

4. Save the webhook
5. Copy the **Webhook Secret** (if provided) and add it to `application.properties`:
   ```properties
   pandadoc.webhook.secret=your_webhook_secret_from_pandadoc
   ```

### 2. Keep ngrok Running

**IMPORTANT**: Your ngrok tunnel must stay running for the webhook to work!

- Keep the terminal/command prompt with ngrok running
- If you close it, you'll get a new URL and need to update PandaDoc
- For production, you'll use your actual server URL instead

### 3. Test the Webhook

1. **Start your backend server** (if not already running):
   ```powershell
   cd C:\Users\reesh\VibeCoding\trialsite-app\backend
   .\mvnw.cmd spring-boot:run
   ```

2. **Test the flow**:
   - Log in as Admin/Doctor
   - Go to Documents page
   - Click "Request Signature" on a document
   - Assign it to a user
   - User logs in and goes to Signatures page
   - User clicks "Sign Now" and signs in PandaDoc
   - **The webhook should automatically update the status to "SIGNED"**

### 4. Verify Webhook is Working

- After signing, check the Signatures page - status should update automatically
- Check backend logs for webhook requests
- In PandaDoc dashboard, you can see webhook delivery status

## Troubleshooting

### Webhook not updating status?
1. Check ngrok is still running
2. Check backend server is running
3. Check PandaDoc webhook logs (in PandaDoc dashboard)
4. Check backend console for webhook requests/errors

### ngrok URL changed?
- If you restart ngrok, you'll get a new URL
- Update the webhook URL in PandaDoc dashboard with the new ngrok URL

### For Production:
When you deploy, replace ngrok URL with your production URL:
```
https://your-production-domain.com/api/signatures/webhook
```

## Current Status

✅ ngrok tunnel: `https://philanthropically-finical-david.ngrok-free.dev`
✅ Webhook endpoint: `/api/signatures/webhook`
⏳ Next: Configure in PandaDoc dashboard

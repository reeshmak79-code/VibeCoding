# PandaDoc Integration Setup Guide

## Overview
This document explains how to set up and configure PandaDoc document signing integration.

## Prerequisites

1. **PandaDoc Account**: You need a PandaDoc account with API access enabled
2. **API Key**: Generate an API key from your PandaDoc account settings

## Setup Steps

### Step 1: Get PandaDoc API Key

1. Log into your PandaDoc account
2. Go to **Settings** → **API** (or **Developer** section)
3. Generate a new API key
4. Copy the API key (you'll need it for configuration)

### Step 2: Configure Backend

1. Open `backend/src/main/resources/application.properties`
2. Update the following properties:
   ```properties
   pandadoc.api.key=YOUR_ACTUAL_API_KEY_HERE
   pandadoc.api.url=https://api.pandadoc.com/public/v1
   pandadoc.webhook.secret=YOUR_WEBHOOK_SECRET_HERE
   ```

### Step 3: Configure Webhook (Optional but Recommended)

1. In PandaDoc dashboard, go to **Settings** → **Webhooks**
2. Add a new webhook with URL: `http://your-backend-url/api/signatures/webhook`
3. Select events to listen for:
   - `document.viewed`
   - `document.completed`
   - `document.declined`
   - `document.expired`
4. Copy the webhook secret and add it to `application.properties`

### Step 4: Test the Integration

1. Start the backend server
2. Log in as Admin or Doctor
3. Go to Documents page
4. Click "Request Signature" on any document
5. Select a user and send for signing
6. The user should receive an email from PandaDoc (if configured)
7. User can sign via the Signatures page

## API Endpoints

### Backend Endpoints:
- `POST /api/signatures/assign` - Assign document for signing (Admin/Doctor only)
- `GET /api/signatures/pending` - Get user's pending signatures
- `GET /api/signatures/{id}/sign-url` - Get signing URL
- `GET /api/signatures/document/{documentId}` - Get all signatures for a document (Admin/Doctor)
- `POST /api/signatures/webhook` - Webhook endpoint for PandaDoc status updates

## Workflow

1. **Admin assigns signature:**
   - Admin clicks "Request Signature" on a document
   - Selects user to sign
   - System uploads document to PandaDoc
   - PandaDoc sends email to user (if configured)
   - Status: `SENT`

2. **User signs:**
   - User logs in and goes to "Signatures" page
   - Clicks "Sign Now" → Opens PandaDoc signing page
   - User signs the document
   - PandaDoc webhook updates status to `SIGNED`

3. **Status tracking:**
   - Admin can see signature status on documents
   - User can see their pending/completed signatures
   - Real-time updates via webhook

## Status Values

- `PENDING` - Created but not sent
- `SENT` - Sent to PandaDoc, waiting for recipient
- `VIEWED` - Recipient has viewed the document
- `SIGNED` - Document has been signed
- `DECLINED` - Recipient declined to sign
- `EXPIRED` - Document signing expired
- `CANCELLED` - Admin cancelled the request

## Troubleshooting

### Common Issues:

1. **"Failed to upload document to PandaDoc"**
   - Check API key is correct
   - Verify file exists and is readable
   - Check PandaDoc API status

2. **"Signing URL not available"**
   - Document may not have been sent to PandaDoc successfully
   - Check PandaDoc dashboard for document status

3. **Webhook not updating status**
   - Verify webhook URL is accessible from internet
   - Check webhook secret matches
   - Verify webhook events are enabled in PandaDoc

## Notes

- The PandaDocService may need adjustments based on PandaDoc's actual API structure
- Some API endpoints might differ - refer to PandaDoc API documentation
- For production, use HTTPS for webhook URL
- Consider rate limiting for API calls

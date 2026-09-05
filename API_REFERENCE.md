# NEXUS v3.0 API Reference

Base URL: `/api`

## Authentication
- `POST /auth/register` — register
- `POST /auth/login` — login and receive JWT
- `GET /auth/me` — current user

## Social integrations
- `GET /social/providers` — provider configuration/capability status
- `GET /social/connections` — connected accounts
- `GET /social/status` — provider + connection details
- `GET /social/connect/{platform}` — create OAuth authorization URL
- `GET /social/callback/{platform}` — OAuth callback
- `DELETE /social/connections/{platform}` — disconnect
- `POST /social/publish` — unified publishing for X/Facebook/Instagram
- `POST /social/x/post` — X post
- `POST /social/meta/facebook/post` — Facebook Page post
- `POST /social/instagram/container` — create Instagram media container
- `POST /social/instagram/publish` — publish Instagram container
- `POST /social/whatsapp/send` — WhatsApp Cloud text message
- `GET /social/webhook` — Meta webhook verification
- `POST /social/webhook` — Meta webhook ingestion; events are pushed to `/topic/social/{userId}`

## NEXUS real-time
- SockJS endpoint: `/ws`
- STOMP conversation topic: `/topic/messages/{platform}/{conversationId}`
- STOMP external social event topic: `/topic/social/{userId}`

## Workspace APIs
- `GET/POST /posts`, post like/save/share/delete
- `GET/POST /comments`
- `GET/POST/DELETE /messages`
- `/contacts` CRUD
- `/notifications` + read endpoints
- `/follows/{userId}` follow/unfollow
- `/search?q=...`
- `/analytics`
- `/ai/chat`
- `/media/upload` and public media files
- `/health`

## Provider reality
These endpoints use official provider APIs; NEXUS does not emulate or scrape the platforms. Provider credentials, OAuth scopes, account eligibility, app review, messaging policies and rate limits remain controlled by Meta/X.

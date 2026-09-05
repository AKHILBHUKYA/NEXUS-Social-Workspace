# NEXUS — Social Intelligence Workspace

Production-oriented unified social workspace combining WhatsApp-style messaging, Instagram/Facebook/X feeds, Reels, AI Agent, Contacts, Analytics and Settings.

**This is a local NEXUS workspace** backed by its own database. It does **not** post to real WhatsApp, Instagram, Facebook or X accounts. Official platform integrations can be added later via provider adapters when you have OAuth credentials.

## Architecture

```
Frontend (React 18 + Vite)  →  Vercel
Backend  (Spring Boot 3 + Java 17/21)  →  Render
Database (PostgreSQL)  →  Neon or Supabase
AI (OpenAI-compatible)  →  Groq (or any compatible endpoint)
```

## Features

- JWT authentication (register / login / me)
- Ownership-based authorization
- Posts: create, like/unlike, comment, share, save/bookmark, delete
- WhatsApp-style messaging with REST + WebSocket/STOMP foundation
- Contacts, notifications, global search, analytics
- AI Agent (Groq-compatible) with graceful local fallback
- Flyway migrations, PostgreSQL production, H2 for local/demo
- CORS locked to `FRONTEND_URL`
- Health check: `GET /api/health`
- Standardized error responses

## Tech stack

| Layer    | Stack                                      |
|----------|--------------------------------------------|
| Frontend | React 18, Vite, lucide-react, SockJS/STOMP |
| Backend  | Spring Boot 3.3, Spring Security, JPA, Flyway, WebSocket |
| DB       | PostgreSQL (prod), H2 (local default)      |
| Auth     | JWT + BCrypt                               |
| AI       | OpenAI-compatible (default: Groq)          |

## Local setup

### Prerequisites

- JDK 17 or 21
- Maven 3.8+
- Node.js 18+
- (Optional) PostgreSQL

### Backend

```bash
cd backend
cp .env.example .env   # edit if using real Postgres / AI key
# Default uses in-memory H2 — no DB install required for demo
mvn spring-boot:run
```

Health: http://localhost:8080/api/health

Demo user (seeded): `demo` / `demo12345`

### Frontend

```bash
cd frontend
cp .env.example .env
# VITE_API_URL=http://localhost:8080/api
npm install
npm run dev
```

Open http://localhost:5173

### With Docker (optional local)

```bash
docker compose up --build
```

## Environment variables

### Backend

| Variable       | Description                          | Example |
|----------------|--------------------------------------|---------|
| `PORT`         | Server port                          | `8080` |
| `DB_URL`       | JDBC URL                             | `jdbc:postgresql://...` |
| `DB_USERNAME`  | DB user                              | |
| `DB_PASSWORD`  | DB password                          | |
| `DB_DRIVER`    | JDBC driver                          | `org.postgresql.Driver` |
| `JWT_SECRET`   | Long random secret (≥32 chars)       | |
| `FRONTEND_URL` | Allowed CORS origin                  | `https://your-app.vercel.app` |
| `AI_BASE_URL`  | OpenAI-compatible base               | `https://api.groq.com/openai/v1` |
| `AI_API_KEY`   | Provider API key (server only)       | |
| `AI_MODEL`     | Model name                           | `llama-3.3-70b-versatile` |

### Frontend

| Variable        | Description        |
|-----------------|--------------------|
| `VITE_API_URL`  | Backend API base   | e.g. `https://nexus-backend.onrender.com/api`

## Deployment

See **[DEPLOYMENT.md](./DEPLOYMENT.md)** for step-by-step:

1. Neon/Supabase PostgreSQL
2. Render backend
3. Vercel frontend
4. Groq AI key

## API overview

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /api/health | No | Health + DB status |
| POST | /api/auth/register | No | Register |
| POST | /api/auth/login | No | Login |
| GET | /api/auth/me | Yes | Current user |
| GET/POST | /api/posts | Yes | List / create posts |
| POST/DELETE | /api/posts/{id}/like | Yes | Like / unlike |
| POST/DELETE | /api/posts/{id}/save | Yes | Save / unsave |
| POST | /api/posts/{id}/share | Yes | Share |
| GET/POST | /api/comments/post/{id} | Yes | Comments |
| GET/POST | /api/messages | Yes | Messages |
| GET/POST/DELETE | /api/contacts | Yes | Contacts |
| GET | /api/analytics | Yes | Analytics |
| POST | /api/ai/chat | Yes | AI chat |
| GET | /api/search?q= | Yes | Global search |
| GET/PATCH | /api/notifications | Yes | Notifications |
| POST/DELETE | /api/follows/{userId} | Yes | Follow / unfollow |

## Testing

```bash
cd backend
mvn test
```

Frontend tests can be added with Vitest (not bundled in this baseline).

## Security notes

- Never commit `.env` or real secrets
- JWT secret must be strong in production
- AI keys stay server-side only
- CORS is restricted to `FRONTEND_URL`
- Passwords are BCrypt-hashed
- Users can only delete their own content

## Known limitations

- Media upload uses URL fields; wire Supabase Storage (or S3) when credentials are available
- Camera access is browser `getUserMedia` — implement in UI when needed
- WebSocket works when backend is awake; Render free tier sleeps — REST remains usable
- Search is intentionally lightweight for free-tier memory
- Official WhatsApp/Instagram/Facebook/X APIs are **not** connected (by design)

## License

MIT — portfolio / educational use.

## Real social integrations
NEXUS includes a Connections screen and server-side OAuth/API adapters for Facebook, Instagram, WhatsApp Cloud API and X API v2. Configure the provider credentials in backend environment variables and register the callback URLs before connecting accounts.

## NEXUS v3.0 — unified real-time social workspace

Included in this final package:
- JWT authentication and protected API
- Unified social connections for WhatsApp Cloud API, Facebook Pages, Instagram Professional accounts and X API v2
- OAuth connect/disconnect/status flows
- Unified publishing endpoint plus platform-specific publishing endpoints
- Instagram image-container publishing flow
- WhatsApp Cloud text messaging endpoint
- Meta webhook verification/ingestion and WebSocket fan-out for real-time events
- STOMP/SockJS real-time messaging for NEXUS conversations
- Posts, comments, likes, saves, shares, follows, contacts, notifications and search
- Analytics dashboard API
- AI assistant API (OpenAI-compatible/Groq configuration)
- Media upload API with configurable public storage URL
- PostgreSQL + Flyway migrations, H2 local fallback, Docker/Render deployment files
- Rate limiting, validation, health/actuator endpoint and CORS configuration

### Important: what makes an external platform "real"
The code calls the official provider APIs; it does not simulate WhatsApp/Facebook/Instagram/X. Real external actions require credentials, account eligibility, scopes/permissions and any provider review required by Meta/X. Configure those secrets in Render (never commit them). Webhooks must be configured at the provider dashboard and point to `https://YOUR_BACKEND/api/social/webhook`.

### Local start
Backend: `cd backend && mvn spring-boot:run`
Frontend: `cd frontend && npm.cmd install && npm.cmd run dev`

### Production
Set `BACKEND_URL`, `FRONTEND_URL`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, provider credentials, `SOCIAL_WHATSAPP_PHONE_NUMBER_ID`, and `META_WEBHOOK_VERIFY_TOKEN`.

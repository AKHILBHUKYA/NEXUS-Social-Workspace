# NEXUS Deployment Guide

Deploy the full stack free-tier friendly:

- Frontend → **Vercel**
- Backend → **Render**
- Database → **Neon** or **Supabase** PostgreSQL
- AI → **Groq** (OpenAI-compatible)

## A. Create GitHub repository

1. Create a new empty repo on GitHub.
2. From the project root:

```bash
git init
git add .
git commit -m "NEXUS Social Intelligence Workspace v2"
git branch -M main
git remote add origin https://github.com/YOUR_USER/nexus-social-workspace.git
git push -u origin main
```

## B. Create PostgreSQL (Neon example)

1. Go to https://neon.tech and create a project.
2. Copy the connection string. Convert to JDBC:

```
jdbc:postgresql://ep-xxxx.region.aws.neon.tech/neondb?sslmode=require
```

3. Note username and password from the Neon dashboard.

**Supabase alternative:** Project Settings → Database → Connection string (URI), same JDBC conversion with `?sslmode=require`.

## C. Create Render backend

1. https://render.com → New → Web Service
2. Connect your GitHub repo
3. Root directory: `backend` (or leave blank if backend is root of deploy)
4. Runtime: Java
5. Build command:

```
mvn clean package -DskipTests
```

6. Start command:

```
java -jar target/nexus-social-workspace-2.0.0.jar
```

7. Health check path: `/api/health`

### Environment variables on Render

| Key | Value |
|-----|-------|
| `DB_URL` | `jdbc:postgresql://...` (from Neon) |
| `DB_USERNAME` | neon user |
| `DB_PASSWORD` | neon password |
| `DB_DRIVER` | `org.postgresql.Driver` |
| `JWT_SECRET` | long random string (32+ chars) |
| `FRONTEND_URL` | `https://YOUR_APP.vercel.app` (set after Vercel) |
| `AI_PROVIDER` | `groq` |
| `AI_BASE_URL` | `https://api.groq.com/openai/v1` |
| `AI_API_KEY` | your Groq key |
| `AI_MODEL` | `llama-3.3-70b-versatile` |
| `DDL_AUTO` | `validate` |

8. Deploy. Wait for health to pass.

Test:

```bash
curl https://YOUR-SERVICE.onrender.com/api/health
```

## D. Deploy frontend to Vercel

1. https://vercel.com → Import Git repo
2. Root directory: `frontend`
3. Framework: Vite
4. Environment variable:

```
VITE_API_URL=https://YOUR-SERVICE.onrender.com/api
```

5. Deploy.

## E. Finalize CORS

1. Set `FRONTEND_URL=https://your-vercel-app.vercel.app` on Render
2. Redeploy backend (or trigger env-only restart)

## F. Groq AI setup

1. https://console.groq.com → API Keys → Create
2. Put key in Render `AI_API_KEY`
3. Confirm model is currently supported (e.g. `llama-3.3-70b-versatile`)

Without a key, AI returns a clear local fallback response — the rest of the app still works.

## G. Smoke test checklist

1. Open Vercel URL
2. Register a new user
3. Login
4. Open WhatsApp → send a message → refresh → message persists
5. Open Instagram → create post → like → comment → save → share
6. Open AI Agent → ask for summary
7. Open Analytics → verify counts increase
8. Search
9. Logout → protected pages require login again
10. `/api/health` returns UP

## Troubleshooting

| Issue | Fix |
|-------|-----|
| CORS errors | Ensure `FRONTEND_URL` matches exact Vercel origin (no trailing slash) |
| 401 on all APIs | Token missing/expired — login again |
| DB connection failed | Check JDBC URL has `sslmode=require` for Neon/Supabase |
| Render sleep | Free tier spins down; first request may take 30–60s |
| AI fallback only | Set valid `AI_API_KEY` and supported `AI_MODEL` |

## Local vs Production

| | Local | Production |
|--|-------|------------|
| DB | H2 in-memory (default) | Neon/Supabase PostgreSQL |
| Frontend URL | http://localhost:5173 | Vercel URL |
| AI | Optional | Groq key recommended |

Never commit real secrets. Use provider dashboards and CI secrets only.

## Official social API integrations (NEXUS v2.1)

The project now includes an official-API integration layer under `/api/social` for Meta (Facebook/Instagram/WhatsApp Cloud API) and X API v2. It never puts provider secrets in the browser.

### Required environment variables

Set these on Render for the backend:

- `BACKEND_URL=https://YOUR-RENDER-SERVICE.onrender.com`
- `FRONTEND_URL=https://YOUR-VERCEL-APP.vercel.app`
- `SOCIAL_META_CLIENT_ID=` Meta developer app ID
- `SOCIAL_META_CLIENT_SECRET=` Meta developer app secret
- `SOCIAL_META_GRAPH_VERSION=v23.0` (change if your approved app uses another current Graph API version)
- `SOCIAL_WHATSAPP_PHONE_NUMBER_ID=` WhatsApp Cloud API phone number ID
- `SOCIAL_X_CLIENT_ID=` X developer app client ID
- `SOCIAL_X_CLIENT_SECRET=` X developer app secret
- `SOCIAL_X_SCOPES=tweet.read tweet.write users.read offline.access`

### OAuth callback URLs

Register these exact callback URLs in the provider developer consoles:

- `https://YOUR-RENDER-SERVICE.onrender.com/api/social/callback/facebook`
- `https://YOUR-RENDER-SERVICE.onrender.com/api/social/callback/instagram`
- `https://YOUR-RENDER-SERVICE.onrender.com/api/social/callback/whatsapp`
- `https://YOUR-RENDER-SERVICE.onrender.com/api/social/callback/x`

Provider permissions, account eligibility, app review, rate limits, and plan restrictions still apply. The code intentionally reports provider errors instead of simulating successful API calls.

## v3.0 production integration checklist

1. Deploy backend to Render and frontend to Vercel.
2. Set `BACKEND_URL` to the public Render URL and `FRONTEND_URL` to the public Vercel URL.
3. Configure Meta app credentials in Render: `SOCIAL_META_CLIENT_ID`, `SOCIAL_META_CLIENT_SECRET`, `SOCIAL_META_GRAPH_VERSION`.
4. Configure X credentials: `SOCIAL_X_CLIENT_ID`, `SOCIAL_X_CLIENT_SECRET`, and the requested `SOCIAL_X_SCOPES`.
5. For WhatsApp Cloud API, set `SOCIAL_WHATSAPP_PHONE_NUMBER_ID`.
6. Set a strong `META_WEBHOOK_VERIFY_TOKEN` and configure the Meta webhook callback as `https://YOUR_BACKEND/api/social/webhook`.
7. In each provider dashboard, use the exact callback URL returned by NEXUS: `/api/social/callback/{platform}`.
8. Complete any provider-specific app review/business verification/account eligibility required for the permissions you request.
9. Test Connect -> OAuth -> Connections -> Publish/Message -> Webhook -> real-time event.

### Capability boundary
NEXUS v3.0 is a real API client/orchestration layer. It cannot bypass Meta/X platform policies, app review, account restrictions, messaging windows, rate limits, or paid/API-plan requirements. Those are controlled by the providers.

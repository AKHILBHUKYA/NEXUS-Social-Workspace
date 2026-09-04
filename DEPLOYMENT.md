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

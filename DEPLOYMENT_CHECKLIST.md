# NEXUS deployment checklist

## Before deployment
- [ ] Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.
- [ ] Set a strong non-default database password.
- [ ] Set `AI_API_KEY` only on the server, never in React source.
- [ ] Set `AI_BASE_URL` and `AI_MODEL` for your provider.
- [ ] Configure HTTPS/reverse proxy for the deployed domain.
- [ ] Review CORS allowed origins for your domain.
- [ ] Back up MySQL before migrations.

## Docker
`docker compose up --build -d`

Frontend is exposed on port 80. Backend remains internal to the compose network.

## Local
Backend:
`cd backend`
`mvn spring-boot:run`

Frontend:
`cd frontend`
`npm install`
`npm run dev`

Open `http://localhost:5173`.

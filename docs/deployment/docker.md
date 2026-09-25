# Docker Deployment

Run the full stack from the repository root:

```bash
docker compose up --build
```

Services:

- `mysql`: MySQL 8 storage with health check.
- `redis`: token blacklist and analytics cache.
- `backend`: Spring Boot gateway on `8080`.
- `frontend`: Nginx-served React build on `3000`.

Environment variables are read from `.env`; `.env.example` documents local defaults.

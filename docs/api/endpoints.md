# API Endpoints

## Auth

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/auth/me`

## Finance

- `GET /api/transactions`
- `POST /api/transactions`
- `GET /api/transactions/{id}`
- `PUT /api/transactions/{id}`
- `DELETE /api/transactions/{id}`
- `POST /api/transactions/import`
- `GET /api/budgets`
- `POST /api/budgets`
- `GET /api/budgets/alerts`
- `PATCH /api/budgets/alerts/{id}/read`
- `GET /api/analytics/overview`
- `GET /api/goals`
- `POST /api/goals`
- `PUT /api/goals/{id}`
- `GET /api/reports/pdf`
- `GET /api/reports/excel`

## Admin

All admin endpoints require `ROLE_SUPER_ADMIN`.

- `GET /api/admin/users`
- `POST /api/admin/users/{id}/reset-password`
- `PATCH /api/admin/users/{id}/deactivate`
- `GET /api/admin/audit`
- `GET /api/admin/metrics`

## WebSocket

- Endpoint: `/ws`
- Topics: `/topic/users/{userId}/transactions`, `/alerts`, `/goals`, `/imports`

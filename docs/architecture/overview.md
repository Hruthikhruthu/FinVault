# FinVault Architecture

FinVault runs as one deployable Spring Boot gateway assembled from bounded backend modules: common domain/repositories, auth, transactions, budgets, analytics, reports, goals, admin, and WebSocket messaging.

The React dashboard talks to `/api/**` through Axios and subscribes to `/ws` with STOMP/SockJS. MySQL stores the system of record, Redis backs JWT blacklist and analytics cache, and Flyway owns schema creation.

## Runtime Flow

1. Users register or login through `/api/auth/**`.
2. The backend issues RS256 access tokens and rotating refresh tokens.
3. Protected API calls pass through the manual JWT filter.
4. Transaction writes publish Spring events and WebSocket messages.
5. Budget and goal listeners react to transaction events.
6. Analytics reads native SQL projections and caches responses for 30 minutes.
7. Reports stream PDF/XLSX payloads without temp files.

# Database Schema

Flyway migration `V1__init.sql` creates:

- `users`
- `refresh_tokens`
- `audit_log`
- `transactions`
- `import_jobs`
- `budgets`
- `alerts`
- `goals`
- `scheduler_audit`

The schema uses foreign keys, optimistic-locking `version` columns, indexes for query paths, and CHECK constraints for non-negative counters and positive monetary values.

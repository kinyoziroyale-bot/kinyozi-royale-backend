# Kinyozi Royale Backend

JDK 17 · Spring Boot 3.3.4 · PostgreSQL · JWT auth · Multi-tenant.

See **PLAN.md** for the full handover brief (architecture, env vars, endpoints,
how to extend). Quick start:

```bash
createdb kinyozi
psql -d kinyozi -f schema.sql
export JWT_SECRET=please-change-this-to-32-plus-chars-secret
./mvnw spring-boot:run
```

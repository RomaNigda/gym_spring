# Gym Spring

## Database migrations

Start PostgreSQL with `docker compose up -d`. Flyway runs automatically when the
application starts, before Hibernate initializes, and creates the `training_type`
dictionary from `src/main/resources/db/migration`. The database container alone
does not run application migrations.

Existing databases created by the old initialization script are baselined at
version 0, so migration V1 also runs for them without replacing existing training
types. Add new versioned migration files rather than editing applied migrations.
Hibernate still manages the other tables through `hibernate.hbm2ddl.auto=update`.

`Trainee` and `Trainer` inherit from `User` using joined tables and share its
primary key. Common fields are accessed directly, for example
`trainee.getUsername()`. Hibernate's schema update does not convert the old
`user_id` profile tables to this structure; use a fresh database when switching
from the previous mapping.

## PostgreSQL migration tests

The migration tests are skipped unless `GYM_TEST_DB_URL` is set. To run them, use
a disposable PostgreSQL database whose user can create and drop schemas:

```shell
GYM_TEST_DB_URL=jdbc:postgresql://localhost:5433/test_db \
GYM_TEST_DB_USERNAME=test_user \
GYM_TEST_DB_PASSWORD=test_password \
./mvnw -Dtest=FlywayMigrationTest test
```

Each test creates and removes its own isolated schema.

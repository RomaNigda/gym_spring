# Gym Spring (Gym CRM)

A Gym CRM back-end built on **plain Spring Framework 6** (no Spring Boot), **Hibernate 6**,
**PostgreSQL 15** and **Flyway**. It manages trainees, trainers, their trainings and
assignments. There is no HTTP layer yet: `GymSpringApplication` is a console entry point
that boots the Spring context and runs a demo scenario through `GymFacade`
(create trainer and trainee → add a training → assign trainer → toggle activity → delete trainee),
then exits.

## Tech stack

| Area            | Technology                                                     |
|-----------------|----------------------------------------------------------------|
| Language / JDK  | Java 21                                                        |
| DI / Tx         | Spring Context, Spring ORM, Spring Tx 6.2 (Java config)        |
| ORM             | Hibernate 6.6 (`LocalSessionFactoryBean`, `HibernateTransactionManager`) |
| Validation      | Hibernate Validator 8 (Jakarta Bean Validation)                |
| Database        | PostgreSQL 15, HikariCP pool                                   |
| Migrations      | Flyway 11                                                      |
| Build / tests   | Maven, JUnit 5, Mockito, JaCoCo                                |
| Misc            | Lombok, SLF4J (simple)                                         |

## Running

### Everything in Docker (recommended)

```shell
docker compose up --build
```

This starts two services:

- `postgres` (`gym_crm_postgres`) – PostgreSQL 15, exposed on host port `5433`, data stored
  in the `gym_crm_data` volume, with a `pg_isready` health check.
- `app` (`gym_crm_app`) – built from the `Dockerfile`; starts only after `postgres` is healthy,
  connects to it via `jdbc:postgresql://postgres:5432/gym_crm`, runs the demo and exits with code 0.
  PostgreSQL keeps running.

To stop everything when the app finishes:

```shell
docker compose up --build --abort-on-container-exit --exit-code-from app
```

Clean up (add `-v` to also delete the database volume):

```shell
docker compose down
```

The `Dockerfile` is multi-stage: a `maven:3.9-eclipse-temurin-21` stage builds the jar
(tests skipped) and copies runtime dependencies to `lib/`; the final
`eclipse-temurin:21-jre-alpine` image runs
`java -cp app.jar:lib/* io.github.romanigda.gym_spring.GymSpringApplication`.

### Locally (database in Docker, app from IDE/Maven)

```shell
docker compose up -d postgres
./mvnw compile exec:java -Dexec.mainClass=io.github.romanigda.gym_spring.GymSpringApplication
```

or just run `GymSpringApplication.main` from the IDE. The default `DB_URL` points to
`localhost:5433`.

### Configuration

`src/main/resources/application.properties` reads environment variables with defaults:

| Variable      | Default                                     | Used by          |
|---------------|---------------------------------------------|------------------|
| `DB_URL`      | `jdbc:postgresql://localhost:5433/gym_crm`  | app              |
| `DB_USERNAME` | `gym`                                       | app, postgres    |
| `DB_PASSWORD` | `gym`                                       | app, postgres    |
| `DB_NAME`     | `gym_crm`                                   | docker compose   |
| `DB_PORT`     | `5433`                                      | docker compose (host port) |

In Docker Compose the app's `DB_URL` is built from `DB_NAME` and always targets the
`postgres` service on the internal network. Values can be overridden via a `.env` file.

## Architecture

```
GymSpringApplication (main)
        │  AnnotationConfigApplicationContext(AppConfig)
        ▼
GymFacade ──► AuthenticationService
   │
   ├──► TraineeService ─┐
   ├──► TrainerService ─┼──► DAO layer (Hibernate SessionFactory) ──► PostgreSQL
   └──► TrainingService ┘          ▲
                                   │
             UserCredentialsGenerator (username/password generation)
```

### Packages (`io.github.romanigda.gym_spring`)

| Package          | Responsibility |
|------------------|----------------|
| `config`         | `AppConfig` (component scan, `application.properties`, imports `HibernateConfig`); `HibernateConfig` (HikariCP `DataSource`, `Flyway` bean, `SessionFactory`, transaction manager). |
| `facade`         | `GymFacade` – single entry point for all 18 use cases. Every operation except profile creation first checks the username/password. |
| `service`, `service.impl` | Business logic and `@Transactional` boundaries: `TraineeService`, `TrainerService`, `TrainingService`, `AuthenticationService`. |
| `dao`, `dao.impl` | Persistence via Hibernate `Session` and HQL: `UserDao`, `TraineeDao`, `TrainerDao`, `TrainingDao`, `TrainingTypeDao`. |
| `domain`         | JPA entities. |
| `dto`            | `NewTraining`, `TraineeTrainingCriteria`, `TrainerTrainingCriteria` (optional filters; `null` = no filter). |
| `util`           | `UserCredentialsGenerator`, `UsernameGenerator` (`First.Last`, adding a numeric suffix on collision), `PasswordGenerator` (random 10 alphanumeric chars), `ServiceValidation`. |
| `exception`      | `ServiceException` and subclasses: `ValidationException`, `AuthenticationException`, `EntityNotFoundException`, `DuplicateEntityException`. |

### Domain model

```
            users (User)
   id, first_name, last_name, username (unique), password, is_active
          ▲                         ▲
          │ JOINED inheritance      │ (shared primary key `id`)
  trainee (Trainee)            trainer (Trainer)
  date_of_birth, address       specialization ──► training_type
          │      ▲                  ▲     │
          │      └─ trainee_trainer ┘     │   (many-to-many, owned by Trainee)
          │                               │
          └──────────► training ◄─────────┘
                 trainee_id, trainer_id, training_type_id,
                 training_name, training_date, training_duration
```

- `Trainee` and `Trainer` extend `User` with `InheritanceType.JOINED`; common fields are
  accessed directly (e.g. `trainee.getUsername()`).
- `Trainee` ↔ `Trainer`: many-to-many through `trainee_trainer`.
- `Training` references a trainee, a trainer and a `TrainingType`. Deleting a trainee
  cascades to their trainings (`CascadeType.REMOVE`, `orphanRemoval`).
- `TrainingType` is an `@Immutable` dictionary (STRENGTH, CARDIO, FLEXIBILITY, BALANCE,
  YOGA, ZUMBA) that the application never modifies.

## Database schema and migrations

- Flyway runs on startup (`Flyway` bean with `initMethod = "migrate"`), before Hibernate
  initializes (`sessionFactory` is `@DependsOn("flyway")`). Migrations live in
  `src/main/resources/db/migration`; `V1__create_training_types.sql` creates and fills
  `training_type`.
- Existing databases created without Flyway are baselined at version 0, so V1 still runs
  for them without replacing existing training types. Add new versioned migrations instead
  of editing applied ones.
- All other tables are managed by Hibernate (`hibernate.hbm2ddl.auto=update`). Schema
  update does not convert the old `user_id`-based profile tables to the current joined
  inheritance; use a fresh database (`docker compose down -v`) when switching from the old
  mapping.

## Tests

```shell
./mvnw test
```

Unit tests cover services, facade, credential generation, exceptions, Hibernate mapping and
configuration. The PostgreSQL migration tests (`FlywayMigrationTest`) are skipped unless
`GYM_TEST_DB_URL` is set. To run them, use a disposable database whose user can create and
drop schemas:

```shell
GYM_TEST_DB_URL=jdbc:postgresql://localhost:5433/test_db \
GYM_TEST_DB_USERNAME=test_user \
GYM_TEST_DB_PASSWORD=test_password \
./mvnw -Dtest=FlywayMigrationTest test
```

Each test creates and removes its own isolated schema. A JaCoCo coverage report is generated
in `target/site/jacoco` by `./mvnw verify`.

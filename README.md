# E-commerce

Spring Boot + PostgreSQL COD e-commerce (MVP). See the delivery plan under
[`docs/product-activities/phase-1-implementation`](docs/product-activities/phase-1-implementation/README.md).

## Prerequisites

Run from the project root; all should succeed:

```bash
java -version            # any JDK on PATH
./gradlew --version      # Gradle 9.5.1
docker version
docker compose version
```

A **JDK 25** must be installed: Gradle uses it as the build toolchain, and
`run.sh` uses it to run the Java 25 jar. The `java` on your `PATH` may be older —
`run.sh` auto-detects a ≥ 25 runtime, or set `JAVA_HOME`.

## Configuration model

Config is loaded in two layers:

| Layer | File | Contents | In the jar? |
|---|---|---|---|
| Base | `src/main/resources/application.yaml` | Non-secret defaults (JPA, Liquibase, actuator) | Yes |
| Environment | `environment/application.yaml` | Datasource URL + credentials | No |

At runtime the environment file is merged over the base via Spring's
`spring.config.additional-location` (external values override matching keys).
Edit `environment/application.yaml` directly to point at your local database.

## Local development

Two separate things run locally, and you need both:

- **The database** — PostgreSQL, started with `docker compose` (step 1).
- **The application** — Spring Boot, started with `./scripts/run.sh` (step 2),
  which connects to that database.

`run.sh` does **not** start a database; if no PostgreSQL is listening on
`localhost:5432`, the app fails at boot with `Connection refused`. (If you run
PostgreSQL another way — e.g. a native install — you can skip step 1, but then
you must create the `ecommerce` schema yourself.)

All commands run from the project root.

### 1. Start PostgreSQL

```bash
docker compose -f docker/docker-compose.yaml up -d
docker compose -f docker/docker-compose.yaml ps
```

Expected: the `ecommerce-postgres` container reaches `healthy`. On the **first**
start (empty volume) `docker/initdb/01-create-schema.sql` runs automatically and
creates the `ecommerce` schema the app connects to (`?currentSchema=ecommerce`).

Data lives in the Docker-managed named volume `ecommerce-postgres`, independent
of the container lifecycle — there is no host folder to create or clean up.

### 2. Run the application

```bash
./scripts/run.sh
```

The script builds the bootable jar (incremental `bootJar`) and runs it, pointing
Spring at `environment/` for the datasource. The app listens on port `8080`.

**Remote debug (JDWP)** is always enabled on `127.0.0.1:8081` — attach your IDE
there (IntelliJ: *Remote JVM Debug*). It does not pause startup; set
`DEBUG_SUSPEND=y` to wait for the debugger, or `DEBUG_PORT=…` to change the port.

The jar is compiled for **Java 25**, so the script runs it with a Java ≥ 25
runtime (not necessarily the `java` on your `PATH`) — override with `JAVA_HOME`
if needed.

### 3. Check the runtime

From another terminal:

```bash
curl --fail --silent http://localhost:8080/actuator/health
curl --fail --silent http://localhost:8080/actuator/health/liveness
curl --fail --silent http://localhost:8080/actuator/health/readiness
```

Expected: all exit `0`; overall health is `{"status":"UP"}`. Readiness includes
the database, so it drops from `UP` if PostgreSQL becomes unavailable. Component
details (JDBC URL, etc.) are never exposed on the public endpoint.

### 4. Stop

```bash
docker compose -f docker/docker-compose.yaml down       # keeps data
docker compose -f docker/docker-compose.yaml down -v    # ALSO wipes the volume
```

`down` keeps the named volume, so your data (and the `ecommerce` schema) survive
a restart. Use `down -v` only when you want a clean slate — for example to
re-run the init script, which executes only against an empty volume.

## Build

```bash
./gradlew build     # compile, run checks, and package the bootable jar
```

CI runs the same gate from a clean state (`./gradlew clean build`); locally you
can drop `clean` so Gradle builds incrementally.

## Troubleshooting

- **App fails: `schema "ecommerce" does not exist`** — the volume was created
  before the init script existed. Reset it:
  `docker compose -f docker/docker-compose.yaml down -v && docker compose -f docker/docker-compose.yaml up -d`.
- **Port already in use (8080/5432)** — stop the process holding the port, or
  change the mapping in `docker/docker-compose.yaml` / the app port.
- **PostgreSQL not `healthy`** — inspect logs:
  `docker compose -f docker/docker-compose.yaml logs postgres`.

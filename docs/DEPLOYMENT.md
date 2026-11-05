# Run and deploy

## Local JAR

Build the frontend first, then run the Maven wrapper with `clean verify` in `backend/`. Launch `java -jar backend/target/conflict-atlas.jar` from the repository root. Java 21+ is required at runtime; Node is only required for building/developing the UI.

Default address: `127.0.0.1:8080`. Default database: `./data/conflict-atlas.mv.db`, relative to the launch directory. Stop the application before moving/copying this file. Keep backups outside served directories and test a restore. The application initializes its v1 tables automatically; do not share an H2 file between server processes or put it on a network filesystem.

## Environment variables

| Variable | Default / purpose |
| --- | --- |
| `PORT` | `8080` |
| `BIND_ADDRESS` | `127.0.0.1`; use a deliberate reachable address for deployment |
| `LIVE_DATA` | `true`; `false` keeps snapshots and disables outbound source refresh |
| `DATABASE_URL` | `jdbc:h2:file:./data/conflict-atlas;DB_CLOSE_ON_EXIT=FALSE` |
| `DATABASE_USER` | `sa` for local H2; set restricted credentials for PostgreSQL |
| `DATABASE_PASSWORD` | Empty for local H2; supply a private password for PostgreSQL |
| `FORWARD_HEADERS_STRATEGY` | `NONE`; `NATIVE` only behind a trusted, correctly configured proxy |

Set real environment variables in the launching shell or process manager. `.env.example` is documentation and is not automatically loaded by Java. Keep credentials out of committed files and command history.

## PostgreSQL

The PostgreSQL JDBC driver is included. Create a dedicated database/user and configure a URL such as `jdbc:postgresql://127.0.0.1:5432/conflict_atlas`. Restrict that user to the application database. The app needs permission to initialize its tables/indexes on first start. Existing H2 runs are not automatically copied to PostgreSQL; export needed results before changing databases.

The v1 simulation guard, provider refresh lock and write rate limit are process-local. Run a single application instance unless those controls are replaced with shared coordination. PostgreSQL integration is supplied but was not exercised against a live PostgreSQL server in the local verification.

## HTTPS reverse proxy

For a shared deployment, use HTTPS and protect access as appropriate for your institution. This release has browser-scoped workspaces, not user accounts. Put authentication at the proxy if named-user access is required.

Keep the Java listener private. Preserve the original host (including a non-default port), overwrite protocol/client-address headers, and enable `FORWARD_HEADERS_STRATEGY=NATIVE` only for that trusted proxy. For example, the relevant Nginx forwarding directives are:

```nginx
proxy_pass http://127.0.0.1:8080;
proxy_set_header Host $http_host;
proxy_set_header X-Forwarded-Proto $scheme;
proxy_set_header X-Forwarded-For $remote_addr;
```

Test both workspace-cookie `Secure` attributes and same-origin POSTs through the actual proxy. Incorrect host/protocol forwarding causes a 403 on writes. Do not expose the H2 console; it is disabled and not included. The database, repository and downloads must stay outside the public document root.

## Optional Docker recipe

```sh
docker build -t conflict-atlas .
docker run --rm -p 127.0.0.1:8080:8080 -v atlas-data:/app/data conflict-atlas
```

The recipe builds the UI and Java tests, then runs the JAR as an unprivileged user with a persistent volume. It was not executed locally; the local and CI verification use the JAR build. No production service was deployed during development.

## Updating

Stop the process, back up data, build the new frontend/JAR, verify tests and start the new JAR against the intended database path. HTML/API responses use `no-store`; hashed UI assets can be cached. Exported results keep their original model and resolved inputs. Changes to model equations must receive a new model version.

For data-release updates, review `scripts/refresh_data.py` URLs, versions, coverage and aliases together, rebuild snapshots, run data checks, inspect the quality register, then rebuild the application. Never relabel an old conflict release as live.

# Darcy Vet

Darcy Vet is a Kotlin veterinary clinic management system. It supports owner and pet registration, richer veterinary profiles, appointment scheduling, medical records, service-based billing, selectable CLI records, change history, reports, safer menu navigation, empty-list feedback, and local SQLite persistence.

## Features

- Register and edit owners.
- Register and edit pet patients with sex, date of birth, weight, allergies, and medical conditions.
- Search owners and patients.
- Schedule, reschedule, complete, and cancel appointments with visit type and veterinarian name.
- Require confirmation before completing or cancelling appointments.
- Record and correct diagnosis, treatment, visit notes, and veterinarian name.
- Keep previous medical-record values when records are corrected.
- Generate invoices from clinic services, mark invoices as paid, and void unpaid invoices.
- Require confirmation before payment and invoice void actions.
- Keep invoice status changes for creation, payment, and voiding.
- Show clinic overview reports for owners, pets, appointments, invoices, and paid revenue.
- Select owners, pets, appointments, records, and invoices from numbered CLI lists instead of typing IDs manually.
- Use `0. Back` options in submenus.
- Show clear empty-state messages when there are no owners, pets, appointments, records, or invoices to display.
- Save and reload clinic data from a local SQLite database.
- Run automated tests for core workflows, correction workflows, richer domain fields, change history, reports, CLI confirmation parsing, storage behavior, SQLite repositories, CLI runtime composition, JSON import/export behavior, database backup/restore behavior, and database health checks.

## Product direction

The repository may remain named `Darcy-Veterinary`, but the user-facing product name is **Darcy Vet**. The current console application is the domain and workflow foundation for a future sellable desktop veterinary clinic management system.

The current development focus is **Stage 2: Product Foundation**. Stage 2 is not a GUI rewrite. Its goal is to make the data layer reliable enough for real clinic data by introducing SQLite, migrations, repository integration tests, backup/restore, and JSON import/export support.

## Requirements

- JDK 17 or newer
- Gradle with Kotlin JVM support

Gradle is configured to use a Java 17 toolchain and can provision one automatically when the local machine does not already have JDK 17 installed.

## Run

```bash
./gradlew run
```

On Windows:

```powershell
.\gradlew.bat run
```

## Test

```bash
./gradlew test
```

On Windows:

```powershell
.\gradlew.bat test
```

## Data storage

Runtime data is saved under the `data/` directory. The CLI now uses SQLite by default at `data/darcy-vet.db`. Database migrations run automatically during CLI startup before repositories are used.

The SQLite foundation includes:

- `DatabaseConfig` for database and backup paths.
- `DatabaseConnectionFactory` for local SQLite connections.
- `DatabaseMigrator` for ordered, idempotent schema migrations.
- Initial schema tables for owners, pets, allergies, medical conditions, appointments, medical records, medical record revisions, invoices, invoice items, invoice status history, and schema migrations.
- Lookup indexes for common repository and reporting queries.
- SQLite repository implementations for all current repository interfaces.
- CLI runtime composition that wires services to SQLite repositories by default.
- No-op CLI storage adapter so JSON snapshots are not loaded into or saved over the SQLite runtime path.
- JSON-to-SQLite import support for migrating existing `clinic-data.json` snapshots into SQLite.
- SQLite-to-JSON export support for writing SQLite data back into the JSON snapshot format.
- Manual database backup and restore support for copying `data/darcy-vet.db` to and from timestamped files under `data/backups/`.
- Database health checks for file presence, SQLite connectivity, foreign-key enforcement, integrity status, required tables, and applied migrations.

The `data/` directory is ignored by Git so local clinic records, generated SQLite databases, and local backups do not get committed accidentally.

JSON is no longer the default runtime persistence path, but import/export support is available for migration and snapshots.

## Stage 2 status

Implemented:

- SQLite JDBC dependency.
- Database path configuration.
- SQLite connection factory.
- Migration runner.
- Initial schema migration.
- Index migration.
- TDD migration coverage.
- `SqliteOwnerRepository`.
- SQLite owner repository integration tests.
- `SqlitePetRepository`.
- SQLite pet repository integration tests.
- `SqliteAppointmentRepository`.
- SQLite appointment repository integration tests.
- `SqliteMedicalRecordRepository`.
- SQLite medical record repository integration tests.
- `SqliteMedicalRecordRevisionRepository`.
- SQLite medical record revision repository integration tests.
- `SqliteInvoiceRepository`.
- SQLite invoice repository integration tests.
- `SqliteInvoiceStatusHistoryRepository`.
- SQLite invoice status history repository integration tests.
- CLI wiring to SQLite by default.
- SQLite CLI runtime composition tests.
- JSON-to-SQLite import support.
- JSON-to-SQLite import integration tests.
- SQLite-to-JSON export support.
- SQLite-to-JSON export integration tests.
- Manual backup and restore support.
- Database backup and restore integration tests.
- Database health check support.
- Database health check integration tests.

Stage 2 is complete. The next product direction is Stage 3: desktop GUI development on top of the SQLite-backed application foundation.

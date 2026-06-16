# Darcy Veterinary

Darcy Veterinary is a Kotlin console application being expanded into a fuller veterinary clinic management system.

## Current capabilities

- Register pet patients.
- View registered patients.
- Search a patient and add a basic clinic service.
- Save and reload local CSV data.
- English console interface.

## Development roadmap

This roadmap is temporary. Completed phases are removed as they are implemented.

### Phase 1: Domain expansion

- Add owner records and connect pets to owners.
- Add appointment scheduling.
- Add medical records for diagnosis, treatment, and notes.
- Add invoices, invoice items, service pricing, and payment status.
- Add domain-specific exceptions.

### Phase 2: Application service layer

- Move business logic out of the console UI.
- Add services for owners, patients, appointments, medical records, and billing.
- Add deterministic ID generation for tests and UUID-based generation for runtime.

### Phase 3: Repository layer

- Replace direct list manipulation with repository interfaces.
- Add in-memory repository implementations for development and tests.

### Phase 4: Persistence

- Replace single patient-only CSV storage with separate storage for owners, pets, appointments, medical records, and invoices.
- Keep runtime data out of Git.

### Phase 5: CLI restructuring

- Split the large console UI into feature menus.
- Keep the main UI responsible for routing only.

### Phase 6: Test-driven development

- Add automated tests for owner-pet registration, duplicate validation, appointment status changes, medical records, billing totals, payment status, and storage reloads.
- Use tests to lock the expected clinic workflows before implementation.

## Target structure

```text
src/main/kotlin/darcy/veterinary/
├── application/
├── domain/
│   ├── exception/
│   └── model/
├── infrastructure/
│   ├── memory/
│   ├── seed/
│   └── storage/
├── presentation/
│   └── cli/
└── repository/
```

## Requirements

- JDK 17 or newer
- Gradle with Kotlin JVM support

Gradle is configured to use a Java 17 toolchain and can provision one automatically when the local machine does not already have JDK 17 installed.

## Run

```bash
gradle run
```

## Test

```bash
gradle test
```

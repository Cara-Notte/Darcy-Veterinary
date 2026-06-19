# Darcy Veterinary

Darcy Veterinary is a Kotlin console application for managing veterinary clinic operations. It supports owner and pet registration, richer veterinary profiles, appointment scheduling, medical records, service-based billing, selectable CLI records, change history, reports, empty-list feedback, and local JSON persistence.

## Features

- Register and edit owners.
- Register and edit pet patients with sex, date of birth, weight, allergies, and medical conditions.
- Search owners and patients.
- Schedule, reschedule, complete, and cancel appointments with visit type and veterinarian name.
- Record and correct diagnosis, treatment, visit notes, and veterinarian name.
- Keep previous medical-record values when records are corrected.
- Generate invoices from clinic services, mark invoices as paid, and void unpaid invoices.
- Keep invoice status changes for creation, payment, and voiding.
- Show clinic overview reports for owners, pets, appointments, invoices, and paid revenue.
- Select owners, pets, appointments, records, and invoices from numbered CLI lists instead of typing IDs manually.
- Show clear empty-state messages when there are no owners, pets, appointments, records, or invoices to display.
- Save and reload clinic data from local JSON files.
- Run automated tests for core workflows, correction workflows, richer domain fields, change history, reports, storage behavior, and CLI list rendering.

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

Runtime data is saved under the `data/` directory. The CLI uses `clinic-data.json` by default so clinical notes, richer veterinary fields, change history, invoice status changes, and names can safely contain punctuation and line breaks. The `data/` directory is ignored by Git so local clinic records do not get committed accidentally.

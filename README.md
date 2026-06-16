# Darcy Veterinary

Darcy Veterinary is a Kotlin console application for managing veterinary clinic operations. It supports owner and pet registration, appointment scheduling, medical records, service-based billing, selectable CLI records, empty-list feedback, and local JSON persistence.

## Features

- Register owners and pet patients.
- Search owners and patients.
- Schedule, complete, and cancel appointments.
- Record diagnosis, treatment, and visit notes.
- Generate invoices from clinic services and mark invoices as paid.
- Select owners, pets, appointments, and invoices from numbered CLI lists instead of typing IDs manually.
- Show clear empty-state messages when there are no owners, pets, appointments, records, or invoices to display.
- Save and reload clinic data from local JSON files.
- Run automated tests for core clinic workflows, storage behavior, and CLI list rendering.

## Project structure

```text
src/
├── main/kotlin/darcy/veterinary/
│   ├── Main.kt
│   ├── application/
│   │   ├── AppointmentService.kt
│   │   ├── BillingService.kt
│   │   ├── IdGenerator.kt
│   │   ├── OwnerService.kt
│   │   ├── PatientService.kt
│   │   └── RecordService.kt
│   ├── domain/
│   │   ├── exception/
│   │   │   └── ClinicErrors.kt
│   │   └── model/
│   │       ├── Appointment.kt
│   │       ├── AppointmentStatus.kt
│   │       ├── ClinicService.kt
│   │       ├── Invoice.kt
│   │       ├── InvoiceItem.kt
│   │       ├── MedicalRecord.kt
│   │       ├── Owner.kt
│   │       ├── PaymentStatus.kt
│   │       └── Pet.kt
│   ├── infrastructure/
│   │   ├── memory/
│   │   │   ├── InMemoryAppointmentRepository.kt
│   │   │   ├── InMemoryInvoiceRepository.kt
│   │   │   ├── InMemoryOwnerRepository.kt
│   │   │   ├── InMemoryPetRepository.kt
│   │   │   └── InMemoryRecordRepository.kt
│   │   ├── seed/
│   │   │   └── SampleDataSeeder.kt
│   │   └── storage/
│   │       ├── ClinicStorage.kt
│   │       ├── CsvClinicStorage.kt
│   │       └── JsonClinicStorage.kt
│   ├── presentation/
│   │   └── cli/
│   │       ├── AppointmentMenu.kt
│   │       ├── BillingMenu.kt
│   │       ├── CliListFormatter.kt
│   │       ├── CliListSelector.kt
│   │       ├── ConsoleUI.kt
│   │       ├── InputReader.kt
│   │       ├── PatientMenu.kt
│   │       └── RecordMenu.kt
│   └── repository/
│       ├── AppointmentRepository.kt
│       ├── InvoiceRepository.kt
│       ├── OwnerRepository.kt
│       ├── PetRepository.kt
│       └── RecordRepository.kt
└── test/kotlin/darcy/veterinary/
    ├── CliListFormatterTest.kt
    ├── ClinicWorkflowTest.kt
    ├── CsvClinicStorageTest.kt
    └── JsonClinicStorageTest.kt
```

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

Runtime data is saved under the `data/` directory. The CLI uses `clinic-data.json` by default so clinical notes and names can safely contain punctuation and line breaks. The `data/` directory is ignored by Git so local clinic records do not get committed accidentally.

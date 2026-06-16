# Darcy Veterinary

Darcy Veterinary is a Kotlin console application for managing veterinary clinic patients and basic services. It lets clinic staff register pets, view patient records, add clinic services, and persist patient data to a local CSV file.

## Features

- Register a new pet patient with an ID, name, and species.
- View all registered patients and their service history.
- Search a patient by ID and add grooming or vaccination services.
- Save and reload patient data from `patients.csv`.
- English console interface.

## Project structure

```text
src/main/kotlin/darcy/veterinary/
├── Main.kt
├── domain/
│   ├── ClinicService.kt
│   └── Pet.kt
├── repository/
│   └── PetRepository.kt
├── storage/
│   └── FileStorage.kt
└── ui/
    └── ConsoleUI.kt
```

## Requirements

- JDK 17 or newer
- Kotlin compiler, or Gradle with Kotlin JVM support

## Run with Kotlin compiler

```bash
kotlinc src/main/kotlin -include-runtime -d darcy-veterinary.jar
java -jar darcy-veterinary.jar
```

## Run with Gradle

```bash
gradle run
```

## Data storage

Patient records are saved to `patients.csv` in the working directory. The file is created automatically when data is saved.

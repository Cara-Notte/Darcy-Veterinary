package darcy.veterinary.storage

import darcy.veterinary.domain.ClinicService
import darcy.veterinary.domain.Pet
import darcy.veterinary.repository.PetRepository
import java.io.File

object FileStorage {
    private const val DEFAULT_FILENAME = "patients.csv"
    private const val FIELD_SEPARATOR = ";"
    private const val SERVICE_SEPARATOR = "|"

    fun saveData(pets: List<Pet>, filename: String = DEFAULT_FILENAME): Result<Unit> = runCatching {
        val header = "id;name;species;services"
        val records = pets.joinToString(System.lineSeparator()) { pet ->
            listOf(
                sanitize(pet.id),
                sanitize(pet.name),
                sanitize(pet.species),
                pet.services.joinToString(SERVICE_SEPARATOR) { it.code }
            ).joinToString(FIELD_SEPARATOR)
        }

        File(filename).writeText(
            if (records.isBlank()) header else "$header${System.lineSeparator()}$records"
        )
    }

    fun loadData(repository: PetRepository, filename: String = DEFAULT_FILENAME): Result<Int> = runCatching {
        val file = File(filename)
        if (!file.exists()) return@runCatching 0

        file.readLines()
            .drop(1)
            .filter { it.isNotBlank() }
            .mapNotNull(::parsePet)
            .count { repository.add(it) }
    }

    private fun parsePet(line: String): Pet? {
        val parts = line.split(FIELD_SEPARATOR)
        if (parts.size < 3) return null

        val services = parts.getOrNull(3)
            ?.split(SERVICE_SEPARATOR)
            ?.filter { it.isNotBlank() }
            ?.mapNotNull(ClinicService::fromCode)
            ?.toMutableList()
            ?: mutableListOf()

        return Pet(
            id = parts[0],
            name = parts[1],
            species = parts[2],
            services = services
        )
    }

    private fun sanitize(value: String): String = value
        .replace(FIELD_SEPARATOR, " ")
        .replace(System.lineSeparator(), " ")
        .trim()
}

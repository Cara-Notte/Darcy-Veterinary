package darcy.veterinary.sqlite

import darcy.veterinary.application.PatientService
import darcy.veterinary.application.SequenceIdGenerator
import darcy.veterinary.domain.exception.EntityNotFoundException
import darcy.veterinary.domain.model.Owner
import darcy.veterinary.domain.model.Pet
import darcy.veterinary.domain.model.PetSex
import darcy.veterinary.infrastructure.database.DatabaseConfig
import darcy.veterinary.infrastructure.database.DatabaseConnectionFactory
import darcy.veterinary.infrastructure.database.DatabaseMigrator
import darcy.veterinary.infrastructure.sqlite.SqliteOwnerRepository
import darcy.veterinary.infrastructure.sqlite.SqlitePetRepository
import java.nio.file.Path
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SqlitePetRepositoryTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `saves and reloads pet with rich profile`() {
        val fixture = fixture()
        val pet = Pet(
            id = "PET-0001",
            ownerId = "OWN-0001",
            name = "Milo",
            species = "Cat",
            breed = "Persian",
            age = 2,
            sex = PetSex.MALE,
            dateOfBirth = LocalDate.of(2024, 5, 12),
            weightKg = 4.25,
            allergies = listOf("Chicken", "Dust"),
            medicalConditions = listOf("Asthma")
        )

        fixture.petRepository.save(pet)

        assertEquals(pet, fixture.petRepository.findById("PET-0001"))
    }

    @Test
    fun `updates pet and replaces allergy and medical condition rows transactionally`() {
        val fixture = fixture()
        val original = Pet(
            id = "PET-0001",
            ownerId = "OWN-0001",
            name = "Milo",
            species = "Cat",
            breed = "Persian",
            age = 2,
            sex = PetSex.MALE,
            dateOfBirth = LocalDate.of(2024, 5, 12),
            weightKg = 4.25,
            allergies = listOf("Chicken", "Dust"),
            medicalConditions = listOf("Asthma", "Sensitive stomach")
        )
        val updated = original.copy(
            name = "Milo Jr",
            breed = null,
            age = 3,
            sex = PetSex.UNKNOWN,
            dateOfBirth = null,
            weightKg = 4.7,
            allergies = listOf("Fish"),
            medicalConditions = emptyList()
        )

        fixture.petRepository.save(original)
        fixture.petRepository.save(updated)

        assertEquals(updated, fixture.petRepository.findById("PET-0001"))
        assertEquals(listOf(updated), fixture.petRepository.findAll())
    }

    @Test
    fun `find all returns pets in insertion order`() {
        val fixture = fixture()
        val first = Pet("PET-0001", "OWN-0001", "Milo", "Cat")
        val second = Pet("PET-0002", "OWN-0001", "Bruno", "Dog", breed = "Golden Retriever")

        fixture.petRepository.save(first)
        fixture.petRepository.save(second)

        assertEquals(listOf(first, second), fixture.petRepository.findAll())
    }

    @Test
    fun `find by owner id returns only matching pets`() {
        val fixture = fixture()
        val ownerTwo = Owner("OWN-0002", "Bima Hartono", "0822222222", null)
        fixture.ownerRepository.save(ownerTwo)
        val milo = Pet("PET-0001", "OWN-0001", "Milo", "Cat")
        val luna = Pet("PET-0002", "OWN-0001", "Luna", "Cat")
        val bruno = Pet("PET-0003", "OWN-0002", "Bruno", "Dog")

        fixture.petRepository.save(milo)
        fixture.petRepository.save(luna)
        fixture.petRepository.save(bruno)

        assertEquals(listOf(milo, luna), fixture.petRepository.findByOwnerId("OWN-0001"))
        assertEquals(listOf(bruno), fixture.petRepository.findByOwnerId("OWN-0002"))
    }

    @Test
    fun `search matches name species and breed case insensitively`() {
        val fixture = fixture()
        val milo = Pet("PET-0001", "OWN-0001", "Milo", "Cat", breed = "Persian")
        val bruno = Pet("PET-0002", "OWN-0001", "Bruno", "Dog", breed = "Golden Retriever")
        fixture.petRepository.save(milo)
        fixture.petRepository.save(bruno)

        assertEquals(listOf(milo), fixture.petRepository.search("milo"))
        assertEquals(listOf(bruno), fixture.petRepository.search("dog"))
        assertEquals(listOf(bruno), fixture.petRepository.search("RETRIEVER"))
        assertEquals(emptyList(), fixture.petRepository.search("   "))
    }

    @Test
    fun `pet persists after repository is reopened`() {
        val config = databaseConfig()
        val firstFixture = fixture(config)
        val pet = Pet(
            id = "PET-0001",
            ownerId = "OWN-0001",
            name = "Milo",
            species = "Cat",
            allergies = listOf("Chicken"),
            medicalConditions = listOf("Asthma")
        )

        firstFixture.petRepository.save(pet)

        val reopenedFixture = fixture(config, seedDefaultOwner = false)
        assertEquals(pet, reopenedFixture.petRepository.findById("PET-0001"))
    }

    @Test
    fun `returns null for missing pet`() {
        val fixture = fixture()

        assertNull(fixture.petRepository.findById("PET-MISSING"))
    }

    @Test
    fun `database foreign key rejects pet for missing owner outside service layer`() {
        val fixture = fixture(seedDefaultOwner = false)
        val pet = Pet("PET-0001", "OWN-MISSING", "Ghost", "Dog")

        assertFailsWith<IllegalStateException> {
            fixture.petRepository.save(pet)
        }
    }

    @Test
    fun `patient service works with SQLite owner and pet repositories`() {
        val fixture = fixture()
        val service = PatientService(fixture.petRepository, fixture.ownerRepository, SequenceIdGenerator())

        val pet = service.registerPet(
            ownerId = "OWN-0001",
            name = "  Milo  ",
            species = "  Cat  ",
            breed = "  Persian  ",
            allergies = listOf(" Chicken ", " "),
            medicalConditions = listOf(" Asthma ")
        )

        assertEquals("PET-0001", pet.id)
        assertEquals(Pet("PET-0001", "OWN-0001", "Milo", "Cat", breed = "Persian", allergies = listOf("Chicken"), medicalConditions = listOf("Asthma")), fixture.petRepository.findById(pet.id))
        assertFailsWith<EntityNotFoundException> {
            service.registerPet("OWN-MISSING", "Ghost", "Dog")
        }
    }

    private fun fixture(
        config: DatabaseConfig = databaseConfig(),
        seedDefaultOwner: Boolean = true
    ): Fixture {
        val connectionFactory = DatabaseConnectionFactory(config)
        DatabaseMigrator(connectionFactory).migrate()
        val ownerRepository = SqliteOwnerRepository(connectionFactory)
        val petRepository = SqlitePetRepository(connectionFactory)

        if (seedDefaultOwner && ownerRepository.findById("OWN-0001") == null) {
            ownerRepository.save(Owner("OWN-0001", "Nadia Prasetyo", "0811111111", "nadia@example.com"))
        }

        return Fixture(ownerRepository, petRepository)
    }

    private fun databaseConfig(): DatabaseConfig = DatabaseConfig(
        databasePath = tempDir.resolve("darcy-vet.db"),
        backupDirectory = tempDir.resolve("backups")
    )

    private data class Fixture(
        val ownerRepository: SqliteOwnerRepository,
        val petRepository: SqlitePetRepository
    )
}

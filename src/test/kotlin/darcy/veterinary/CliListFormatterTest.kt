package darcy.veterinary

import darcy.veterinary.presentation.cli.CliListFormatter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CliListFormatterTest {
    @Test
    fun `empty list renders a clear empty state`() {
        val lines = CliListFormatter.renderList(
            title = "Owners",
            items = emptyList<String>(),
            emptyMessage = "No owners registered yet.",
            formatter = { it }
        )

        assertEquals(listOf("Owners", "No owners registered yet."), lines)
    }

    @Test
    fun `non-empty list renders numbered rows`() {
        val lines = CliListFormatter.renderList(
            title = "Pets",
            items = listOf("Milo", "Darcy"),
            emptyMessage = "No pets registered yet.",
            formatter = { it }
        )

        assertEquals(listOf("Pets", "1. Milo", "2. Darcy"), lines)
    }
}

package darcy.veterinary

import darcy.veterinary.presentation.cli.CliConfirmation
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CliConfirmationTest {
    @Test
    fun `confirmation parser accepts yes values`() {
        assertEquals(true, CliConfirmation.parse("y"))
        assertEquals(true, CliConfirmation.parse("yes"))
        assertEquals(true, CliConfirmation.parse("YES"))
    }

    @Test
    fun `confirmation parser accepts no values`() {
        assertEquals(false, CliConfirmation.parse("n"))
        assertEquals(false, CliConfirmation.parse("no"))
        assertEquals(false, CliConfirmation.parse("NO"))
    }

    @Test
    fun `confirmation parser rejects unclear values`() {
        assertEquals(null, CliConfirmation.parse("maybe"))
        assertEquals(null, CliConfirmation.parse(""))
    }
}

package no.nav.helse.modell.command.nyny

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class ArbeidsgiverCommandTest {
    private companion object {
        private const val ORGNR = "123456789"
    }

    private val dao = mockk<ArbeidsgiverDao>(relaxed = true)

    private lateinit var context: CommandContext
    private val command = ArbeidsgiverCommand(ORGNR, dao)

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(dao)
    }

    @Test
    fun `opprett arbeidsgiver`() {
        arbeidsgiverFinnesIkke()
        assertTrue(command.execute(context))
        verify(exactly = 1) { dao.insertArbeidsgiver(ORGNR.toLong(), "Ukjent") }
    }

    @Test
    fun `oppretter ikke arbeidsgiver når den finnes`() {
        arbeidsgiverFinnes()
        assertTrue(command.execute(context))
        verify(exactly = 0) { dao.insertArbeidsgiver(any(), any()) }
    }

    private fun arbeidsgiverFinnes() {
        every { dao.findArbeidsgiverByOrgnummer(ORGNR.toLong()) } returns 1
    }

    private fun arbeidsgiverFinnesIkke() {
        every { dao.findArbeidsgiverByOrgnummer(ORGNR.toLong()) } returns null
    }
}

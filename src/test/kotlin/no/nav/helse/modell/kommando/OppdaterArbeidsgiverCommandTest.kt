package no.nav.helse.modell.kommando

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class OppdaterArbeidsgiverCommandTest {
    private companion object {
        private const val ORGNR = "123456789"
    }

    private val dao = mockk<ArbeidsgiverDao>(relaxed = true)

    private lateinit var context: CommandContext
    private val command = OppdaterArbeidsgiverCommand(ORGNR, dao)

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(dao)
    }

    @Test
    fun `oppdaterer ikke når informasjonen er ny`() {
        every { dao.findNavnSistOppdatert(ORGNR) } returns LocalDate.now()
        assertTrue(command.execute(context))
        verify(exactly = 0) { dao.updateNavn(any(), any()) }
    }

    @Test
    fun `oppdaterer ikke når informasjonen er gammel`() {
        every { dao.findNavnSistOppdatert(ORGNR) } returns LocalDate.now().minusYears(1)
        assertTrue(command.execute(context))
        assertFalse(context.harBehov())
        verify(exactly = 0) { dao.updateNavn(any(), any()) }
    }
}

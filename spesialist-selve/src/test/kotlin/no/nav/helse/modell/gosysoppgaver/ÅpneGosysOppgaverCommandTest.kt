package no.nav.helse.modell.gosysoppgaver

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.meldinger.ÅpneGosysOppgaverløsning
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.behov
import no.nav.helse.modell.vedtak.Warning
import no.nav.helse.modell.vedtak.WarningKilde
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ÅpneGosysOppgaverCommandTest {

    private companion object {
        private const val FNR = "12345678911"
        private const val AKTØR_ID = "1234567891112"
        private val VEDTAKPERIODE_ID = UUID.randomUUID()
    }

    private val dao = mockk<ÅpneGosysOppgaverDao>(relaxed = true)
    private val warningDao = mockk<WarningDao>(relaxed = true)
    private val command = ÅpneGosysOppgaverCommand(AKTØR_ID, dao, warningDao, VEDTAKPERIODE_ID)
    private lateinit var context: CommandContext

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(dao)
    }

    @Test
    fun `Ber om åpne oppgaver i gosys`() {
        assertFalse(command.execute(context))
        assertEquals(listOf("ÅpneOppgaver"), context.behov().keys.toList())
    }

    @Test
    fun `Mangler løsning ved resume`() {
        assertFalse(command.resume(context))
        verify(exactly = 0) { dao.persisterÅpneGosysOppgaver(any()) }
    }

    @Test
    fun `Lagrer løsning ved resume`() {
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, 0, false))
        assertTrue(command.resume(context))
        verify(exactly = 1) { dao.persisterÅpneGosysOppgaver(any()) }
    }

    @Test
    fun `Lagrer ikke warning ved ingen åpne oppgaver`() {
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, 0, false))
        assertTrue(command.resume(context))
        verify(exactly = 1) { dao.persisterÅpneGosysOppgaver(any()) }
        verify(exactly = 0) { warningDao.leggTilWarning(VEDTAKPERIODE_ID, any()) }
    }

    @Test
    fun `Lagrer warning ved åpne oppgaver`() {
        val forventetWarning = Warning(
            melding = "Det finnes åpne oppgaver på sykepenger i Gosys",
            kilde = WarningKilde.Spesialist
        )
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, 1, false))
        assertTrue(command.resume(context))
        verify(exactly = 1) { dao.persisterÅpneGosysOppgaver(any()) }
        verify(exactly = 1) { warningDao.leggTilWarning(VEDTAKPERIODE_ID, forventetWarning) }
    }

    @Test
    fun `Lagrer warning ved oppslag feilet`() {
        val forventetWarning = Warning(
            melding = "Kunne ikke sjekke åpne oppgaver på sykepenger i Gosys",
            kilde = WarningKilde.Spesialist
        )
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, null, true))
        assertTrue(command.resume(context))
        verify(exactly = 1) { dao.persisterÅpneGosysOppgaver(any()) }
        verify(exactly = 1) { warningDao.leggTilWarning(VEDTAKPERIODE_ID, forventetWarning) }
    }
}

package no.nav.helse.modell.varsel

import ToggleHelpers.disable
import ToggleHelpers.enable
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.UUID
import no.nav.helse.mediator.Toggle
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vergemal.VergemålDao
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class LeggPåVarslerCommandTest {
    private lateinit var context: CommandContext

    private val vergemålDao = mockk<VergemålDao>(relaxed = true)
    private val personDao = mockk<PersonDao>(relaxed = true)
    private val sykefraværstilfelle = mockk<Sykefraværstilfelle>(relaxed = true)

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(vergemålDao, personDao, sykefraværstilfelle)
        Toggle.BeholdRevurderingerMedVergemålEllerUtland.enable()
    }

    @Test
    fun `skal legge på varsel om vergemål dersom utbetaling er revurdering`() {
        val slot = slot<Varsel>()
        every { vergemålDao.harVergemål(fødselsnummer) } returns true
        assertTrue(hentCommand(Utbetalingtype.REVURDERING).execute(context))
        verify (exactly = 1) { sykefraværstilfelle.håndter(capture(slot), hendelseId) }
        assertEquals("SB_EX_4", slot.captured.toDto().varselkode)
    }

    @Test
    fun `skal ikke legge på varsel for vergemål dersom utbetaling ikke er revurdering`() {
        every { vergemålDao.harVergemål(fødselsnummer) } returns true
        assertTrue(hentCommand(Utbetalingtype.UTBETALING).execute(context))
        verify (exactly = 0) { sykefraværstilfelle.håndter(any(), hendelseId) }
    }

    @Test
    fun `skal ikke legge på varsel for vergemål dersom toggle er disabled`() {
        Toggle.BeholdRevurderingerMedVergemålEllerUtland.disable()
        every { vergemålDao.harVergemål(fødselsnummer) } returns true
        assertTrue(hentCommand(Utbetalingtype.REVURDERING).execute(context))
        verify (exactly = 0) { sykefraværstilfelle.håndter(any(), hendelseId) }
    }

    @Test
    fun `skal legge på varsel om utland dersom utbetaling er revurdering`() {
        val slot = slot<Varsel>()
        every { personDao.finnEnhetId(fødselsnummer) } returns "0393"
        assertTrue(hentCommand(Utbetalingtype.REVURDERING).execute(context))
        verify (exactly = 1) { sykefraværstilfelle.håndter(capture(slot), hendelseId) }
        assertEquals("SB_EX_5", slot.captured.toDto().varselkode)
    }

    @Test
    fun `skal ikke legge på varsel for utland dersom utbetaling ikke er revurdering`() {
        every { personDao.finnEnhetId(fødselsnummer) } returns "0393"
        assertTrue(hentCommand(Utbetalingtype.UTBETALING).execute(context))
        verify (exactly = 0) { sykefraværstilfelle.håndter(any(), hendelseId) }
    }

    @Test
    fun `skal ikke legge på varsel for utland dersom toggle er disabled`() {
        Toggle.BeholdRevurderingerMedVergemålEllerUtland.disable()
        every { personDao.finnEnhetId(fødselsnummer) } returns "0393"
        assertTrue(hentCommand(Utbetalingtype.REVURDERING).execute(context))
        verify (exactly = 0) { sykefraværstilfelle.håndter(any(), hendelseId) }
    }

    private fun hentCommand(utbetalingstype: Utbetalingtype) =
        LeggPåVarslerCommand(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            personDao = personDao,
            vergemålDao = vergemålDao,
            hendelseId = hendelseId,
            utbetaling = Utbetaling(utbetalingId, 1000, 1000, utbetalingstype),
            sykefraværstilfelle = sykefraværstilfelle
        )

    private companion object {
        private const val fødselsnummer = "12345678910"
        private val vedtaksperiodeId = UUID.randomUUID()
        private val hendelseId = UUID.randomUUID()
        private val utbetalingId = UUID.randomUUID()
    }

}
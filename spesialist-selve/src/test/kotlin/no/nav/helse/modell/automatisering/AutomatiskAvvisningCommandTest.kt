package no.nav.helse.modell.automatisering

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vergemal.VergemålDao
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AutomatiskAvvisningCommandTest {
    private lateinit var context: CommandContext

    private val vergemålDao = mockk<VergemålDao>(relaxed = true)
    private val personDao = mockk<PersonDao>(relaxed = true)
    private val egenAnsattDao = mockk<EgenAnsattDao>(relaxed = true)
    private val godkjenningMediator = mockk<GodkjenningMediator>(relaxed = true)
    private val sykefraværstilfelle = mockk<Sykefraværstilfelle>(relaxed = true)

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(vergemålDao, personDao, egenAnsattDao, godkjenningMediator)
    }

    @Test
    fun `skal ikke avvises ved egen ansatt`() {
        every { egenAnsattDao.erEgenAnsatt(fødselsnummer) } returns true
        executeCommand(hentCommand(Utbetalingtype.UTBETALING), null)
    }

    @Test
    fun `skal avvise ved vergemål dersom utbetaling ikke er revurdering`() {
        every { vergemålDao.harVergemål(fødselsnummer) } returns true
        executeCommand(hentCommand(Utbetalingtype.UTBETALING), "Vergemål")
    }

    @Test
    fun `skal ikke avvise ved vergemål dersom utbetaling er revurdering`() {
        every { vergemålDao.harVergemål(fødselsnummer) } returns true
        executeCommand(hentCommand(Utbetalingtype.REVURDERING), null)
    }

    @Test
    fun `skal ikke avvise ved vergemål dersom kanAvvises er false`() {
        every { vergemålDao.harVergemål(fødselsnummer) } returns true
        executeCommand(hentCommand(Utbetalingtype.UTBETALING, false), null)
    }

    @Test
    fun `skal avvise ved utland dersom utbetaling ikke er revurdering`() {
        every { personDao.finnEnhetId(fødselsnummer) } returns "0393"
        executeCommand(hentCommand(Utbetalingtype.UTBETALING), "Utland")
    }

    @Test
    fun `skal avvise skjønnsfastsettelse dersom utbetaling ikke er revurdering og fødselsnummer ikke starter på 31`() {
        every { sykefraværstilfelle.kreverSkjønnsfastsettelse(vedtaksperiodeId) } returns true
        executeCommand(
            hentCommand(Utbetalingtype.UTBETALING, fødselsnummer = "12345678910"),
            "Skjønnsfastsettelse"
        )
    }

    @Test
    fun `skal ikke avvise uavhengig av skjønnsfastsettelse dersom kanAvvises-flagg er false`() {
        every { sykefraværstilfelle.kreverSkjønnsfastsettelse(vedtaksperiodeId) } returns true
        executeCommand(
            hentCommand(Utbetalingtype.UTBETALING, kanAvvises = false, fødselsnummer = "12345678910"),
            null
        )
    }

    @Test
    fun `skal ikke avvise skjønnsfastsettelse dersom utbetaling ikke er revurdering og fødselsnummer starter på 31`() {
        every { sykefraværstilfelle.kreverSkjønnsfastsettelse(vedtaksperiodeId) } returns true
        executeCommand(hentCommand(Utbetalingtype.UTBETALING, fødselsnummer = "31345678910"), null)
    }

    @Test
    fun `skal ikke avvise skjønnsfastsettelse dersom utbetaling er revurdering og fødselsnummer ikke starter på 31`() {
        every { sykefraværstilfelle.kreverSkjønnsfastsettelse(vedtaksperiodeId) } returns true
        executeCommand(hentCommand(Utbetalingtype.REVURDERING, fødselsnummer = "12345678910"), null)
    }

    @Test
    fun `skal ikke avvise ved utland dersom utbetaling er revurdering`() {
        every { personDao.finnEnhetId(fødselsnummer) } returns "0393"
        executeCommand(hentCommand(Utbetalingtype.REVURDERING), null)
    }

    private fun executeCommand(command: AutomatiskAvvisningCommand, forventetÅrsakTilAvvisning: String?) {
        assertTrue(command.execute(context))
        if (forventetÅrsakTilAvvisning != null)
            verify (exactly = 1) { godkjenningMediator.automatiskAvvisning(any(), any(), listOf(forventetÅrsakTilAvvisning), any(), any()) }
        else
            verify (exactly = 0) { godkjenningMediator.automatiskAvvisning(any(), any(), any()) }
    }

    private fun hentCommand(
        utbetalingstype: Utbetalingtype,
        kanAvvises: Boolean = true,
        fødselsnummer: String = "12345678910",
    ) =
        AutomatiskAvvisningCommand(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            personDao = personDao,
            vergemålDao = vergemålDao,
            godkjenningMediator = godkjenningMediator,
            hendelseId = hendelseId,
            utbetaling = Utbetaling(utbetalingId, 1000, 1000, utbetalingstype),
            kanAvvises = kanAvvises,
            sykefraværstilfelle = sykefraværstilfelle,
        )

    private companion object {
        private const val fødselsnummer = "12345678910"
        private val vedtaksperiodeId = UUID.randomUUID()
        private val hendelseId = UUID.randomUUID()
        private val utbetalingId = UUID.randomUUID()
    }
}

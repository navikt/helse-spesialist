package no.nav.helse.modell.automatisering

import ToggleHelpers.disable
import ToggleHelpers.enable
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.Toggle
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.person.PersonDao
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

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(vergemålDao, personDao, egenAnsattDao, godkjenningMediator)
    }

    @Test
    fun `skal avvise ved egen ansatt`() {
        every { egenAnsattDao.erEgenAnsatt(fødselsnummer) } returns true
        executeCommand(hentCommand(Utbetalingtype.UTBETALING), "Egen ansatt")
    }

    @Test
    fun `skal ikke avvises ved egen ansatt når toggle er på`() {
        Toggle.EgenAnsatt.enable()
        every { egenAnsattDao.erEgenAnsatt(fødselsnummer) } returns true
        executeCommand(hentCommand(Utbetalingtype.UTBETALING), null)
        Toggle.EgenAnsatt.disable()
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
    fun `skal avvise ved utland dersom utbetaling ikke er revurdering`() {
        every { personDao.finnEnhetId(fødselsnummer) } returns "0393"
        executeCommand(hentCommand(Utbetalingtype.UTBETALING), "Utland")
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

    private fun hentCommand(utbetalingstype: Utbetalingtype) =
        AutomatiskAvvisningCommand(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            egenAnsattDao = egenAnsattDao,
            personDao = personDao,
            vergemålDao = vergemålDao,
            godkjenningMediator = godkjenningMediator,
            hendelseId = hendelseId,
            utbetaling = Utbetaling(utbetalingId, 1000, 1000, utbetalingstype),
            kanAvvises = true
        )

    private companion object {
        private const val fødselsnummer = "12345678910"
        private val vedtaksperiodeId = UUID.randomUUID()
        private val hendelseId = UUID.randomUUID()
        private val utbetalingId = UUID.randomUUID()
    }
}

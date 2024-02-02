package no.nav.helse.modell.automatisering

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vergemal.VergemålDao
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AutomatiskAvvisningCommandTest {
    private lateinit var context: CommandContext

    private val vergemålDao = mockk<VergemålDao>(relaxed = true)
    private val personDao = mockk<PersonDao>(relaxed = true)
    private val vedtakDao = mockk<VedtakDao>(relaxed = true)
    private val egenAnsattDao = mockk<EgenAnsattDao>(relaxed = true)
    private val godkjenningMediator = mockk<GodkjenningMediator>(relaxed = true)
    private val sykefraværstilfelle = mockk<Sykefraværstilfelle>(relaxed = true)

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(vergemålDao, personDao, egenAnsattDao, godkjenningMediator)
    }

    @Test
    fun `skal avvise ved vergemål dersom perioden kan avvises`() {
        every { vergemålDao.harVergemål(fødselsnummer) } returns true
        assertAvvisning(lagCommand(kanAvvises = true), "Vergemål")
    }

    @Test
    fun `skal ikke avvise ved vergemål dersom perioden ikke kan avvises`() {
        every { vergemålDao.harVergemål(fødselsnummer) } returns true
        assertIkkeAvvisning(lagCommand(kanAvvises = false))
    }

    @Test
    fun `skal ikke avvise ved vergemål dersom kanAvvises er false`() {
        every { vergemålDao.harVergemål(fødselsnummer) } returns true
        assertIkkeAvvisning(lagCommand(Utbetalingtype.UTBETALING, false))
    }

    @Test
    fun `skal avvise ved utland dersom perioden kan avvises`() {
        every { personDao.finnEnhetId(fødselsnummer) } returns "0393"
        assertAvvisning(lagCommand(kanAvvises = true), "Utland")
    }

    @Test
    fun `skal ikke avvise uavhengig av skjønnsfastsettelse dersom kanAvvises-flagg er false`() {
        every { sykefraværstilfelle.kreverSkjønnsfastsettelse(vedtaksperiodeId) } returns true
        assertIkkeAvvisning(lagCommand(Utbetalingtype.UTBETALING, kanAvvises = false, fødselsnummer = "12345678910"))
    }

    @Test
    fun `skal ikke avvise skjønnsfastsettelse dersom utbetaling ikke er revurdering og fødselsnummer starter på 31`() {
        every { sykefraværstilfelle.kreverSkjønnsfastsettelse(vedtaksperiodeId) } returns true
        assertIkkeAvvisning(lagCommand(Utbetalingtype.UTBETALING, fødselsnummer = "31345678910"))
    }

    @Test
    fun `skal ikke avvise skjønnsfastsettelse dersom perioden ikke kan avvises og fødselsnummer ikke starter på 31`() {
        every { sykefraværstilfelle.kreverSkjønnsfastsettelse(vedtaksperiodeId) } returns true
        assertIkkeAvvisning(lagCommand(Utbetalingtype.REVURDERING, fødselsnummer = "12345678910"))
    }

    @Test
    fun `kan ikke avvise skjønnsfastsettelse for flere arbeidsgivere hvis kanAvvises er false`() {
        every { sykefraværstilfelle.kreverSkjønnsfastsettelse(vedtaksperiodeId) } returns true
        every { vedtakDao.finnInntektskilde(vedtaksperiodeId) } returns Inntektskilde.FLERE_ARBEIDSGIVERE
        assertIkkeAvvisning(lagCommand(Utbetalingtype.REVURDERING, kanAvvises = false, fødselsnummer = "12345678910"))
    }

    @Test
    fun `skal ikke avvise skjønnsfastsettelse dersom det er en arbeidsgiver`() {
        every { sykefraværstilfelle.kreverSkjønnsfastsettelse(vedtaksperiodeId) } returns true
        every { vedtakDao.finnInntektskilde(vedtaksperiodeId) } returns Inntektskilde.EN_ARBEIDSGIVER
        assertIkkeAvvisning(lagCommand(Utbetalingtype.REVURDERING, fødselsnummer = "12345678910"))
    }

    @Test
    fun `skal ikke avvise ved utland dersom perioden ikke kan avvises`() {
        every { personDao.finnEnhetId(fødselsnummer) } returns "0393"
        assertIkkeAvvisning(lagCommand(kanAvvises = false))
    }

    private fun assertAvvisning(command: AutomatiskAvvisningCommand, forventetÅrsak: String) {
        assertTrue(command.execute(context))
        verify (exactly = 1) { godkjenningMediator.automatiskAvvisning(
            any(),
            any(),
            listOf(forventetÅrsak),
            any(),
            any()
        ) }
    }

    private fun assertIkkeAvvisning(command: AutomatiskAvvisningCommand) {
        assertTrue(command.execute(context))
        verify (exactly = 0) { godkjenningMediator.automatiskAvvisning(any(), any(), any()) }
        verify (exactly = 0) { godkjenningMediator.automatiskAvvisning(any(), any(), any(), any(), any()) }
    }

    private fun lagCommand(
        utbetalingstype: Utbetalingtype = Utbetalingtype.UTBETALING,
        kanAvvises: Boolean = true,
        fødselsnummer: String = "12345678910",
    ) =
        AutomatiskAvvisningCommand(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            personDao = personDao,
            vergemålDao = vergemålDao,
            vedtakDao = vedtakDao,
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

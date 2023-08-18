package no.nav.helse.modell.kommando

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.objectMapper
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class UtbetalingsgodkjenningCommandTest {
    private companion object {
        private const val GODKJENT = true
        private const val IDENT = "Z999999"
        private const val EPOST = "saksbehandler@nav.no"
        private val TIDSPUNKT = LocalDateTime.now()
        private val GODKJENNINGSBEHOV_ID = UUID.randomUUID()
        private val vedtaksperiodeId = UUID.randomUUID()
        private val fødselsnummer = "1234"
        private val utbetaling = Utbetaling(UUID.randomUUID(), 1000, 1000, Utbetalingtype.UTBETALING)
    }

    private val godkjenningsbehovJson = """{ "@event_name": "behov" }"""
    private val hendelseDao = mockk<HendelseDao>()
    private lateinit var commandContext: CommandContext
    private lateinit var command: UtbetalingsgodkjenningCommand

    @BeforeEach
    fun setup() {
        clearMocks(hendelseDao)
        commandContext = CommandContext(UUID.randomUUID())
        command = UtbetalingsgodkjenningCommand(
            behandlingId = UUID.randomUUID(),
            hendelseId = UUID.randomUUID(),
            godkjent = GODKJENT,
            saksbehandlerIdent = IDENT,
            epostadresse = EPOST,
            godkjenttidspunkt = TIDSPUNKT,
            årsak = null,
            begrunnelser = null,
            kommentar = null,
            saksbehandleroverstyringer = emptyList(),
            godkjenningsbehovhendelseId = GODKJENNINGSBEHOV_ID,
            hendelseDao = hendelseDao,
            godkjenningMediator = GodkjenningMediator(
                mockk(relaxed = true),
                mockk(relaxed = true),
                mockk(relaxed = true),
                mockk(relaxed = true),
                mockk(relaxed = true),
            ),
            vedtaksperiodeId = vedtaksperiodeId,
            fødselsnummer = fødselsnummer,
            utbetaling = utbetaling,
            sykefraværstilfelle = Sykefraværstilfelle(
                fødselsnummer, 1.januar, listOf(Generasjon(UUID.randomUUID(), UUID.randomUUID(), 1.januar, 31.januar, 1.januar)), emptyList()
            )
        )
    }

    @Test
    fun `løser godkjenningsbehovet`() {
        every { hendelseDao.finnUtbetalingsgodkjenningbehovJson(GODKJENNINGSBEHOV_ID) } returns godkjenningsbehovJson
        assertTrue(command.execute(commandContext))
        assertNotNull(commandContext.meldinger()
            .map(objectMapper::readTree)
            .filter { it["@event_name"].asText() == "behov" }
            .firstOrNull { it["@løsning"].hasNonNull("Godkjenning") })
    }
}

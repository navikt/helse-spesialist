package no.nav.helse.spesialist.application.kommando

import io.mockk.clearMocks
import io.mockk.mockk
import no.nav.helse.db.MeldingDao
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.LøsGodkjenningsbehov
import no.nav.helse.modell.melding.Godkjenningsbehovløsning
import no.nav.helse.modell.melding.UtgåendeHendelse
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning
import no.nav.helse.spesialist.application.Testdata.godkjenningsbehovData
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling
import no.nav.helse.spesialist.testfixtures.jan
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class LøsGodkjenningsbehovTest {
    private companion object {
        private const val GODKJENT = true
        private const val IDENT = "Z999999"
        private const val EPOST = "saksbehandler@nav.no"
        private val TIDSPUNKT = LocalDateTime.now()
        private val GODKJENNINGSBEHOV_ID = UUID.randomUUID()
        private val vedtaksperiodeId = UUID.randomUUID()
        private const val fødselsnummer = "1234"
        private val utbetaling = Utbetaling(UUID.randomUUID(), 1000, 1000, Utbetalingtype.UTBETALING)
    }

    private val godkjenningsbehovJson = """{ "@event_name": "behov" }"""
    private val meldingDao = mockk<MeldingDao>()
    private lateinit var commandContext: CommandContext
    private lateinit var command: LøsGodkjenningsbehov


    private val saksbehandler = Saksbehandlerløsning.Saksbehandler(
        ident = "saksbehandlerident",
        epostadresse = "saksbehandler@nav.no"
    )

    private val beslutter = Saksbehandlerløsning.Saksbehandler(
        ident = "beslutterident",
        epostadresse = "beslutter@nav.no"
    )

    private val observer = object : CommandContextObserver {
        val hendelser = mutableListOf<UtgåendeHendelse>()

        override fun hendelse(hendelse: UtgåendeHendelse) {
            this.hendelser.add(hendelse)
        }
    }

    @BeforeEach
    fun setup() {
        clearMocks(meldingDao)
        commandContext = CommandContext(UUID.randomUUID())
        commandContext.nyObserver(observer)
        command = LøsGodkjenningsbehov(
            utbetaling = utbetaling,
            sykefraværstilfelle = Sykefraværstilfelle(
                fødselsnummer,
                1 jan 2018,
                listOf(LegacyBehandling(UUID.randomUUID(), UUID.randomUUID(), 1 jan 2018, 31 jan 2018, 1 jan 2018))
            ),
            godkjent = GODKJENT,
            godkjenttidspunkt = TIDSPUNKT,
            ident = IDENT,
            epostadresse = EPOST,
            årsak = null,
            begrunnelser = null,
            kommentar = null,
            saksbehandleroverstyringer = emptyList(),
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            godkjenningMediator = GodkjenningMediator(
                mockk(relaxed = true),
            ),
            godkjenningsbehovData = godkjenningsbehovData(
                id = GODKJENNINGSBEHOV_ID,
                fødselsnummer = fødselsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                json = godkjenningsbehovJson
            )
        )
    }

    @Test
    fun `løser godkjenningsbehovet`() {
        assertTrue(command.execute(commandContext))
        val løsning = observer
            .hendelser
            .filterIsInstance<Godkjenningsbehovløsning>()
            .singleOrNull()
        assertNotNull(løsning)
    }
}

package no.nav.helse.mediator.kafka.meldinger

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.kafka.Hendelsefabrikk
import no.nav.helse.modell.command.nyny.CommandContext
import no.nav.helse.modell.overstyring.Dagtype
import no.nav.helse.modell.overstyring.OverstyringDagDto
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.saksbehandler.SaksbehandlerDao
import no.nav.helse.tildeling.ReservasjonDao
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class OverstyringMessageTest {
    companion object {
        private val ID = UUID.randomUUID()
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private const val FØDSELSNUMMER = "12020052345"
        private val OID = UUID.randomUUID()
        private const val NAVN = "Saks Behandler"
        private const val EPOST = "saks.behandler@nav.no"
        private const val ORGNUMMER = "987654321"
        private const val BEGRUNNELSE = "Begrunnelse"
        private val OVERSTYRTE_DAGER = listOf(
            OverstyringDagDto(
                dato = LocalDate.of(2020, 1, 1),
                type = Dagtype.Sykedag,
                grad = 100
            )
        )
        private const val JSON = """{ "this_key_should_exist": "this_value_should_exist" }"""
    }

    private val saksbehandlerDao = mockk<SaksbehandlerDao>(relaxed = true)
    private val reservasjonDao = mockk<ReservasjonDao>(relaxed = true)
    private val overstyringDao = mockk<OverstyringDao>(relaxed = true)
    private val risikovurderingDao = mockk<RisikovurderingDao>(relaxed = true)
    private val hendelsefabrikk = Hendelsefabrikk(
        reservasjonDao = reservasjonDao,
        saksbehandlerDao = saksbehandlerDao,
        overstyringDao = overstyringDao,
        risikovurderingDao = risikovurderingDao,
        personDao = mockk(),
        arbeidsgiverDao = mockk(),
        vedtakDao = mockk(),
        commandContextDao = mockk(),
        snapshotDao = mockk(),
        speilSnapshotRestClient = mockk(),
        oppgaveMediator = mockk(),
        oppgaveDao = mockk(),
        miljøstyrtFeatureToggle = mockk(relaxed = true)
    )

    private val overstyringMessage = hendelsefabrikk.overstyring(
        id = ID,
        fødselsnummer = FØDSELSNUMMER,
        oid = OID,
        navn = NAVN,
        epost = EPOST,
        orgnummer = ORGNUMMER,
        begrunnelse = BEGRUNNELSE,
        overstyrteDager = OVERSTYRTE_DAGER,
        json = JSON
    )

    private lateinit var context: CommandContext

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
    }

    @Test
    fun `Persisterer overstyring`() {
        overstyringMessage.execute(context)

        verify(exactly = 1) { saksbehandlerDao.opprettSaksbehandler(OID, NAVN, EPOST) }
        verify(exactly = 1) { reservasjonDao.reserverPerson(OID, FØDSELSNUMMER) }
        verify(exactly = 1) {
            overstyringDao.persisterOverstyring(
                hendelseId = ID,
                fødselsnummer = FØDSELSNUMMER,
                organisasjonsnummer = ORGNUMMER,
                begrunnelse = BEGRUNNELSE,
                overstyrteDager = OVERSTYRTE_DAGER,
                saksbehandlerRef = OID
            )
        }
    }
}

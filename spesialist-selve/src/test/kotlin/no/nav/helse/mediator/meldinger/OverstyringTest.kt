package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.Hendelsefabrikk
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.overstyring.Dagtype
import no.nav.helse.overstyring.OverstyringDagDto
import no.nav.helse.reservasjon.ReservasjonDao
import no.nav.helse.saksbehandler.SaksbehandlerDao
import no.nav.helse.tildeling.TildelingDao
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class OverstyringTest {
    companion object {
        private val ID = UUID.randomUUID()
        private const val FØDSELSNUMMER = "12020052345"
        private val OID = UUID.randomUUID()
        private const val NAVN = "Saks Behandler"
        private const val IDENT = "Z999999"
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
    private val tildelingDao = mockk<TildelingDao>(relaxed = true)
    private val overstyringDao = mockk<OverstyringDao>(relaxed = true)
    private val risikovurderingDao = mockk<RisikovurderingDao>(relaxed = true)
    private val hendelsefabrikk = Hendelsefabrikk(
        hendelseDao = mockk(),
        personDao = mockk(),
        arbeidsgiverDao = mockk(),
        vedtakDao = mockk(),
        warningDao = mockk(),
        oppgaveDao = mockk(),
        commandContextDao = mockk(),
        snapshotDao = mockk(),
        reservasjonDao = reservasjonDao,
        tildelingDao = tildelingDao,
        saksbehandlerDao = saksbehandlerDao,
        overstyringDao = overstyringDao,
        risikovurderingDao = risikovurderingDao,
        digitalKontaktinformasjonDao = mockk(),
        åpneGosysOppgaverDao = mockk(),
        egenAnsattDao = mockk(),
        speilSnapshotRestClient = mockk(),
        oppgaveMediator = mockk(),
        godkjenningMediator = mockk(relaxed = true),
        automatisering = mockk(relaxed = true),
        arbeidsforholdDao = mockk(relaxed = true),
        utbetalingDao = mockk(relaxed = true),
        opptegnelseDao = mockk(relaxed = true)
    )

    private val overstyringMessage = hendelsefabrikk.overstyring(
        id = ID,
        fødselsnummer = FØDSELSNUMMER,
        oid = OID,
        navn = NAVN,
        ident = IDENT,
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

        verify(exactly = 1) { saksbehandlerDao.opprettSaksbehandler(OID, NAVN, EPOST, IDENT) }
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

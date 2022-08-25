package no.nav.helse.mediator.meldinger

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.mediator.Hendelsefabrikk
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.spesialist.api.oppgave.OppgaveDao
import no.nav.helse.spesialist.api.overstyring.Dagtype
import no.nav.helse.spesialist.api.overstyring.OverstyringDagDto
import no.nav.helse.spesialist.api.reservasjon.ReservasjonDao
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerDao
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
        private val OPPRETTET = LocalDate.now().atStartOfDay()
        private const val JSON = """{ "this_key_should_exist": "this_value_should_exist" }"""
    }

    private val saksbehandlerDao = mockk<SaksbehandlerDao>(relaxed = true)
    private val reservasjonDao = mockk<ReservasjonDao>(relaxed = true)
    private val overstyringDao = mockk<OverstyringDao>(relaxed = true)
    private val oppgaveDao = mockk<OppgaveDao>()

    private val hendelsefabrikk = Hendelsefabrikk(
        dataSource = mockk(relaxed = true),
        oppgaveDao = oppgaveDao,
        reservasjonDao = reservasjonDao,
        saksbehandlerDao = saksbehandlerDao,
        overstyringDao = overstyringDao,
        snapshotClient = mockk(),
        oppgaveMediator = mockk(),
        godkjenningMediator = mockk(),
        automatisering = mockk(),
        overstyringMediator = mockk(relaxed = true),
    )

    private val overstyringAvTidslinjeMessage = hendelsefabrikk.overstyringTidslinje(
        id = ID,
        fødselsnummer = FØDSELSNUMMER,
        oid = OID,
        navn = NAVN,
        ident = IDENT,
        epost = EPOST,
        orgnummer = ORGNUMMER,
        begrunnelse = BEGRUNNELSE,
        overstyrteDager = OVERSTYRTE_DAGER,
        opprettet = OPPRETTET,
        json = JSON
    )

    private lateinit var context: CommandContext

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
    }

    @Test
    fun `Persisterer overstyring av tidslinje`() {
        every { oppgaveDao.finnNyesteVedtaksperiodeIdMedStatus(any(),any(),any(), any()) } returns(null)

        overstyringAvTidslinjeMessage.execute(context)

        verify(exactly = 1) { saksbehandlerDao.opprettSaksbehandler(OID, NAVN, EPOST, IDENT) }
        verify(exactly = 1) { reservasjonDao.reserverPerson(OID, FØDSELSNUMMER) }
        verify(exactly = 1) { overstyringDao.finnEksternHendelseIdFraHendelseId(ID) }
        verify(exactly = 1) {
            overstyringDao.persisterOverstyringTidslinje(
                hendelseId = ID,
                eksternHendelseId = any(), // Vi kan ikke asserte denne fordi den blir generert inne i context
                fødselsnummer = FØDSELSNUMMER,
                organisasjonsnummer = ORGNUMMER,
                begrunnelse = BEGRUNNELSE,
                overstyrteDager = OVERSTYRTE_DAGER,
                saksbehandlerRef = OID,
                tidspunkt = OPPRETTET
            )
        }
    }
}

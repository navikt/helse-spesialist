package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.mediator.Hendelsefabrikk
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.modell.overstyring.OverstyringDao
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
        private const val ORGNUMMER = "987654321"
        private const val BEGRUNNELSE = "Begrunnelse"
        private var OVERSTYRTE_DAGER = listOf(
            OverstyringDagDto(
                dato = LocalDate.of(2020, 1, 1),
                type = Dagtype.Sykedag,
                grad = 100,
                fraType = Dagtype.Feriedag,
                fraGrad = null
            )
        )
        private val OPPRETTET = LocalDate.now().atStartOfDay()
        private const val JSON = """{ "this_key_should_exist": "this_value_should_exist" }"""
    }

    private val saksbehandlerDao = mockk<SaksbehandlerDao>(relaxed = true)
    private val reservasjonDao = mockk<ReservasjonDao>(relaxed = true)
    private val overstyringDao = mockk<OverstyringDao>(relaxed = true)
    private val oppgaveDao = mockk<OppgaveDao>(relaxed = true)

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
        snapshotMediator = mockk(relaxed = true),
        versjonAvKode = "versjonAvKode",
    )

    private val overstyringAvTidslinjeMessage get() = hendelsefabrikk.overstyringTidslinje(
        id = ID,
        fødselsnummer = FØDSELSNUMMER,
        oid = OID,
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
        overstyringAvTidslinjeMessage.execute(context)

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

    @Test
    fun `Leser inn overstyring av tidslinje med arbeidsdag`() {
        OVERSTYRTE_DAGER = listOf(
            OverstyringDagDto(
                dato = LocalDate.of(2020, 1, 1),
                type = Dagtype.Arbeidsdag,
                grad = 100,
                fraType = Dagtype.Sykedag,
                fraGrad = null
            )
        )
        overstyringAvTidslinjeMessage.execute(context)

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

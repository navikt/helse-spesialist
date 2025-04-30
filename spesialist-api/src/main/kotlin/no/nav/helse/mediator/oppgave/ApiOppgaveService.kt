package no.nav.helse.mediator.oppgave

import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.OppgavesorteringForDatabase
import no.nav.helse.db.SorteringsnøkkelForDatabase
import no.nav.helse.mediator.TilgangskontrollørForApi
import no.nav.helse.mediator.oppgave.OppgaveMapper.tilApiversjon
import no.nav.helse.mediator.oppgave.OppgaveMapper.tilBehandledeOppgaver
import no.nav.helse.mediator.oppgave.OppgaveMapper.tilDatabaseversjon
import no.nav.helse.mediator.oppgave.OppgaveMapper.tilEgenskaperForVisning
import no.nav.helse.mediator.oppgave.OppgaveMapper.tilOppgaverTilBehandling
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.spesialist.api.bootstrap.Tilgangsgrupper
import no.nav.helse.spesialist.api.graphql.schema.ApiAntallOppgaver
import no.nav.helse.spesialist.api.graphql.schema.ApiBehandledeOppgaver
import no.nav.helse.spesialist.api.graphql.schema.ApiFiltrering
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgaveegenskap
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgaverTilBehandling
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgavesortering
import no.nav.helse.spesialist.api.graphql.schema.ApiSorteringsnokkel
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.domain.legacy.LegacySaksbehandler
import java.time.LocalDate
import java.util.UUID

class ApiOppgaveService(
    private val oppgaveDao: OppgaveDao,
    private val tilgangsgrupper: Tilgangsgrupper,
    private val oppgaveService: OppgaveService,
) {
    fun oppgaver(
        saksbehandlerFraApi: SaksbehandlerFraApi,
        offset: Int,
        limit: Int,
        sortering: List<ApiOppgavesortering>,
        filtrering: ApiFiltrering,
    ): ApiOppgaverTilBehandling {
        val saksbehandler = saksbehandlerFraApi.tilSaksbehandler()
        val egenskaperSaksbehandlerIkkeHarTilgangTil =
            Egenskap
                .alleTilgangsstyrteEgenskaper
                .filterNot { saksbehandler.harTilgangTil(listOf(it)) }
                .map(Egenskap::toString)

        val alleUkategoriserteEgenskaper =
            Egenskap
                .alleUkategoriserteEgenskaper
                .map(Egenskap::toString)

        val ekskluderteEgenskaper =
            filtrering.ekskluderteEgenskaper?.tilDatabaseversjon()?.map(
                EgenskapForDatabase::toString,
            ) ?: emptyList()

        val egenskaperSomSkalEkskluderes =
            egenskaperSaksbehandlerIkkeHarTilgangTil + ekskluderteEgenskaper + if (filtrering.ingenUkategoriserteEgenskaper) alleUkategoriserteEgenskaper else emptyList()

        val filtreringer =
            OppgaveDao.Filtreringer(
                filtrering.egenskaper
                    .groupBy { it.kategori }
                    .map { it.key.tilDatabaseversjon() to it.value.tilDatabaseversjon() }
                    .toMap(),
            )

        val oppgaver =
            oppgaveDao
                .finnOppgaverForVisning(
                    ekskluderEgenskaper = egenskaperSomSkalEkskluderes,
                    saksbehandlerOid = saksbehandler.oid(),
                    offset = offset,
                    limit = limit,
                    sortering = sortering.tilOppgavesorteringForDatabase(),
                    egneSakerPåVent = filtrering.egneSakerPaVent,
                    egneSaker = filtrering.egneSaker,
                    tildelt = filtrering.tildelt,
                    filtreringer = filtreringer,
                )
        return ApiOppgaverTilBehandling(
            oppgaver = oppgaver.tilOppgaverTilBehandling(),
            totaltAntallOppgaver = if (oppgaver.isEmpty()) 0 else oppgaver.first().filtrertAntall,
        )
    }

    fun antallOppgaver(saksbehandlerFraApi: SaksbehandlerFraApi): ApiAntallOppgaver {
        val saksbehandler = saksbehandlerFraApi.tilSaksbehandler()
        val antallOppgaver = oppgaveDao.finnAntallOppgaver(saksbehandlerOid = saksbehandler.oid())
        return antallOppgaver.tilApiversjon()
    }

    fun behandledeOppgaver(
        saksbehandlerFraApi: SaksbehandlerFraApi,
        offset: Int,
        limit: Int,
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate = LocalDate.now(),
    ): ApiBehandledeOppgaver {
        val saksbehandler = saksbehandlerFraApi.tilSaksbehandler()
        val behandledeOppgaver =
            oppgaveDao.finnBehandledeOppgaver(
                behandletAvOid = saksbehandler.oid(),
                offset = offset,
                limit = limit,
                fom = fom,
                tom = tom,
            )
        return ApiBehandledeOppgaver(
            oppgaver = behandledeOppgaver.tilBehandledeOppgaver(),
            totaltAntallOppgaver = if (behandledeOppgaver.isEmpty()) 0 else behandledeOppgaver.first().filtrertAntall,
        )
    }

    fun hentEgenskaper(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): List<ApiOppgaveegenskap> {
        val egenskaper =
            oppgaveDao.finnEgenskaper(
                vedtaksperiodeId = vedtaksperiodeId,
                utbetalingId = utbetalingId,
            )

        return egenskaper?.tilEgenskaperForVisning() ?: emptyList()
    }

    private fun SaksbehandlerFraApi.tilSaksbehandler() =
        LegacySaksbehandler(
            epostadresse = epost,
            oid = oid,
            navn = navn,
            ident = ident,
            tilgangskontroll = TilgangskontrollørForApi(grupper, tilgangsgrupper),
        )

    private fun List<ApiOppgavesortering>.tilOppgavesorteringForDatabase() =
        map {
            when (it.nokkel) {
                ApiSorteringsnokkel.TILDELT_TIL ->
                    OppgavesorteringForDatabase(
                        SorteringsnøkkelForDatabase.TILDELT_TIL,
                        it.stigende,
                    )

                ApiSorteringsnokkel.OPPRETTET ->
                    OppgavesorteringForDatabase(
                        SorteringsnøkkelForDatabase.OPPRETTET,
                        it.stigende,
                    )

                ApiSorteringsnokkel.SOKNAD_MOTTATT ->
                    OppgavesorteringForDatabase(
                        SorteringsnøkkelForDatabase.SØKNAD_MOTTATT,
                        it.stigende,
                    )

                ApiSorteringsnokkel.TIDSFRIST ->
                    OppgavesorteringForDatabase(
                        SorteringsnøkkelForDatabase.TIDSFRIST,
                        it.stigende,
                    )
            }
        }

    fun sendTilBeslutter(
        oppgaveId: Long,
        beslutter: LegacySaksbehandler?,
    ) {
        oppgaveService.oppgave(oppgaveId) {
            sendTilBeslutter(beslutter)
        }
    }

    fun sendIRetur(
        oppgaveId: Long,
        opprinneligLegacySaksbehandler: LegacySaksbehandler,
    ) {
        oppgaveService.oppgave(oppgaveId) {
            sendIRetur(opprinneligLegacySaksbehandler)
        }
    }

    fun venterPåSaksbehandler(oppgaveId: Long): Boolean = oppgaveDao.venterPåSaksbehandler(oppgaveId)

    fun spleisBehandlingId(oppgaveId: Long): UUID = oppgaveDao.finnSpleisBehandlingId(oppgaveId)

    fun fødselsnummer(oppgaveId: Long): String = oppgaveDao.finnFødselsnummer(oppgaveId)
}

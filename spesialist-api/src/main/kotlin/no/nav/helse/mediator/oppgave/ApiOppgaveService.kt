package no.nav.helse.mediator.oppgave

import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.OppgaveFraDatabaseForVisning
import no.nav.helse.db.OppgavesorteringForDatabase
import no.nav.helse.db.SorteringsnøkkelForDatabase
import no.nav.helse.mediator.oppgave.OppgaveMapper.tilApiversjon
import no.nav.helse.mediator.oppgave.OppgaveMapper.tilBehandledeOppgaver
import no.nav.helse.mediator.oppgave.OppgaveMapper.tilDatabaseversjon
import no.nav.helse.mediator.oppgave.OppgaveMapper.tilEgenskaperForVisning
import no.nav.helse.mediator.oppgave.OppgaveMapper.tilOppgaverTilBehandling
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.spesialist.api.graphql.schema.ApiAntallOppgaver
import no.nav.helse.spesialist.api.graphql.schema.ApiBehandledeOppgaver
import no.nav.helse.spesialist.api.graphql.schema.ApiEgenskap
import no.nav.helse.spesialist.api.graphql.schema.ApiFiltrering
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgaveegenskap
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgaverTilBehandling
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgavesortering
import no.nav.helse.spesialist.api.graphql.schema.ApiSorteringsnokkel
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.legacy.SaksbehandlerWrapper
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import java.time.LocalDate
import java.util.UUID

class ApiOppgaveService(
    private val oppgaveDao: OppgaveDao,
    private val oppgaveService: OppgaveService,
) {
    fun oppgaver(
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        offset: Int,
        limit: Int,
        sortering: List<ApiOppgavesortering>,
        filtrering: ApiFiltrering,
    ): ApiOppgaverTilBehandling =
        oppgaveDao
            .finnOppgaverForVisning(
                ekskluderEgenskaper =
                    egenskaperSaksbehandlerIkkeHarTilgangTil(saksbehandler, tilgangsgrupper)
                        .plus(filtrering.tilEkskluderteEgenskaper())
                        .map(Egenskap::toString),
                saksbehandlerOid = saksbehandler.id().value,
                offset = offset,
                limit = limit,
                sortering = sortering.tilOppgavesorteringForDatabase(),
                egneSakerPåVent = filtrering.egneSakerPaVent,
                egneSaker = filtrering.egneSaker,
                tildelt = filtrering.tildelt,
                grupperteFiltrerteEgenskaper =
                    filtrering.egenskaper
                        .groupBy { it.kategori }
                        .map { it.key.tilDatabaseversjon() to it.value.tilDatabaseversjon() }
                        .toMap(),
            ).tilApiOppgaverTilBehandling()

    private fun ApiFiltrering.tilEkskluderteEgenskaper(): List<Egenskap> =
        buildList {
            addAll(ekskluderteEgenskaper.orEmpty().map { it.egenskap }.map { it.tilEgenskap() })
            if (ingenUkategoriserteEgenskaper) {
                addAll(Egenskap.entries.filter { it.kategori == Egenskap.Kategori.Ukategorisert })
            }
        }

    private fun ApiEgenskap.tilEgenskap(): Egenskap =
        when (this) {
            ApiEgenskap.RISK_QA -> Egenskap.RISK_QA
            ApiEgenskap.FORTROLIG_ADRESSE -> Egenskap.FORTROLIG_ADRESSE
            ApiEgenskap.STRENGT_FORTROLIG_ADRESSE -> Egenskap.STRENGT_FORTROLIG_ADRESSE
            ApiEgenskap.EGEN_ANSATT -> Egenskap.EGEN_ANSATT
            ApiEgenskap.BESLUTTER -> Egenskap.BESLUTTER
            ApiEgenskap.SPESIALSAK -> Egenskap.SPESIALSAK
            ApiEgenskap.REVURDERING -> Egenskap.REVURDERING
            ApiEgenskap.SOKNAD -> Egenskap.SØKNAD
            ApiEgenskap.STIKKPROVE -> Egenskap.STIKKPRØVE
            ApiEgenskap.UTBETALING_TIL_SYKMELDT -> Egenskap.UTBETALING_TIL_SYKMELDT
            ApiEgenskap.DELVIS_REFUSJON -> Egenskap.DELVIS_REFUSJON
            ApiEgenskap.UTBETALING_TIL_ARBEIDSGIVER -> Egenskap.UTBETALING_TIL_ARBEIDSGIVER
            ApiEgenskap.INGEN_UTBETALING -> Egenskap.INGEN_UTBETALING
            ApiEgenskap.EN_ARBEIDSGIVER -> Egenskap.EN_ARBEIDSGIVER
            ApiEgenskap.FLERE_ARBEIDSGIVERE -> Egenskap.FLERE_ARBEIDSGIVERE
            ApiEgenskap.FORLENGELSE -> Egenskap.FORLENGELSE
            ApiEgenskap.FORSTEGANGSBEHANDLING -> Egenskap.FORSTEGANGSBEHANDLING
            ApiEgenskap.INFOTRYGDFORLENGELSE -> Egenskap.INFOTRYGDFORLENGELSE
            ApiEgenskap.OVERGANG_FRA_IT -> Egenskap.OVERGANG_FRA_IT
            ApiEgenskap.UTLAND -> Egenskap.UTLAND
            ApiEgenskap.HASTER -> Egenskap.HASTER
            ApiEgenskap.RETUR -> Egenskap.RETUR
            ApiEgenskap.VERGEMAL -> Egenskap.VERGEMÅL
            ApiEgenskap.SKJONNSFASTSETTELSE -> Egenskap.SKJØNNSFASTSETTELSE
            ApiEgenskap.PA_VENT -> Egenskap.PÅ_VENT
            ApiEgenskap.TILBAKEDATERT -> Egenskap.TILBAKEDATERT
            ApiEgenskap.GOSYS -> Egenskap.GOSYS
            ApiEgenskap.MANGLER_IM -> Egenskap.MANGLER_IM
            ApiEgenskap.MEDLEMSKAP -> Egenskap.MEDLEMSKAP
            ApiEgenskap.GRUNNBELOPSREGULERING -> Egenskap.GRUNNBELØPSREGULERING
            ApiEgenskap.SELVSTENDIG_NAERINGSDRIVENDE -> Egenskap.SELVSTENDIG_NÆRINGSDRIVENDE
            ApiEgenskap.ARBEIDSTAKER -> Egenskap.ARBEIDSTAKER
        }

    fun tildelteOppgaver(
        innloggetSaksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        oppslåttSaksbehandler: Saksbehandler,
        offset: Int,
        limit: Int,
    ): ApiOppgaverTilBehandling =
        oppgaveDao
            .finnTildelteOppgaver(
                saksbehandlerOid = oppslåttSaksbehandler.id().value,
                ekskluderEgenskaper =
                    egenskaperSaksbehandlerIkkeHarTilgangTil(innloggetSaksbehandler, tilgangsgrupper)
                        .map(Egenskap::toString),
                offset = offset,
                limit = limit,
            ).tilApiOppgaverTilBehandling()

    private fun egenskaperSaksbehandlerIkkeHarTilgangTil(
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
    ): List<Egenskap> =
        Egenskap.entries.filterNot {
            Oppgave.harTilgangTilEgenskap(
                egenskap = it,
                saksbehandler = saksbehandler,
                saksbehandlerTilgangsgrupper = tilgangsgrupper,
            )
        }

    private fun List<OppgaveFraDatabaseForVisning>.tilApiOppgaverTilBehandling(): ApiOppgaverTilBehandling =
        ApiOppgaverTilBehandling(
            oppgaver = tilOppgaverTilBehandling(),
            totaltAntallOppgaver = if (isEmpty()) 0 else this.first().filtrertAntall,
        )

    fun antallOppgaver(saksbehandler: Saksbehandler): ApiAntallOppgaver {
        val antallOppgaver = oppgaveDao.finnAntallOppgaver(saksbehandlerOid = saksbehandler.id().value)
        return antallOppgaver.tilApiversjon()
    }

    fun behandledeOppgaver(
        saksbehandler: Saksbehandler,
        offset: Int,
        limit: Int,
        fom: LocalDate,
        tom: LocalDate,
    ): ApiBehandledeOppgaver {
        val behandledeOppgaver =
            oppgaveDao.finnBehandledeOppgaver(
                behandletAvOid = saksbehandler.id().value,
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
        beslutter: SaksbehandlerWrapper?,
    ) {
        oppgaveService.oppgave(oppgaveId) {
            sendTilBeslutter(beslutter)
        }
    }

    fun sendIRetur(
        oppgaveId: Long,
        opprinneligSaksbehandlerWrapper: SaksbehandlerWrapper,
    ) {
        oppgaveService.oppgave(oppgaveId) {
            sendIRetur(opprinneligSaksbehandlerWrapper)
        }
    }

    fun venterPåSaksbehandler(oppgaveId: Long): Boolean = oppgaveDao.venterPåSaksbehandler(oppgaveId)

    fun spleisBehandlingId(oppgaveId: Long): UUID = oppgaveDao.finnSpleisBehandlingId(oppgaveId)

    fun fødselsnummer(oppgaveId: Long): String = oppgaveDao.finnFødselsnummer(oppgaveId)
}

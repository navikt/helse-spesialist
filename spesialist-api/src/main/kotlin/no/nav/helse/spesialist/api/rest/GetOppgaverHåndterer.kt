package no.nav.helse.spesialist.api.rest

import io.ktor.http.Parameters
import io.ktor.util.flattenEntries
import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.OppgaveFraDatabaseForVisning
import no.nav.helse.db.OppgavesorteringForDatabase
import no.nav.helse.db.SessionContext
import no.nav.helse.db.SorteringsnøkkelForDatabase
import no.nav.helse.mediator.oppgave.OppgaveMapper.tilOppgaverTilBehandling
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.spesialist.api.graphql.schema.ApiEgenskap
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgaverTilBehandling
import no.nav.helse.spesialist.api.graphql.schema.ApiSorteringrekkefolge
import no.nav.helse.spesialist.api.graphql.schema.ApiSorteringsnokkel
import no.nav.helse.spesialist.application.logg.sikkerlogg
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import kotlin.reflect.typeOf
import kotlin.time.measureTimedValue

class GetOppgaverHåndterer : GetHåndterer<GetOppgaverHåndterer.URLParametre, ApiOppgaverTilBehandling> {
    override val urlPath = "oppgaver?{args}"

    data class URLParametre(
        val pageNumber: Int?,
        val pageSize: Int?,
        val sortBy: ApiSorteringsnokkel?,
        val sortDirection: ApiSorteringrekkefolge?,
        val minstEnAvEgenskapene: List<String>?, // Kommaseparerte
        val ingenAvEgenskapene: String?, // Kommaseparert
        val erTildelt: Boolean?,
        val erPaaVent: Boolean?,
        val tildeltTilIdent: String?,
    )

    override fun extractParametre(
        pathParameters: Parameters,
        queryParameters: Parameters,
    ) = URLParametre(
        pageNumber = queryParameters["pageNumber"]?.toIntOrNull(),
        pageSize = queryParameters["pageSize"]?.toIntOrNull(),
        sortBy = queryParameters["sortBy"]?.let { enumValueOf<ApiSorteringsnokkel>(it) },
        sortDirection = queryParameters["sortDirection"]?.let { enumValueOf<ApiSorteringrekkefolge>(it) },
        minstEnAvEgenskapene = queryParameters.getList("minstEnAvEgenskapene"),
        ingenAvEgenskapene = queryParameters["ingenAvEgenskapene"],
        erTildelt = queryParameters["erTildelt"]?.toBooleanStrictOrNull(),
        erPaaVent = queryParameters["erPaaVent"]?.toBooleanStrictOrNull(),
        tildeltTilIdent = queryParameters["tildeltTilIdent"],
    )

    private fun Parameters.getList(name: String): List<String> {
        val lowercaseName = name.lowercase()
        return flattenEntries()
            .asSequence()
            .map { (name, value) -> name.lowercase() to value }
            .filter { (name, _) -> name.startsWith("$lowercaseName[") && name.endsWith("]") }
            .map { (name, value) -> name.removeSurrounding("${lowercaseName.lowercase()}[", "]").toInt() to value }
            .sortedBy { (number, _) -> number }
            .map { (_, value) -> value }
            .toList()
    }

    override fun håndter(
        urlParametre: URLParametre,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
    ): RestResponse<ApiOppgaverTilBehandling> {
        sikkerlogg.debug("Henter OppgaverTilBehandling for ${saksbehandler.navn}")
        val (oppgaver, tid) =
            measureTimedValue {
                oppgaverTilBehandling(saksbehandler, tilgangsgrupper, urlParametre, transaksjon)
            }
        sikkerlogg.debug("Query OppgaverTilBehandling er ferdig etter ${tid.inWholeMilliseconds} ms")
        val grense = 5000
        if (tid.inWholeMilliseconds > grense) {
            sikkerlogg.info("Det tok over $grense ms å hente oppgaver med disse queryparametrene: $urlParametre")
        }

        return RestResponse.ok(oppgaver)
    }

    private fun oppgaverTilBehandling(
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        urlParametre: URLParametre,
        transaksjon: SessionContext,
    ): ApiOppgaverTilBehandling {
        val pageSize = urlParametre.pageSize ?: 50
        val pageNumber = urlParametre.pageNumber ?: 1
        return transaksjon.oppgaveDao
            .finnOppgaverForVisning(
                ekskluderEgenskaper =
                    egenskaperSaksbehandlerIkkeSkalFåOppIOversikten(
                        saksbehandler,
                        tilgangsgrupper,
                    ).plus(tilEgenskaper(urlParametre.ingenAvEgenskapene))
                        .map(Egenskap::toString),
                saksbehandlerOid = saksbehandler.id().value,
                offset = (pageNumber - 1) * pageSize,
                limit = pageSize,
                sortering = tilOppgavesorteringForDatabase(urlParametre.sortBy, urlParametre.sortDirection),
                egneSakerPåVent = urlParametre.tildeltTilIdent == saksbehandler.ident && urlParametre.erPaaVent == true,
                egneSaker = urlParametre.tildeltTilIdent == saksbehandler.ident && urlParametre.erPaaVent == false,
                tildelt = urlParametre.erTildelt,
                grupperteFiltrerteEgenskaper = tilGruppertMap(urlParametre.minstEnAvEgenskapene),
            ).tilApiOppgaverTilBehandling()
    }

    private fun tilGruppertMap(minstEnAvEgenskapene: List<String>?): Map<Egenskap.Kategori, List<EgenskapForDatabase>> =
        minstEnAvEgenskapene
            .orEmpty()
            .flatMap { it.split(',') }
            .map { enumValueOf<ApiEgenskap>(it).tilEgenskap() }
            .groupBy { it.kategori }
            .map { it.key to it.value.tilDatabaseversjon() }
            .toMap()

    private fun tilEgenskaper(excludedEgenskaper: String?): List<Egenskap> =
        excludedEgenskaper
            ?.takeUnless { it.isEmpty() }
            ?.split(',')
            ?.toList()
            .orEmpty()
            .map { enumValueOf<ApiEgenskap>(it).tilEgenskap() }

    internal fun List<Egenskap>.tilDatabaseversjon(): List<EgenskapForDatabase> = this.map { it.tilDatabaseversjon() }

    private fun Egenskap.tilDatabaseversjon() =
        when (this) {
            Egenskap.RISK_QA -> EgenskapForDatabase.RISK_QA
            Egenskap.FORTROLIG_ADRESSE -> EgenskapForDatabase.FORTROLIG_ADRESSE
            Egenskap.STRENGT_FORTROLIG_ADRESSE -> EgenskapForDatabase.STRENGT_FORTROLIG_ADRESSE
            Egenskap.EGEN_ANSATT -> EgenskapForDatabase.EGEN_ANSATT
            Egenskap.BESLUTTER -> EgenskapForDatabase.BESLUTTER
            Egenskap.SPESIALSAK -> EgenskapForDatabase.SPESIALSAK
            Egenskap.REVURDERING -> EgenskapForDatabase.REVURDERING
            Egenskap.SØKNAD -> EgenskapForDatabase.SØKNAD
            Egenskap.STIKKPRØVE -> EgenskapForDatabase.STIKKPRØVE
            Egenskap.UTBETALING_TIL_SYKMELDT -> EgenskapForDatabase.UTBETALING_TIL_SYKMELDT
            Egenskap.DELVIS_REFUSJON -> EgenskapForDatabase.DELVIS_REFUSJON
            Egenskap.UTBETALING_TIL_ARBEIDSGIVER -> EgenskapForDatabase.UTBETALING_TIL_ARBEIDSGIVER
            Egenskap.INGEN_UTBETALING -> EgenskapForDatabase.INGEN_UTBETALING
            Egenskap.EN_ARBEIDSGIVER -> EgenskapForDatabase.EN_ARBEIDSGIVER
            Egenskap.FLERE_ARBEIDSGIVERE -> EgenskapForDatabase.FLERE_ARBEIDSGIVERE
            Egenskap.FORLENGELSE -> EgenskapForDatabase.FORLENGELSE
            Egenskap.FORSTEGANGSBEHANDLING -> EgenskapForDatabase.FORSTEGANGSBEHANDLING
            Egenskap.INFOTRYGDFORLENGELSE -> EgenskapForDatabase.INFOTRYGDFORLENGELSE
            Egenskap.OVERGANG_FRA_IT -> EgenskapForDatabase.OVERGANG_FRA_IT
            Egenskap.UTLAND -> EgenskapForDatabase.UTLAND
            Egenskap.HASTER -> EgenskapForDatabase.HASTER
            Egenskap.RETUR -> EgenskapForDatabase.RETUR
            Egenskap.VERGEMÅL -> EgenskapForDatabase.VERGEMÅL
            Egenskap.SKJØNNSFASTSETTELSE -> EgenskapForDatabase.SKJØNNSFASTSETTELSE
            Egenskap.PÅ_VENT -> EgenskapForDatabase.PÅ_VENT
            Egenskap.TILBAKEDATERT -> EgenskapForDatabase.TILBAKEDATERT
            Egenskap.GOSYS -> EgenskapForDatabase.GOSYS
            Egenskap.MANGLER_IM -> EgenskapForDatabase.MANGLER_IM
            Egenskap.MEDLEMSKAP -> EgenskapForDatabase.MEDLEMSKAP
            Egenskap.GRUNNBELØPSREGULERING -> EgenskapForDatabase.GRUNNBELØPSREGULERING
            Egenskap.SELVSTENDIG_NÆRINGSDRIVENDE -> EgenskapForDatabase.SELVSTENDIG_NÆRINGSDRIVENDE
            Egenskap.ARBEIDSTAKER -> EgenskapForDatabase.ARBEIDSTAKER
        }

    private fun List<OppgaveFraDatabaseForVisning>.tilApiOppgaverTilBehandling(): ApiOppgaverTilBehandling =
        ApiOppgaverTilBehandling(
            oppgaver = tilOppgaverTilBehandling(),
            totaltAntallOppgaver = if (isEmpty()) 0 else this.first().filtrertAntall,
        )

    private fun tilOppgavesorteringForDatabase(
        sorteringsnokkel: ApiSorteringsnokkel?,
        rekkefølge: ApiSorteringrekkefolge?,
    ): List<OppgavesorteringForDatabase> =
        listOf(
            OppgavesorteringForDatabase(
                nøkkel =
                    when (sorteringsnokkel) {
                        null, ApiSorteringsnokkel.OPPRETTET -> SorteringsnøkkelForDatabase.OPPRETTET
                        ApiSorteringsnokkel.TILDELT_TIL -> SorteringsnøkkelForDatabase.TILDELT_TIL
                        ApiSorteringsnokkel.SOKNAD_MOTTATT -> SorteringsnøkkelForDatabase.SØKNAD_MOTTATT
                        ApiSorteringsnokkel.TIDSFRIST -> SorteringsnøkkelForDatabase.TIDSFRIST
                    },
                stigende =
                    when (rekkefølge) {
                        ApiSorteringrekkefolge.ASCENDING, null -> true
                        ApiSorteringrekkefolge.DESCENDING -> false
                    },
            ),
        )

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

    private fun egenskaperSaksbehandlerIkkeSkalFåOppIOversikten(
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
    ): List<Egenskap> =
        Egenskap.entries.filterNot {
            Oppgave.harTilgangTilEgenskap(
                egenskap = it,
                saksbehandler = saksbehandler,
                saksbehandlerTilgangsgrupper = tilgangsgrupper,
            ) &&
                when (it) {
                    Egenskap.BESLUTTER -> Tilgangsgruppe.BESLUTTER in tilgangsgrupper
                    Egenskap.STIKKPRØVE -> Tilgangsgruppe.STIKKPRØVE in tilgangsgrupper
                    else -> true
                }
        }

    override val urlParametersClass = URLParametre::class

    override val responseBodyType = typeOf<ApiOppgaverTilBehandling>()
}

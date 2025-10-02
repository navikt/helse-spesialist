package no.nav.helse.spesialist.api.rest

import io.ktor.http.Parameters
import io.ktor.util.flattenEntries
import no.nav.helse.db.SessionContext
import no.nav.helse.db.SorteringsnøkkelForDatabase
import no.nav.helse.db.Sorteringsrekkefølge
import no.nav.helse.mediator.oppgave.OppgaveMapper.tilApiversjon
import no.nav.helse.mediator.oppgave.OppgaveRepository
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.spesialist.api.graphql.schema.ApiEgenskap
import no.nav.helse.spesialist.api.graphql.schema.ApiKommentar
import no.nav.helse.spesialist.api.graphql.schema.ApiPaVentInfo
import no.nav.helse.spesialist.api.graphql.schema.ApiPersonnavn
import no.nav.helse.spesialist.api.graphql.schema.ApiSorteringsnokkel
import no.nav.helse.spesialist.api.graphql.schema.ApiSorteringsrekkefolge
import no.nav.helse.spesialist.api.graphql.schema.ApiTildeling
import no.nav.helse.spesialist.application.logg.sikkerlogg
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import java.util.UUID
import kotlin.reflect.typeOf
import kotlin.time.measureTimedValue

class GetOppgaverHåndterer : GetHåndterer<GetOppgaverHåndterer.URLParametre, ApiOppgaveProjeksjonSide> {
    override val urlPath = "oppgaver?{args}"

    data class URLParametre(
        val minstEnAvEgenskapene: List<String>?, // Kommaseparerte
        val ingenAvEgenskapene: String?, // Kommaseparert
        val erTildelt: Boolean?,
        val tildeltTilOid: UUID?,
        val erPaaVent: Boolean?,
        val sorterPaa: ApiSorteringsnokkel?,
        val sorteringsrekkefoelge: ApiSorteringsrekkefolge?,
        val sidetall: Int?,
        val sidestoerrelse: Int?,
    )

    override fun extractParametre(
        pathParameters: Parameters,
        queryParameters: Parameters,
    ) = URLParametre(
        minstEnAvEgenskapene = queryParameters.getList("minstEnAvEgenskapene"),
        ingenAvEgenskapene = queryParameters["ingenAvEgenskapene"],
        erTildelt = queryParameters["erTildelt"]?.toBooleanStrictOrNull(),
        tildeltTilOid = queryParameters["tildeltTilOid"]?.let(UUID::fromString),
        erPaaVent = queryParameters["erPaaVent"]?.toBooleanStrictOrNull(),
        sorterPaa = queryParameters["sorterPaa"]?.let { enumValueOf<ApiSorteringsnokkel>(it) },
        sorteringsrekkefoelge = queryParameters["sorteringsrekkefoelge"]?.let { enumValueOf<ApiSorteringsrekkefolge>(it) },
        sidetall = queryParameters["sidetall"]?.toIntOrNull(),
        sidestoerrelse = queryParameters["sidestoerrelse"]?.toIntOrNull(),
    )

    override fun håndter(
        urlParametre: URLParametre,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
    ): RestResponse<ApiOppgaveProjeksjonSide> {
        sikkerlogg.debug("Henter OppgaverTilBehandling for ${saksbehandler.navn}")
        val (oppgaver, tid) =
            measureTimedValue {
                transaksjon.oppgaveRepository
                    .finnOppgaveProjeksjoner(
                        minstEnAvEgenskapene =
                            urlParametre.minstEnAvEgenskapene
                                .orEmpty()
                                .map { it.split(',').map { enumValueOf<ApiEgenskap>(it).tilEgenskap() }.toSet() },
                        ingenAvEgenskapene =
                            egenskaperSaksbehandlerIkkeSkalFåOppIOversikten(
                                saksbehandler,
                                tilgangsgrupper,
                            ).plus(tilEgenskaper(urlParametre.ingenAvEgenskapene)).toSet(),
                        erTildelt = urlParametre.erTildelt,
                        tildeltTilOid = urlParametre.tildeltTilOid?.let(::SaksbehandlerOid),
                        erPåVent = urlParametre.erPaaVent,
                        ikkeSendtTilBeslutterAvOid = saksbehandler.id(),
                        sorterPå =
                            when (urlParametre.sorterPaa) {
                                null,
                                ApiSorteringsnokkel.OPPRETTET,
                                -> SorteringsnøkkelForDatabase.OPPRETTET

                                ApiSorteringsnokkel.TILDELT_TIL -> SorteringsnøkkelForDatabase.TILDELT_TIL
                                ApiSorteringsnokkel.SOKNAD_MOTTATT -> SorteringsnøkkelForDatabase.SØKNAD_MOTTATT
                                ApiSorteringsnokkel.TIDSFRIST -> SorteringsnøkkelForDatabase.TIDSFRIST
                            },
                        sorteringsrekkefølge =
                            when (urlParametre.sorteringsrekkefoelge) {
                                null,
                                ApiSorteringsrekkefolge.STIGENDE,
                                -> Sorteringsrekkefølge.STIGENDE

                                ApiSorteringsrekkefolge.SYNKENDE -> Sorteringsrekkefølge.SYNKENDE
                            },
                        sidetall = urlParametre.sidetall?.takeUnless { it < 1 } ?: 1,
                        sidestørrelse = urlParametre.sidestoerrelse?.takeUnless { it < 1 } ?: 10,
                    ).tilApiType(transaksjon)
            }
        sikkerlogg.debug("Query OppgaverTilBehandling er ferdig etter ${tid.inWholeMilliseconds} ms")
        val grense = 5000
        if (tid.inWholeMilliseconds > grense) {
            sikkerlogg.info("Det tok over $grense ms å hente oppgaver med disse queryparametrene: $urlParametre")
        }

        return RestResponse.ok(oppgaver)
    }

    private fun tilEgenskaper(excludedEgenskaper: String?): List<Egenskap> =
        excludedEgenskaper
            ?.takeUnless { it.isEmpty() }
            ?.split(',')
            ?.toList()
            .orEmpty()
            .map { enumValueOf<ApiEgenskap>(it).tilEgenskap() }

    private fun OppgaveRepository.Side<OppgaveRepository.OppgaveProjeksjon>.tilApiType(transaksjon: SessionContext): ApiOppgaveProjeksjonSide {
        val saksbehandlere =
            transaksjon.saksbehandlerRepository
                .finnAlle(elementer.mapNotNull { it.tildeltTilOid }.toSet())
                .associateBy { it.id() }
        return ApiOppgaveProjeksjonSide(
            totaltAntall = totaltAntall,
            sidetall = sidetall,
            sidestoerrelse = sidestørrelse,
            elementer =
                elementer.map { oppgave ->
                    ApiOppgaveProjeksjon(
                        id = oppgave.id.toString(),
                        aktorId = oppgave.aktørId,
                        navn =
                            ApiPersonnavn(
                                fornavn = oppgave.navn.fornavn,
                                etternavn = oppgave.navn.etternavn,
                                mellomnavn = oppgave.navn.mellomnavn,
                            ),
                        egenskaper =
                            oppgave.egenskaper
                                .map { egenskap -> egenskap.tilApiversjon() }
                                .sortedBy { it.name },
                        tildeling =
                            oppgave.tildeltTilOid
                                ?.let { tildeltTilOid -> saksbehandlere[tildeltTilOid] }
                                ?.let { tildelt ->
                                    ApiTildeling(
                                        navn = tildelt.navn,
                                        epost = tildelt.epost,
                                        oid = tildelt.id().value,
                                    )
                                },
                        opprettetTidspunkt = oppgave.opprettetTidspunkt,
                        opprinneligSoeknadstidspunkt = oppgave.opprinneligSøknadstidspunkt,
                        paVentInfo =
                            oppgave.påVentInfo?.let { påVentInfo ->
                                ApiPaVentInfo(
                                    arsaker = påVentInfo.årsaker,
                                    tekst = påVentInfo.tekst,
                                    dialogRef = påVentInfo.dialogRef.toInt(),
                                    saksbehandler = påVentInfo.saksbehandler,
                                    opprettet = påVentInfo.opprettet,
                                    tidsfrist = påVentInfo.tidsfrist,
                                    kommentarer =
                                        påVentInfo.kommentarer.map {
                                            ApiKommentar(
                                                id = it.id,
                                                tekst = it.tekst,
                                                opprettet = it.opprettet,
                                                saksbehandlerident = it.saksbehandlerident,
                                                feilregistrert_tidspunkt = null,
                                            )
                                        },
                                )
                            },
                    )
                },
        )
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

    override val urlParametersClass = URLParametre::class

    override val responseBodyType = typeOf<ApiOppgaveProjeksjonSide>()
}

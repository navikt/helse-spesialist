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
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgaveSorteringsfelt
import no.nav.helse.spesialist.api.graphql.schema.ApiPersonnavn
import no.nav.helse.spesialist.api.graphql.schema.ApiSorteringsrekkefolge
import no.nav.helse.spesialist.api.graphql.schema.ApiTildeling
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.domain.Dialog
import no.nav.helse.spesialist.domain.DialogId
import no.nav.helse.spesialist.domain.PåVent
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import java.time.ZoneId
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
        val sorteringsfelt: ApiOppgaveSorteringsfelt?,
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
        sorteringsfelt = queryParameters["sorteringsfelt"]?.let { enumValueOf<ApiOppgaveSorteringsfelt>(it) },
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
        val (finnOppgaveProjeksjoner, tidsbruk) =
            measureTimedValue {
                transaksjon.oppgaveRepository
                    .finnOppgaveProjeksjoner(
                        minstEnAvEgenskapene =
                            urlParametre.minstEnAvEgenskapene
                                .orEmpty()
                                .map { it.tilEgenskaper() },
                        ingenAvEgenskapene =
                            Egenskap.entries
                                .filterNot { it.skalDukkeOppFor(saksbehandler, tilgangsgrupper) }
                                .plus(urlParametre.ingenAvEgenskapene.tilEgenskaper())
                                .toSet(),
                        erTildelt = urlParametre.erTildelt,
                        tildeltTilOid = urlParametre.tildeltTilOid?.let(::SaksbehandlerOid),
                        erPåVent = urlParametre.erPaaVent,
                        ikkeSendtTilBeslutterAvOid = saksbehandler.id(),
                        sorterPå =
                            when (urlParametre.sorteringsfelt) {
                                null,
                                ApiOppgaveSorteringsfelt.opprettetTidspunkt,
                                -> SorteringsnøkkelForDatabase.OPPRETTET

                                ApiOppgaveSorteringsfelt.tildeling -> SorteringsnøkkelForDatabase.TILDELT_TIL
                                ApiOppgaveSorteringsfelt.opprinneligSoeknadstidspunkt -> SorteringsnøkkelForDatabase.SØKNAD_MOTTATT
                                ApiOppgaveSorteringsfelt.paVentInfo_tidsfrist -> SorteringsnøkkelForDatabase.TIDSFRIST
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
                    )
            }
        logg.info("Hentet oppgaver på ${tidsbruk.inWholeMilliseconds} ms.\n Bruke følgende filtre: $urlParametre")
        return RestResponse.ok(
            finnOppgaveProjeksjoner.tilApiType(transaksjon),
        )
    }

    private fun String?.tilEgenskaper(): Set<Egenskap> =
        this
            ?.takeUnless { it.isEmpty() }
            ?.split(',')
            ?.toList()
            .orEmpty()
            .map { it.tilEgenskap() }
            .toSet()

    private fun String.tilEgenskap(): Egenskap =
        when (enumValueOf<ApiEgenskap>(this)) {
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

    private fun OppgaveRepository.Side<OppgaveRepository.OppgaveProjeksjon>.tilApiType(transaksjon: SessionContext): ApiOppgaveProjeksjonSide {
        val personer =
            measureTimedValue {
                transaksjon.personRepository
                    .finnAlle(elementer.map { it.personId }.toSet())
                    .associateBy { it.id() }
            }.let {
                logg.info("Hentet personer for oppgaver. Brukte ${it.duration.inWholeMilliseconds} ms.")
                it.value
            }

        val påVenter =
            measureTimedValue {
                transaksjon.påVentRepository
                    .finnAlle(elementer.mapNotNull { it.påVentId }.toSet())
                    .associateBy { it.id() }
            }.let {
                logg.info("Hentet påventer for oppgaver. Brukte ${it.duration.inWholeMilliseconds} ms.")
                it.value
            }

        val dialoger =
            measureTimedValue {
                transaksjon.dialogRepository
                    .finnAlle(påVenter.values.mapNotNull { it.dialogRef }.toSet())
                    .associateBy { it.id() }
            }.let {
                logg.info("Hentet dialoger for oppgaver. Brukte ${it.duration.inWholeMilliseconds} ms.")
                it.value
            }

        val saksbehandlere =
            measureTimedValue {
                transaksjon.saksbehandlerRepository
                    .finnAlle(
                        elementer
                            .mapNotNull { it.tildeltTilOid }
                            .plus(påVenter.values.map { it.saksbehandlerOid })
                            .toSet(),
                    ).associateBy { it.id() }
            }.let {
                logg.info("Hentet saksbehandlere for oppgaver. Brukte ${it.duration.inWholeMilliseconds} ms.")
                it.value
            }

        return ApiOppgaveProjeksjonSide(
            totaltAntall = totaltAntall,
            sidetall = sidetall,
            sidestoerrelse = sidestørrelse,
            elementer =
                elementer.map { oppgave ->
                    val person = personer.getRequired(oppgave.personId)
                    ApiOppgaveProjeksjon(
                        id = oppgave.id.toString(),
                        aktorId = person.aktørId,
                        navn =
                            person.info?.let { personInfo ->
                                ApiPersonnavn(
                                    fornavn = personInfo.fornavn,
                                    etternavn = personInfo.etternavn,
                                    mellomnavn = personInfo.mellomnavn,
                                )
                            } ?: error("Person med id ${oppgave.personId} har ingen info"),
                        egenskaper =
                            oppgave.egenskaper
                                .map { egenskap -> egenskap.tilApiversjon() }
                                .sortedBy { it.name },
                        tildeling =
                            oppgave.tildeltTilOid
                                ?.let { saksbehandlere.getRequired(it) }
                                ?.let { tildeltSaksbehandler ->
                                    ApiTildeling(
                                        navn = tildeltSaksbehandler.navn,
                                        epost = tildeltSaksbehandler.epost,
                                        oid = tildeltSaksbehandler.id().value,
                                    )
                                },
                        opprettetTidspunkt = oppgave.opprettetTidspunkt,
                        opprinneligSoeknadstidspunkt = oppgave.opprinneligSøknadstidspunkt,
                        paVentInfo =
                            oppgave.påVentId
                                ?.let { påVentId -> påVenter[påVentId] }
                                ?.tilPaaVent(saksbehandlere, dialoger),
                    )
                },
        )
    }

    private fun PåVent.tilPaaVent(
        saksbehandlere: Map<SaksbehandlerOid, Saksbehandler>,
        dialoger: Map<DialogId, Dialog>,
    ): ApiOppgaveProjeksjon.PaaVent =
        ApiOppgaveProjeksjon.PaaVent(
            arsaker = årsaker,
            tekst = notattekst,
            dialogRef = dialogRef?.value ?: error("LagtPåVent ${id()} har ingen dialogRef"),
            saksbehandler = saksbehandlere.getRequired(saksbehandlerOid).ident,
            opprettet =
                opprettetTidspunkt
                    .atZone(ZoneId.of("Europe/Oslo"))
                    .toLocalDateTime(),
            tidsfrist = frist,
            kommentarer =
                dialogRef
                    ?.let {
                        dialoger
                            .getRequired(it)
                            .kommentarer
                            .map { kommentar ->
                                ApiOppgaveProjeksjon.PaaVent.Kommentar(
                                    id = kommentar.id().value,
                                    tekst = kommentar.tekst,
                                    opprettet = kommentar.opprettetTidspunkt,
                                    saksbehandlerident = kommentar.saksbehandlerident,
                                    feilregistrert_tidspunkt = null,
                                )
                            }
                    }.orEmpty(),
        )

    private fun <K, V> Map<K, V>.getRequired(key: K): V =
        getOrElse(key) {
            error("Fant ikke igjen nøkkel $key i map. Det betyr antageligvis at den ikke ble funnet i databasen.")
        }

    private fun Egenskap.skalDukkeOppFor(
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
    ): Boolean =
        Oppgave.harTilgangTilEgenskap(
            egenskap = this,
            saksbehandler = saksbehandler,
            saksbehandlerTilgangsgrupper = tilgangsgrupper,
        ) &&
            when (this) {
                Egenskap.BESLUTTER -> Tilgangsgruppe.BESLUTTER in tilgangsgrupper
                Egenskap.STIKKPRØVE -> Tilgangsgruppe.STIKKPRØVE in tilgangsgrupper
                else -> true
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

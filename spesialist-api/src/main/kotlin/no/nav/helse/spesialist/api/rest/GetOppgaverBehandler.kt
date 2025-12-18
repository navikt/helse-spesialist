package no.nav.helse.spesialist.api.rest

import io.github.smiley4.ktoropenapi.config.RouteConfig
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
import no.nav.helse.spesialist.api.graphql.schema.ApiSorteringsrekkefølge
import no.nav.helse.spesialist.api.graphql.schema.ApiTildeling
import no.nav.helse.spesialist.api.rest.resources.Oppgaver
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.domain.Dialog
import no.nav.helse.spesialist.domain.DialogId
import no.nav.helse.spesialist.domain.PåVent
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import java.time.ZoneId
import kotlin.time.measureTimedValue

class GetOppgaverBehandler : GetBehandler<Oppgaver, ApiOppgaveProjeksjonSide, ApiGetOppgaverErrorCode> {
    override fun behandle(
        resource: Oppgaver,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
    ): RestResponse<ApiOppgaveProjeksjonSide, ApiGetOppgaverErrorCode> {
        val (finnOppgaveProjeksjoner, tidsbruk) =
            measureTimedValue {
                transaksjon.oppgaveRepository
                    .finnOppgaveProjeksjoner(
                        minstEnAvEgenskapene = resource.minstEnAvEgenskapene.map { it.tilEgenskaper() },
                        ingenAvEgenskapene =
                            Egenskap.entries
                                .filterNot { it.skalDukkeOppFor(saksbehandler, tilgangsgrupper) }
                                .plus(resource.ingenAvEgenskapene.tilEgenskaper())
                                .toSet(),
                        erTildelt = resource.erTildelt,
                        tildeltTilOid = resource.tildeltTilOid?.let(::SaksbehandlerOid),
                        erPåVent = resource.erPaaVent,
                        ikkeSendtTilBeslutterAvOid = saksbehandler.id,
                        sorterPå =
                            when (resource.sorteringsfelt) {
                                null,
                                ApiOppgaveSorteringsfelt.opprettetTidspunkt,
                                -> SorteringsnøkkelForDatabase.OPPRETTET

                                ApiOppgaveSorteringsfelt.tildeling -> SorteringsnøkkelForDatabase.TILDELT_TIL

                                ApiOppgaveSorteringsfelt.opprinneligSoeknadstidspunkt -> SorteringsnøkkelForDatabase.SØKNAD_MOTTATT

                                ApiOppgaveSorteringsfelt.paVentInfo_tidsfrist -> SorteringsnøkkelForDatabase.TIDSFRIST

                                ApiOppgaveSorteringsfelt.behandlingOpprettetTidspunkt -> SorteringsnøkkelForDatabase.BEHANDLING_OPPRETTET_TIDSPUNKT
                            },
                        sorteringsrekkefølge =
                            when (resource.sorteringsrekkefoelge) {
                                null,
                                ApiSorteringsrekkefølge.STIGENDE,
                                -> Sorteringsrekkefølge.STIGENDE

                                ApiSorteringsrekkefølge.SYNKENDE -> Sorteringsrekkefølge.SYNKENDE
                            },
                        sidetall = resource.sidetall?.takeUnless { it < 1 } ?: 1,
                        sidestørrelse = resource.sidestoerrelse?.takeUnless { it < 1 } ?: 10,
                    )
            }
        logg.info("Hentet oppgaver på ${tidsbruk.inWholeMilliseconds} ms.\nParametere: $resource")
        return RestResponse.OK(
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
            ApiEgenskap.JORDBRUKER_REINDRIFT -> Egenskap.JORDBRUKER_REINDRIFT
        }

    private fun OppgaveRepository.Side<OppgaveRepository.OppgaveProjeksjon>.tilApiType(transaksjon: SessionContext): ApiOppgaveProjeksjonSide {
        val personer =
            measureTimedValue {
                transaksjon.personRepository
                    .finnAlle(elementer.map { it.identitetsnummer }.toSet())
                    .associateBy { it.id }
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
                    ).associateBy { it.id }
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
                    val person = personer.getRequired(oppgave.identitetsnummer)
                    val personPseudoId =
                        transaksjon.personPseudoIdDao.nyPersonPseudoId(identitetsnummer = person.id)
                    ApiOppgaveProjeksjon(
                        id = oppgave.id.toString(),
                        aktorId = person.aktørId,
                        personPseudoId = personPseudoId.value,
                        navn =
                            person.info?.let { personInfo ->
                                ApiPersonnavn(
                                    fornavn = personInfo.fornavn,
                                    etternavn = personInfo.etternavn,
                                    mellomnavn = personInfo.mellomnavn,
                                )
                            } ?: error("Person med identitetsnummer ${oppgave.identitetsnummer} har ingen info"),
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
                                        oid = tildeltSaksbehandler.id.value,
                                    )
                                },
                        opprettetTidspunkt = oppgave.opprettetTidspunkt,
                        opprinneligSoeknadstidspunkt = oppgave.opprinneligSøknadstidspunkt,
                        behandlingOpprettetTidspunkt = oppgave.behandlingOpprettetTidspunkt,
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
    ): ApiOppgaveProjeksjon.PaaVentInfo =
        ApiOppgaveProjeksjon.PaaVentInfo(
            arsaker = årsaker,
            tekst = notattekst,
            dialogRef = dialogRef?.value ?: error("LagtPåVent ${id()} har ingen dialogRef"),
            saksbehandler = saksbehandlere.getRequired(saksbehandlerOid).ident.value,
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
                                ApiOppgaveProjeksjon.PaaVentInfo.Kommentar(
                                    id = kommentar.id().value,
                                    tekst = kommentar.tekst,
                                    opprettet = kommentar.opprettetTidspunkt,
                                    saksbehandlerident = kommentar.saksbehandlerident.value,
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

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Oppgaver")
            request {
                queryParameter<List<String>?>(Oppgaver::minstEnAvEgenskapene.name) {
                    explode = true
                }
            }
        }
    }
}

enum class ApiGetOppgaverErrorCode : ApiErrorCode

package no.nav.helse.spesialist.api.rest.oppgaver

import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.oppgave.OppgaveRepository
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.spesialist.api.graphql.schema.ApiEgenskap
import no.nav.helse.spesialist.api.rest.ApiOppgaveProjeksjon
import no.nav.helse.spesialist.api.rest.ApiOppgaveProjeksjonSide
import no.nav.helse.spesialist.api.rest.ApiPersonnavn
import no.nav.helse.spesialist.api.rest.ApiTildeling
import no.nav.helse.spesialist.domain.Dialog
import no.nav.helse.spesialist.domain.DialogId
import no.nav.helse.spesialist.domain.PåVent
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import java.time.ZoneId

internal fun OppgaveRepository.Side<OppgaveRepository.OppgaveProjeksjon>.tilApiType(transaksjon: SessionContext): ApiOppgaveProjeksjonSide {
    val personer =
        transaksjon.personRepository
            .finnAlle(elementer.map { it.identitetsnummer }.toSet())
            .associateBy { it.id }

    val påVenter =
        transaksjon.påVentRepository
            .finnAlle(elementer.mapNotNull { it.påVentId }.toSet())
            .associateBy { it.id() }

    val dialoger =
        transaksjon.dialogRepository
            .finnAlle(påVenter.values.mapNotNull { it.dialogRef }.toSet())
            .associateBy { it.id() }

    val saksbehandlere =
        transaksjon.saksbehandlerRepository
            .finnAlle(
                elementer
                    .mapNotNull { it.tildeltTilOid }
                    .plus(påVenter.values.map { it.saksbehandlerOid })
                    .toSet(),
            ).associateBy { it.id }

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
                    behandlingOpprettetTidspunkt = oppgave.behandlingOpprettetTidspunkt,
                    paVentInfo =
                        oppgave.påVentId
                            ?.let { påVentId -> påVenter[påVentId] }
                            ?.tilPaaVent(saksbehandlere, dialoger),
                )
            },
    )
}

internal fun PåVent.tilPaaVent(
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

internal fun <K, V> Map<K, V>.getRequired(key: K): V =
    getOrElse(key) {
        error("Fant ikke igjen nøkkel $key i map. Det betyr antageligvis at den ikke ble funnet i databasen.")
    }

internal fun Egenskap.skalDukkeOppFor(
    brukerroller: Set<Brukerrolle>,
): Boolean =
    Oppgave.harTilgangTilEgenskap(
        egenskap = this,
        brukerroller = brukerroller,
    ) &&
        when (this) {
            Egenskap.BESLUTTER -> Brukerrolle.Beslutter in brukerroller
            Egenskap.STIKKPRØVE -> Brukerrolle.Stikkprøve in brukerroller
            else -> true
        }

internal fun Egenskap.tilApiversjon(): ApiEgenskap =
    when (this) {
        Egenskap.RISK_QA -> ApiEgenskap.RISK_QA
        Egenskap.FORTROLIG_ADRESSE -> ApiEgenskap.FORTROLIG_ADRESSE
        Egenskap.STRENGT_FORTROLIG_ADRESSE -> ApiEgenskap.STRENGT_FORTROLIG_ADRESSE
        Egenskap.EGEN_ANSATT -> ApiEgenskap.EGEN_ANSATT
        Egenskap.BESLUTTER -> ApiEgenskap.BESLUTTER
        Egenskap.SPESIALSAK -> ApiEgenskap.SPESIALSAK
        Egenskap.REVURDERING -> ApiEgenskap.REVURDERING
        Egenskap.SØKNAD -> ApiEgenskap.SOKNAD
        Egenskap.STIKKPRØVE -> ApiEgenskap.STIKKPROVE
        Egenskap.UTBETALING_TIL_SYKMELDT -> ApiEgenskap.UTBETALING_TIL_SYKMELDT
        Egenskap.DELVIS_REFUSJON -> ApiEgenskap.DELVIS_REFUSJON
        Egenskap.UTBETALING_TIL_ARBEIDSGIVER -> ApiEgenskap.UTBETALING_TIL_ARBEIDSGIVER
        Egenskap.INGEN_UTBETALING -> ApiEgenskap.INGEN_UTBETALING
        Egenskap.HASTER -> ApiEgenskap.HASTER
        Egenskap.RETUR -> ApiEgenskap.RETUR
        Egenskap.VERGEMÅL -> ApiEgenskap.VERGEMAL
        Egenskap.EN_ARBEIDSGIVER -> ApiEgenskap.EN_ARBEIDSGIVER
        Egenskap.FLERE_ARBEIDSGIVERE -> ApiEgenskap.FLERE_ARBEIDSGIVERE
        Egenskap.UTLAND -> ApiEgenskap.UTLAND
        Egenskap.FORLENGELSE -> ApiEgenskap.FORLENGELSE
        Egenskap.FORSTEGANGSBEHANDLING -> ApiEgenskap.FORSTEGANGSBEHANDLING
        Egenskap.INFOTRYGDFORLENGELSE -> ApiEgenskap.INFOTRYGDFORLENGELSE
        Egenskap.OVERGANG_FRA_IT -> ApiEgenskap.OVERGANG_FRA_IT
        Egenskap.SKJØNNSFASTSETTELSE -> ApiEgenskap.SKJONNSFASTSETTELSE
        Egenskap.PÅ_VENT -> ApiEgenskap.PA_VENT
        Egenskap.TILBAKEDATERT -> ApiEgenskap.TILBAKEDATERT
        Egenskap.GOSYS -> ApiEgenskap.GOSYS
        Egenskap.MANGLER_IM -> ApiEgenskap.MANGLER_IM
        Egenskap.MEDLEMSKAP -> ApiEgenskap.MEDLEMSKAP
        Egenskap.GRUNNBELØPSREGULERING -> ApiEgenskap.GRUNNBELOPSREGULERING
        Egenskap.SELVSTENDIG_NÆRINGSDRIVENDE -> ApiEgenskap.SELVSTENDIG_NAERINGSDRIVENDE
        Egenskap.ARBEIDSTAKER -> ApiEgenskap.ARBEIDSTAKER
        Egenskap.JORDBRUKER_REINDRIFT -> ApiEgenskap.JORDBRUKER_REINDRIFT
    }

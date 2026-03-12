package no.nav.helse.spesialist.api.rest

import io.github.smiley4.ktoropenapi.config.RouteConfig
import no.nav.helse.db.SorteringsnøkkelForDatabase
import no.nav.helse.db.Sorteringsrekkefølge
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.spesialist.api.graphql.schema.ApiEgenskap
import no.nav.helse.spesialist.api.rest.resources.Oppgaver
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.SaksbehandlerOid

class GetOppgaverBehandler : GetBehandler<Oppgaver, ApiOppgaveProjeksjonSide, ApiGetOppgaverErrorCode> {
    override fun behandle(
        resource: Oppgaver,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiOppgaveProjeksjonSide, ApiGetOppgaverErrorCode> {
        val oppgaver =
            kallKontekst.transaksjon.oppgaveRepository
                .finnOppgaveProjeksjoner(
                    minstEnAvEgenskapene = resource.minstEnAvEgenskapene.map { it.tilEgenskaper() },
                    ingenAvEgenskapene =
                        Egenskap.entries
                            .filterNot { it.skalDukkeOppFor(kallKontekst.brukerroller) }
                            .plus(resource.ingenAvEgenskapene.tilEgenskaper())
                            .toSet(),
                    erTildelt = resource.erTildelt,
                    tildeltTilOid = resource.tildeltTilOid?.let(::SaksbehandlerOid),
                    erPåVent = resource.erPaaVent,
                    ikkeSendtTilBeslutterAvOid = kallKontekst.saksbehandler.id,
                    sorterPå =
                        when (resource.sorteringsfelt) {
                            null, ApiOppgaveSorteringsfelt.opprettetTidspunkt -> SorteringsnøkkelForDatabase.OPPRETTET

                            ApiOppgaveSorteringsfelt.tildeling -> SorteringsnøkkelForDatabase.TILDELT_TIL

                            ApiOppgaveSorteringsfelt.påVentInfo_tidsfrist -> SorteringsnøkkelForDatabase.TIDSFRIST

                            ApiOppgaveSorteringsfelt.behandlingOpprettetTidspunkt -> SorteringsnøkkelForDatabase.BEHANDLING_OPPRETTET_TIDSPUNKT
                        },
                    sorteringsrekkefølge =
                        when (resource.sorteringsrekkefoelge) {
                            null, ApiSorteringsrekkefølge.STIGENDE -> Sorteringsrekkefølge.STIGENDE

                            ApiSorteringsrekkefølge.SYNKENDE -> Sorteringsrekkefølge.SYNKENDE
                        },
                    sidetall = resource.sidetall?.takeUnless { it < 1 } ?: 1,
                    sidestørrelse = resource.sidestoerrelse?.takeUnless { it < 1 } ?: 10,
                ).tilApiType(kallKontekst.transaksjon)

        loggInfo("Hentet ${oppgaver.elementer.size} oppgaver (av totalt ${oppgaver.totaltAntall})")

        return RestResponse.OK(oppgaver)
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

    override val tag = Tags.OPPGAVER

    override fun openApi(config: RouteConfig) {
        config.request {
            queryParameter<List<String>?>(Oppgaver::minstEnAvEgenskapene.name) {
                explode = true
            }
        }
    }
}

enum class ApiGetOppgaverErrorCode : ApiErrorCode

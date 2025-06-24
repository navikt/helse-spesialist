package no.nav.helse.mediator.oppgave

import no.nav.helse.db.AntallOppgaverFraDatabase
import no.nav.helse.db.BehandletOppgaveFraDatabaseForVisning
import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.OppgaveFraDatabaseForVisning
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.spesialist.api.graphql.schema.ApiAntallArbeidsforhold
import no.nav.helse.spesialist.api.graphql.schema.ApiAntallOppgaver
import no.nav.helse.spesialist.api.graphql.schema.ApiBehandletOppgave
import no.nav.helse.spesialist.api.graphql.schema.ApiEgenskap
import no.nav.helse.spesialist.api.graphql.schema.ApiKategori
import no.nav.helse.spesialist.api.graphql.schema.ApiKommentar
import no.nav.helse.spesialist.api.graphql.schema.ApiMottaker
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgaveTilBehandling
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgaveegenskap
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgavetype
import no.nav.helse.spesialist.api.graphql.schema.ApiPaVentInfo
import no.nav.helse.spesialist.api.graphql.schema.ApiPeriodetype
import no.nav.helse.spesialist.api.graphql.schema.ApiPersonnavn
import no.nav.helse.spesialist.api.graphql.schema.ApiTildeling

internal object OppgaveMapper {
    internal fun List<OppgaveFraDatabaseForVisning>.tilOppgaverTilBehandling() =
        map { oppgave ->
            val egenskaper = oppgave.egenskaper.tilModellversjoner()
            ApiOppgaveTilBehandling(
                id = oppgave.id.toString(),
                opprettet = oppgave.opprettet,
                opprinneligSoknadsdato = oppgave.opprinneligSøknadsdato,
                tidsfrist = oppgave.tidsfrist,
                paVentInfo =
                    oppgave.paVentInfo?.let { påVentInfo ->
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
                vedtaksperiodeId = oppgave.vedtaksperiodeId,
                navn =
                    ApiPersonnavn(
                        fornavn = oppgave.navn.fornavn,
                        etternavn = oppgave.navn.etternavn,
                        mellomnavn = oppgave.navn.mellomnavn,
                    ),
                aktorId = oppgave.aktørId,
                tildeling =
                    oppgave.tildelt?.let { tildelt ->
                        ApiTildeling(
                            tildelt.navn,
                            tildelt.epostadresse,
                            tildelt.oid,
                        )
                    },
                egenskaper =
                    egenskaper.map { egenskap ->
                        ApiOppgaveegenskap(egenskap.tilApiversjon(), egenskap.kategori.tilApiversjon())
                    },
                periodetype = egenskaper.periodetype(),
                oppgavetype = egenskaper.oppgavetype(),
                mottaker = egenskaper.mottaker(),
                antallArbeidsforhold = egenskaper.antallArbeidsforhold(),
            )
        }

    internal fun Set<EgenskapForDatabase>.tilEgenskaperForVisning() =
        tilModellversjoner().map { egenskap ->
            ApiOppgaveegenskap(egenskap.tilApiversjon(), egenskap.kategori.tilApiversjon())
        }

    internal fun AntallOppgaverFraDatabase.tilApiversjon(): ApiAntallOppgaver =
        ApiAntallOppgaver(
            antallMineSaker = this.antallMineSaker,
            antallMineSakerPaVent = this.antallMineSakerPåVent,
        )

    internal fun List<BehandletOppgaveFraDatabaseForVisning>.tilBehandledeOppgaver() =
        map {
            val egenskaper = it.egenskaper.tilModellversjoner()
            ApiBehandletOppgave(
                id = it.id.toString(),
                aktorId = it.aktørId,
                oppgavetype = egenskaper.oppgavetype(),
                periodetype = egenskaper.periodetype(),
                antallArbeidsforhold = egenskaper.antallArbeidsforhold(),
                ferdigstiltTidspunkt = it.ferdigstiltTidspunkt,
                ferdigstiltAv = it.ferdigstiltAv,
                beslutter = it.beslutter,
                saksbehandler = it.saksbehandler,
                personnavn =
                    ApiPersonnavn(
                        fornavn = it.navn.fornavn,
                        etternavn = it.navn.etternavn,
                        mellomnavn = it.navn.mellomnavn,
                    ),
            )
        }

    private fun Set<EgenskapForDatabase>.tilModellversjoner(): List<Egenskap> = this.mapNotNull { it.tilModellversjon() }

    private fun List<Egenskap>.periodetype(): ApiPeriodetype {
        val egenskap = single { egenskap -> egenskap.kategori == Egenskap.Kategori.Periodetype }
        return when (egenskap) {
            Egenskap.FORSTEGANGSBEHANDLING -> ApiPeriodetype.FORSTEGANGSBEHANDLING
            Egenskap.FORLENGELSE -> ApiPeriodetype.FORLENGELSE
            Egenskap.INFOTRYGDFORLENGELSE -> ApiPeriodetype.INFOTRYGDFORLENGELSE
            Egenskap.OVERGANG_FRA_IT -> ApiPeriodetype.OVERGANG_FRA_IT
            else -> throw IllegalArgumentException("Kunne ikke mappe egenskap til periodetype")
        }
    }

    private fun List<Egenskap>.oppgavetype(): ApiOppgavetype {
        val egenskap = single { egenskap -> egenskap.kategori == Egenskap.Kategori.Oppgavetype }
        return when (egenskap) {
            Egenskap.SØKNAD -> ApiOppgavetype.SOKNAD
            Egenskap.REVURDERING -> ApiOppgavetype.REVURDERING
            else -> throw IllegalArgumentException("Kunne ikke mappe egenskap til periodetype")
        }
    }

    private fun List<Egenskap>.mottaker(): ApiMottaker {
        val egenskap = single { egenskap -> egenskap.kategori == Egenskap.Kategori.Mottaker }
        return when (egenskap) {
            Egenskap.UTBETALING_TIL_SYKMELDT -> ApiMottaker.SYKMELDT
            Egenskap.UTBETALING_TIL_ARBEIDSGIVER -> ApiMottaker.ARBEIDSGIVER
            Egenskap.DELVIS_REFUSJON -> ApiMottaker.BEGGE
            Egenskap.INGEN_UTBETALING -> ApiMottaker.INGEN
            else -> throw IllegalArgumentException("Kunne ikke mappe egenskap til periodetype")
        }
    }

    private fun List<Egenskap>.antallArbeidsforhold(): ApiAntallArbeidsforhold {
        val egenskap = single { egenskap -> egenskap.kategori == Egenskap.Kategori.Inntektskilde }
        return when (egenskap) {
            Egenskap.EN_ARBEIDSGIVER -> ApiAntallArbeidsforhold.ET_ARBEIDSFORHOLD
            Egenskap.FLERE_ARBEIDSGIVERE -> ApiAntallArbeidsforhold.FLERE_ARBEIDSFORHOLD
            else -> throw IllegalArgumentException("Kunne ikke mappe egenskap til periodetype")
        }
    }

    internal fun List<ApiOppgaveegenskap>.tilDatabaseversjon(): List<EgenskapForDatabase> = this.map { it.tilDatabaseversjon() }

    internal fun ApiKategori.tilDatabaseversjon(): Egenskap.Kategori =
        when (this) {
            ApiKategori.Mottaker -> Egenskap.Kategori.Mottaker
            ApiKategori.Inntektskilde -> Egenskap.Kategori.Inntektskilde
            ApiKategori.Inntektsforhold -> Egenskap.Kategori.Inntektsforhold
            ApiKategori.Oppgavetype -> Egenskap.Kategori.Oppgavetype
            ApiKategori.Ukategorisert -> Egenskap.Kategori.Ukategorisert
            ApiKategori.Periodetype -> Egenskap.Kategori.Periodetype
            ApiKategori.Status -> Egenskap.Kategori.Status
        }

    private fun Egenskap.tilApiversjon(): ApiEgenskap =
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
        }

    private fun Egenskap.Kategori.tilApiversjon(): ApiKategori =
        when (this) {
            Egenskap.Kategori.Mottaker -> ApiKategori.Mottaker
            Egenskap.Kategori.Inntektskilde -> ApiKategori.Inntektskilde
            Egenskap.Kategori.Inntektsforhold -> ApiKategori.Inntektsforhold
            Egenskap.Kategori.Oppgavetype -> ApiKategori.Oppgavetype
            Egenskap.Kategori.Ukategorisert -> ApiKategori.Ukategorisert
            Egenskap.Kategori.Periodetype -> ApiKategori.Periodetype
            Egenskap.Kategori.Status -> ApiKategori.Status
        }

    private fun EgenskapForDatabase.tilModellversjon(): Egenskap? =
        when (this) {
            EgenskapForDatabase.RISK_QA -> Egenskap.RISK_QA
            EgenskapForDatabase.FORTROLIG_ADRESSE -> Egenskap.FORTROLIG_ADRESSE
            EgenskapForDatabase.STRENGT_FORTROLIG_ADRESSE -> Egenskap.STRENGT_FORTROLIG_ADRESSE
            EgenskapForDatabase.EGEN_ANSATT -> Egenskap.EGEN_ANSATT
            EgenskapForDatabase.BESLUTTER -> Egenskap.BESLUTTER
            EgenskapForDatabase.SPESIALSAK -> Egenskap.SPESIALSAK
            EgenskapForDatabase.REVURDERING -> Egenskap.REVURDERING
            EgenskapForDatabase.SØKNAD -> Egenskap.SØKNAD
            EgenskapForDatabase.STIKKPRØVE -> Egenskap.STIKKPRØVE
            EgenskapForDatabase.UTBETALING_TIL_SYKMELDT -> Egenskap.UTBETALING_TIL_SYKMELDT
            EgenskapForDatabase.DELVIS_REFUSJON -> Egenskap.DELVIS_REFUSJON
            EgenskapForDatabase.UTBETALING_TIL_ARBEIDSGIVER -> Egenskap.UTBETALING_TIL_ARBEIDSGIVER
            EgenskapForDatabase.INGEN_UTBETALING -> Egenskap.INGEN_UTBETALING
            EgenskapForDatabase.HASTER -> Egenskap.HASTER
            EgenskapForDatabase.RETUR -> Egenskap.RETUR
            EgenskapForDatabase.VERGEMÅL -> Egenskap.VERGEMÅL
            EgenskapForDatabase.EN_ARBEIDSGIVER -> Egenskap.EN_ARBEIDSGIVER
            EgenskapForDatabase.FLERE_ARBEIDSGIVERE -> Egenskap.FLERE_ARBEIDSGIVERE
            EgenskapForDatabase.UTLAND -> Egenskap.UTLAND
            EgenskapForDatabase.FORLENGELSE -> Egenskap.FORLENGELSE
            EgenskapForDatabase.FORSTEGANGSBEHANDLING -> Egenskap.FORSTEGANGSBEHANDLING
            EgenskapForDatabase.INFOTRYGDFORLENGELSE -> Egenskap.INFOTRYGDFORLENGELSE
            EgenskapForDatabase.OVERGANG_FRA_IT -> Egenskap.OVERGANG_FRA_IT
            EgenskapForDatabase.SKJØNNSFASTSETTELSE -> Egenskap.SKJØNNSFASTSETTELSE
            EgenskapForDatabase.PÅ_VENT -> Egenskap.PÅ_VENT
            EgenskapForDatabase.TILBAKEDATERT -> Egenskap.TILBAKEDATERT
            EgenskapForDatabase.GOSYS -> Egenskap.GOSYS
            EgenskapForDatabase.MANGLER_IM -> Egenskap.MANGLER_IM
            EgenskapForDatabase.MEDLEMSKAP -> Egenskap.MEDLEMSKAP
            EgenskapForDatabase.GRUNNBELØPSREGULERING -> Egenskap.GRUNNBELØPSREGULERING
            EgenskapForDatabase.SELVSTENDIG_NÆRINGSDRIVENDE -> Egenskap.SELVSTENDIG_NÆRINGSDRIVENDE
            EgenskapForDatabase.ARBEIDSTAKER -> Egenskap.ARBEIDSTAKER
            // Gammel egenskap fra tidligere iterasjon av tilkommen inntekt, skal overses
            EgenskapForDatabase.TILKOMMEN -> null
        }

    private fun ApiOppgaveegenskap.tilDatabaseversjon() =
        when (this.egenskap) {
            ApiEgenskap.RISK_QA -> EgenskapForDatabase.RISK_QA
            ApiEgenskap.FORTROLIG_ADRESSE -> EgenskapForDatabase.FORTROLIG_ADRESSE
            ApiEgenskap.STRENGT_FORTROLIG_ADRESSE -> EgenskapForDatabase.STRENGT_FORTROLIG_ADRESSE
            ApiEgenskap.EGEN_ANSATT -> EgenskapForDatabase.EGEN_ANSATT
            ApiEgenskap.BESLUTTER -> EgenskapForDatabase.BESLUTTER
            ApiEgenskap.SPESIALSAK -> EgenskapForDatabase.SPESIALSAK
            ApiEgenskap.REVURDERING -> EgenskapForDatabase.REVURDERING
            ApiEgenskap.SOKNAD -> EgenskapForDatabase.SØKNAD
            ApiEgenskap.STIKKPROVE -> EgenskapForDatabase.STIKKPRØVE
            ApiEgenskap.UTBETALING_TIL_SYKMELDT -> EgenskapForDatabase.UTBETALING_TIL_SYKMELDT
            ApiEgenskap.DELVIS_REFUSJON -> EgenskapForDatabase.DELVIS_REFUSJON
            ApiEgenskap.UTBETALING_TIL_ARBEIDSGIVER -> EgenskapForDatabase.UTBETALING_TIL_ARBEIDSGIVER
            ApiEgenskap.INGEN_UTBETALING -> EgenskapForDatabase.INGEN_UTBETALING
            ApiEgenskap.EN_ARBEIDSGIVER -> EgenskapForDatabase.EN_ARBEIDSGIVER
            ApiEgenskap.FLERE_ARBEIDSGIVERE -> EgenskapForDatabase.FLERE_ARBEIDSGIVERE
            ApiEgenskap.FORLENGELSE -> EgenskapForDatabase.FORLENGELSE
            ApiEgenskap.FORSTEGANGSBEHANDLING -> EgenskapForDatabase.FORSTEGANGSBEHANDLING
            ApiEgenskap.INFOTRYGDFORLENGELSE -> EgenskapForDatabase.INFOTRYGDFORLENGELSE
            ApiEgenskap.OVERGANG_FRA_IT -> EgenskapForDatabase.OVERGANG_FRA_IT
            ApiEgenskap.UTLAND -> EgenskapForDatabase.UTLAND
            ApiEgenskap.HASTER -> EgenskapForDatabase.HASTER
            ApiEgenskap.RETUR -> EgenskapForDatabase.RETUR
            ApiEgenskap.VERGEMAL -> EgenskapForDatabase.VERGEMÅL
            ApiEgenskap.SKJONNSFASTSETTELSE -> EgenskapForDatabase.SKJØNNSFASTSETTELSE
            ApiEgenskap.PA_VENT -> EgenskapForDatabase.PÅ_VENT
            ApiEgenskap.TILBAKEDATERT -> EgenskapForDatabase.TILBAKEDATERT
            ApiEgenskap.GOSYS -> EgenskapForDatabase.GOSYS
            ApiEgenskap.MANGLER_IM -> EgenskapForDatabase.MANGLER_IM
            ApiEgenskap.MEDLEMSKAP -> EgenskapForDatabase.MEDLEMSKAP
            ApiEgenskap.GRUNNBELOPSREGULERING -> EgenskapForDatabase.GRUNNBELØPSREGULERING
            ApiEgenskap.SELVSTENDIG_NAERINGSDRIVENDE -> EgenskapForDatabase.SELVSTENDIG_NÆRINGSDRIVENDE
            ApiEgenskap.ARBEIDSTAKER -> EgenskapForDatabase.ARBEIDSTAKER
        }
}

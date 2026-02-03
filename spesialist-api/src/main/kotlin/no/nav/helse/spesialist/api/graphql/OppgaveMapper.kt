package no.nav.helse.spesialist.api.graphql

import no.nav.helse.db.AntallOppgaverFraDatabase
import no.nav.helse.db.BehandletOppgaveFraDatabaseForVisning
import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.spesialist.api.graphql.schema.ApiAntallArbeidsforhold
import no.nav.helse.spesialist.api.graphql.schema.ApiAntallOppgaver
import no.nav.helse.spesialist.api.graphql.schema.ApiBehandletOppgave
import no.nav.helse.spesialist.api.graphql.schema.ApiEgenskap
import no.nav.helse.spesialist.api.graphql.schema.ApiKategori
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgaveegenskap
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgavetype
import no.nav.helse.spesialist.api.graphql.schema.ApiPeriodetype
import no.nav.helse.spesialist.api.graphql.schema.ApiPersonnavn
import no.nav.helse.spesialist.application.PersonPseudoId

internal object OppgaveMapper {
    internal fun Set<EgenskapForDatabase>.tilEgenskaperForVisning() =
        tilModellversjoner().map { egenskap ->
            ApiOppgaveegenskap(egenskap.tilApiversjon(), egenskap.kategori.tilApiversjon())
        }

    internal fun AntallOppgaverFraDatabase.tilApiversjon(): ApiAntallOppgaver =
        ApiAntallOppgaver(
            antallMineSaker = this.antallMineSaker,
            antallMineSakerPaVent = this.antallMineSakerPåVent,
        )

    internal fun BehandletOppgaveFraDatabaseForVisning.tilBehandledeOppgaver(personPseudoId: PersonPseudoId): ApiBehandletOppgave {
        val egenskaper = egenskaper.tilModellversjoner()
        return ApiBehandletOppgave(
            id = id.toString(),
            aktorId = aktørId,
            personPseudoId = personPseudoId.value,
            oppgavetype = egenskaper.oppgavetype(),
            periodetype = egenskaper.periodetype(),
            antallArbeidsforhold = egenskaper.antallArbeidsforhold(),
            ferdigstiltTidspunkt = ferdigstiltTidspunkt,
            ferdigstiltAv = ferdigstiltAv,
            beslutter = beslutter,
            saksbehandler = saksbehandler,
            personnavn =
                ApiPersonnavn(
                    fornavn = navn.fornavn,
                    etternavn = navn.etternavn,
                    mellomnavn = navn.mellomnavn,
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

    private fun List<Egenskap>.antallArbeidsforhold(): ApiAntallArbeidsforhold {
        val egenskap = single { egenskap -> egenskap.kategori == Egenskap.Kategori.Inntektskilde }
        return when (egenskap) {
            Egenskap.EN_ARBEIDSGIVER -> ApiAntallArbeidsforhold.ET_ARBEIDSFORHOLD
            Egenskap.FLERE_ARBEIDSGIVERE -> ApiAntallArbeidsforhold.FLERE_ARBEIDSFORHOLD
            else -> throw IllegalArgumentException("Kunne ikke mappe egenskap til periodetype")
        }
    }

    fun Egenskap.tilApiversjon(): ApiEgenskap =
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

    private fun Egenskap.Kategori.tilApiversjon(): ApiKategori =
        when (this) {
            Egenskap.Kategori.Mottaker -> ApiKategori.Mottaker
            Egenskap.Kategori.Inntektskilde -> ApiKategori.Inntektskilde
            Egenskap.Kategori.Inntektsforhold -> ApiKategori.Inntektsforhold
            Egenskap.Kategori.Oppgavetype -> ApiKategori.Oppgavetype
            Egenskap.Kategori.Ukategorisert -> ApiKategori.Ukategorisert
            Egenskap.Kategori.Periodetype -> ApiKategori.Periodetype
            Egenskap.Kategori.Status -> ApiKategori.Status
            Egenskap.Kategori.Arbeidssituasjon -> ApiKategori.Arbeidssituasjon
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
            EgenskapForDatabase.JORDBRUKER_REINDRIFT -> Egenskap.JORDBRUKER_REINDRIFT
            // Gammel egenskap fra tidligere iterasjon av tilkommen inntekt, skal overses
            EgenskapForDatabase.TILKOMMEN -> null
        }
}

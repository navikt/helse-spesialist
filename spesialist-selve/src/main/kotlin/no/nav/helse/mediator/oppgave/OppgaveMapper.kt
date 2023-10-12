package no.nav.helse.mediator.oppgave

import no.nav.helse.db.BehandletOppgaveFraDatabaseForVisning
import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.OppgaveFraDatabaseForVisning
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.spesialist.api.graphql.schema.AntallArbeidsforhold
import no.nav.helse.spesialist.api.graphql.schema.BehandletOppgave
import no.nav.helse.spesialist.api.graphql.schema.Kategori
import no.nav.helse.spesialist.api.graphql.schema.Mottaker
import no.nav.helse.spesialist.api.graphql.schema.OppgaveTilBehandling
import no.nav.helse.spesialist.api.graphql.schema.Oppgaveegenskap
import no.nav.helse.spesialist.api.graphql.schema.Oppgavetype
import no.nav.helse.spesialist.api.graphql.schema.Periodetype
import no.nav.helse.spesialist.api.graphql.schema.Personnavn
import no.nav.helse.spesialist.api.graphql.schema.Tildeling
import no.nav.helse.spesialist.api.graphql.schema.Egenskap as EgenskapForApi

internal object OppgaveMapper {
    internal fun List<OppgaveFraDatabaseForVisning>.tilOppgaverTilBehandling() = map {
        val egenskaper = it.egenskaper.tilModellversjoner()
        OppgaveTilBehandling(
            id = it.id.toString(),
            opprettet = it.opprettet.toString(),
            opprinneligSoknadsdato = it.opprinneligSøknadsdato.toString(),
            vedtaksperiodeId = it.vedtaksperiodeId.toString(),
            navn = Personnavn(
                fornavn = it.navn.fornavn,
                etternavn = it.navn.etternavn,
                mellomnavn = it.navn.mellomnavn,
            ),
            aktorId = it.aktørId,
            tildeling = it.tildelt?.let { tildelt ->
                Tildeling(
                    tildelt.navn,
                    tildelt.epostadresse,
                    tildelt.oid.toString(),
                    it.påVent
                )
            },
            egenskaper = egenskaper.map { egenskap ->
                Oppgaveegenskap(egenskap.tilApiversjon(), egenskap.kategori.tilApiversjon())
            },
            periodetype = egenskaper.periodetype(),
            oppgavetype = egenskaper.oppgavetype(),
            mottaker = egenskaper.mottaker(),
            antallArbeidsforhold = egenskaper.antallArbeidsforhold(),
        )
    }

    internal fun List<BehandletOppgaveFraDatabaseForVisning>.tilBehandledeOppgaver() = map {
        val egenskaper = it.egenskaper.tilModellversjoner()
        BehandletOppgave(
            id = it.id.toString(),
            aktorId = it.aktørId,
            oppgavetype = egenskaper.oppgavetype(),
            periodetype = egenskaper.periodetype(),
            antallArbeidsforhold = egenskaper.antallArbeidsforhold(),
            ferdigstiltTidspunkt = it.ferdigstiltTidspunkt.toString(),
            ferdigstiltAv = it.ferdigstiltAv,
            personnavn = Personnavn(
                fornavn = it.navn.fornavn,
                etternavn = it.navn.etternavn,
                mellomnavn = it.navn.mellomnavn,
            ),
        )
    }

    private fun List<EgenskapForDatabase>.tilModellversjoner(): List<Egenskap> = this.map { it.tilModellversjon() }

    private fun List<Egenskap>.periodetype(): Periodetype {
        val egenskap = single { egenskap -> egenskap.kategori ==  Egenskap.Kategori.Periodetype }
        return when (egenskap) {
            Egenskap.FORSTEGANGSBEHANDLING -> Periodetype.FORSTEGANGSBEHANDLING
            Egenskap.FORLENGELSE -> Periodetype.FORLENGELSE
            Egenskap.INFOTRYGDFORLENGELSE -> Periodetype.INFOTRYGDFORLENGELSE
            Egenskap.OVERGANG_FRA_IT -> Periodetype.OVERGANG_FRA_IT
            else -> throw IllegalArgumentException("Kunne ikke mappe egenskap til periodetype")
        }
    }

    private fun List<Egenskap>.oppgavetype(): Oppgavetype {
        val egenskap = single { egenskap -> egenskap.kategori == Egenskap.Kategori.Oppgavetype }
        return when (egenskap) {
            Egenskap.SØKNAD -> Oppgavetype.SOKNAD
            Egenskap.REVURDERING -> Oppgavetype.REVURDERING
            else -> throw IllegalArgumentException("Kunne ikke mappe egenskap til periodetype")
        }
    }

    private fun List<Egenskap>.mottaker(): Mottaker {
        val egenskap = single { egenskap -> egenskap.kategori ==  Egenskap.Kategori.Mottaker }
        return when (egenskap) {
            Egenskap.UTBETALING_TIL_SYKMELDT -> Mottaker.SYKMELDT
            Egenskap.UTBETALING_TIL_ARBEIDSGIVER -> Mottaker.ARBEIDSGIVER
            Egenskap.DELVIS_REFUSJON -> Mottaker.BEGGE
            Egenskap.INGEN_UTBETALING -> Mottaker.INGEN
            else -> throw IllegalArgumentException("Kunne ikke mappe egenskap til periodetype")
        }
    }

    private fun List<Egenskap>.antallArbeidsforhold(): AntallArbeidsforhold {
        val egenskap = single { egenskap -> egenskap.kategori ==  Egenskap.Kategori.Inntektskilde }
        return when (egenskap) {
            Egenskap.EN_ARBEIDSGIVER -> AntallArbeidsforhold.ET_ARBEIDSFORHOLD
            Egenskap.FLERE_ARBEIDSGIVERE -> AntallArbeidsforhold.FLERE_ARBEIDSFORHOLD
            else -> throw IllegalArgumentException("Kunne ikke mappe egenskap til periodetype")
        }
    }

    private fun Egenskap.tilApiversjon(): EgenskapForApi = when (this) {
        Egenskap.RISK_QA -> EgenskapForApi.RISK_QA
        Egenskap.FORTROLIG_ADRESSE -> EgenskapForApi.FORTROLIG_ADRESSE
        Egenskap.STRENGT_FORTROLIG_ADRESSE -> EgenskapForApi.STRENGT_FORTROLIG_ADRESSE
        Egenskap.EGEN_ANSATT -> EgenskapForApi.EGEN_ANSATT
        Egenskap.BESLUTTER -> EgenskapForApi.BESLUTTER
        Egenskap.SPESIALSAK -> EgenskapForApi.SPESIALSAK
        Egenskap.REVURDERING -> EgenskapForApi.REVURDERING
        Egenskap.SØKNAD -> EgenskapForApi.SOKNAD
        Egenskap.STIKKPRØVE -> EgenskapForApi.STIKKPROVE
        Egenskap.UTBETALING_TIL_SYKMELDT -> EgenskapForApi.UTBETALING_TIL_SYKMELDT
        Egenskap.DELVIS_REFUSJON -> EgenskapForApi.DELVIS_REFUSJON
        Egenskap.UTBETALING_TIL_ARBEIDSGIVER -> EgenskapForApi.UTBETALING_TIL_ARBEIDSGIVER
        Egenskap.INGEN_UTBETALING -> EgenskapForApi.INGEN_UTBETALING
        Egenskap.HASTER -> EgenskapForApi.HASTER
        Egenskap.RETUR -> EgenskapForApi.RETUR
        Egenskap.FULLMAKT -> EgenskapForApi.FULLMAKT
        Egenskap.VERGEMÅL -> EgenskapForApi.VERGEMAL
        Egenskap.EN_ARBEIDSGIVER -> EgenskapForApi.EN_ARBEIDSGIVER
        Egenskap.FLERE_ARBEIDSGIVERE -> EgenskapForApi.FLERE_ARBEIDSGIVERE
        Egenskap.UTLAND -> EgenskapForApi.UTLAND
        Egenskap.FORLENGELSE -> EgenskapForApi.FORLENGELSE
        Egenskap.FORSTEGANGSBEHANDLING -> EgenskapForApi.FORSTEGANGSBEHANDLING
        Egenskap.INFOTRYGDFORLENGELSE -> EgenskapForApi.INFOTRYGDFORLENGELSE
        Egenskap.OVERGANG_FRA_IT -> EgenskapForApi.OVERGANG_FRA_IT
    }

    private fun Egenskap.Kategori.tilApiversjon(): Kategori = when (this) {
        Egenskap.Kategori.Mottaker -> Kategori.Mottaker
        Egenskap.Kategori.Inntektskilde -> Kategori.Inntektskilde
        Egenskap.Kategori.Oppgavetype -> Kategori.Oppgavetype
        Egenskap.Kategori.Ukategorisert -> Kategori.Ukategorisert
        Egenskap.Kategori.Periodetype -> Kategori.Periodetype
    }

    internal fun EgenskapForDatabase.tilModellversjon(): Egenskap = when (this) {
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
        EgenskapForDatabase.FULLMAKT -> Egenskap.FULLMAKT
        EgenskapForDatabase.VERGEMÅL -> Egenskap.VERGEMÅL
        EgenskapForDatabase.EN_ARBEIDSGIVER -> Egenskap.EN_ARBEIDSGIVER
        EgenskapForDatabase.FLERE_ARBEIDSGIVERE -> Egenskap.FLERE_ARBEIDSGIVERE
        EgenskapForDatabase.UTLAND -> Egenskap.UTLAND
        EgenskapForDatabase.FORLENGELSE -> Egenskap.FORLENGELSE
        EgenskapForDatabase.FORSTEGANGSBEHANDLING -> Egenskap.FORSTEGANGSBEHANDLING
        EgenskapForDatabase.INFOTRYGDFORLENGELSE -> Egenskap.INFOTRYGDFORLENGELSE
        EgenskapForDatabase.OVERGANG_FRA_IT -> Egenskap.OVERGANG_FRA_IT
    }

    internal fun Egenskap.tilDatabaseversjon(): EgenskapForDatabase = when (this) {
        Egenskap.DELVIS_REFUSJON -> EgenskapForDatabase.DELVIS_REFUSJON
        Egenskap.INGEN_UTBETALING -> EgenskapForDatabase.INGEN_UTBETALING
        Egenskap.REVURDERING -> EgenskapForDatabase.REVURDERING
        Egenskap.STIKKPRØVE -> EgenskapForDatabase.STIKKPRØVE
        Egenskap.SØKNAD -> EgenskapForDatabase.SØKNAD
        Egenskap.EGEN_ANSATT -> EgenskapForDatabase.EGEN_ANSATT
        Egenskap.FORTROLIG_ADRESSE -> EgenskapForDatabase.FORTROLIG_ADRESSE
        Egenskap.STRENGT_FORTROLIG_ADRESSE -> EgenskapForDatabase.STRENGT_FORTROLIG_ADRESSE
        Egenskap.RISK_QA -> EgenskapForDatabase.RISK_QA
        Egenskap.UTBETALING_TIL_ARBEIDSGIVER -> EgenskapForDatabase.UTBETALING_TIL_ARBEIDSGIVER
        Egenskap.UTBETALING_TIL_SYKMELDT -> EgenskapForDatabase.UTBETALING_TIL_SYKMELDT
        Egenskap.EN_ARBEIDSGIVER -> EgenskapForDatabase.EN_ARBEIDSGIVER
        Egenskap.FLERE_ARBEIDSGIVERE -> EgenskapForDatabase.FLERE_ARBEIDSGIVERE
        Egenskap.UTLAND -> EgenskapForDatabase.UTLAND
        Egenskap.HASTER -> EgenskapForDatabase.HASTER
        Egenskap.BESLUTTER -> EgenskapForDatabase.BESLUTTER
        Egenskap.RETUR -> EgenskapForDatabase.RETUR
        Egenskap.FULLMAKT -> EgenskapForDatabase.FULLMAKT
        Egenskap.VERGEMÅL -> EgenskapForDatabase.VERGEMÅL
        Egenskap.SPESIALSAK -> EgenskapForDatabase.SPESIALSAK
        Egenskap.FORLENGELSE -> EgenskapForDatabase.FORLENGELSE
        Egenskap.FORSTEGANGSBEHANDLING -> EgenskapForDatabase.FORSTEGANGSBEHANDLING
        Egenskap.INFOTRYGDFORLENGELSE -> EgenskapForDatabase.INFOTRYGDFORLENGELSE
        Egenskap.OVERGANG_FRA_IT -> EgenskapForDatabase.OVERGANG_FRA_IT
    }

    internal fun Egenskap.tilKafkaversjon(): String {
        return when (this) {
            Egenskap.SØKNAD -> "SØKNAD"
            Egenskap.STIKKPRØVE -> "STIKKPRØVE"
            Egenskap.RISK_QA -> "RISK_QA"
            Egenskap.REVURDERING -> "REVURDERING"
            Egenskap.FORTROLIG_ADRESSE -> "FORTROLIG_ADRESSE"
            Egenskap.STRENGT_FORTROLIG_ADRESSE -> "STRENGT_FORTROLIG_ADRESSE"
            Egenskap.UTBETALING_TIL_SYKMELDT -> "UTBETALING_TIL_SYKMELDT"
            Egenskap.DELVIS_REFUSJON -> "DELVIS_REFUSJON"
            Egenskap.UTBETALING_TIL_ARBEIDSGIVER -> "UTBETALING_TIL_ARBEIDSGIVER"
            Egenskap.INGEN_UTBETALING -> "INGEN_UTBETALING"
            Egenskap.EGEN_ANSATT -> "EGEN_ANSATT"
            Egenskap.EN_ARBEIDSGIVER -> "EN_ARBEIDSGIVER"
            Egenskap.FLERE_ARBEIDSGIVERE -> "FLERE_ARBEIDSGIVERE"
            Egenskap.UTLAND -> "UTLAND"
            Egenskap.HASTER -> "HASTER"
            Egenskap.BESLUTTER -> "BESLUTTER"
            Egenskap.RETUR -> "RETUR"
            Egenskap.FULLMAKT -> "FULLMAKT"
            Egenskap.VERGEMÅL -> "VERGEMÅL"
            Egenskap.SPESIALSAK -> "SPESIALSAK"
            Egenskap.FORLENGELSE -> "FORLENGELSE"
            Egenskap.FORSTEGANGSBEHANDLING -> "FORSTEGANGSBEHANDLING"
            Egenskap.INFOTRYGDFORLENGELSE -> "INFOTRYGDFORLENGELSE"
            Egenskap.OVERGANG_FRA_IT -> "OVERGANG_FRA_IT"
        }
    }
}
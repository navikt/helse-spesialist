package no.nav.helse.spesialist.api.graphql.schema

import java.time.format.DateTimeFormatter
import no.nav.helse.spesialist.api.oppgave.FerdigstiltOppgaveDto
import no.nav.helse.spesialist.api.vedtaksperiode.Inntektskilde

enum class Oppgavetype {
    SOKNAD,
    STIKKPROVE,
    RISK_QA,
    REVURDERING,
    FORTROLIG_ADRESSE,
    UTBETALING_TIL_SYKMELDT,
    DELVIS_REFUSJON,
    UTBETALING_TIL_ARBEIDSGIVER,
    INGEN_UTBETALING
}

enum class Egenskap {
    RISK_QA,
    FORTROLIG_ADRESSE,
    EGEN_ANSATT,
    BESLUTTER,
    SPESIALSAK,
    REVURDERING,
    SOKNAD,
    STIKKPROVE,
    UTBETALING_TIL_SYKMELDT,
    DELVIS_REFUSJON,
    UTBETALING_TIL_ARBEIDSGIVER,
    INGEN_UTBETALING,
    EN_ARBEIDSGIVER,
    FLERE_ARBEIDSGIVERE,
    FORLENGELSE,
    FORSTEGANGSBEHANDLING,
    INFOTRYGDFORLENGELSE,
    OVERGANG_FRA_IT,
    UTLAND,
    HASTER,
    RETUR,
    FULLMAKT,
    VERGEMAL
}

enum class Kategori {
    Mottaker,
    Inntektskilde,
    Oppgavetype,
    Ukategorisert,
    Periodetype
}

data class Boenhet(
    val id: String,
    val navn: String,
)

data class OppgaveForOversiktsvisning(
    val id: UUIDString,
    val type: Oppgavetype,
    val opprettet: DateTimeString,
    val opprinneligSoknadsdato: DateTimeString,
    val vedtaksperiodeId: UUIDString,
    val personinfo: Personinfo,
    val navn: Personnavn,
    val aktorId: String,
    val fodselsnummer: String,
    val flereArbeidsgivere: Boolean,
    val boenhet: Boenhet?,
    val tildeling: Tildeling?,
    val periodetype: Periodetype?,
    val sistSendt: DateTimeString?,
    val totrinnsvurdering: Totrinnsvurdering?,
    val mottaker: Mottaker?,
    val haster: Boolean?,
    val harVergemal: Boolean?,
    val tilhorerEnhetUtland: Boolean?,
    val spesialsak: Boolean,
)

data class OppgaveTilBehandling(
    val id: String,
    val opprettet: DateTimeString,
    val opprinneligSoknadsdato: DateTimeString,
    val vedtaksperiodeId: UUIDString,
    val navn: Personnavn,
    val aktorId: String,
    val tildeling: Tildeling?,
    val egenskaper: List<Oppgaveegenskap>
)

data class Oppgaveegenskap(
    val egenskap: Egenskap,
    val kategori: Kategori,
)

data class OppgaveForPeriodevisning(
    val id: String,
)

data class Totrinnsvurdering(
    val erRetur: Boolean,
    val saksbehandler: UUIDString?,
    val beslutter: UUIDString?,
    val erBeslutteroppgave: Boolean
)

enum class Mottaker {
    SYKMELDT,
    ARBEIDSGIVER,
    BEGGE
}

data class Personnavn(
    val fornavn: String,
    val etternavn: String,
    val mellomnavn: String?,
)

data class FerdigstiltOppgave(
    val id: String,
    val type: Oppgavetype,
    val ferdigstiltTidspunkt: DateTimeString,
    val personnavn: Personnavn,
    val aktorId: String,
    val periodetype: Periodetype,
    val inntektstype: Inntektstype,
    val bosted: String?,
    val ferdigstiltAv: String?,
)

internal fun no.nav.helse.spesialist.api.oppgave.Oppgavetype.tilOppgavetype(): Oppgavetype =
    when (this) {
        no.nav.helse.spesialist.api.oppgave.Oppgavetype.SØKNAD -> Oppgavetype.SOKNAD
        no.nav.helse.spesialist.api.oppgave.Oppgavetype.STIKKPRØVE -> Oppgavetype.STIKKPROVE
        no.nav.helse.spesialist.api.oppgave.Oppgavetype.RISK_QA -> Oppgavetype.RISK_QA
        no.nav.helse.spesialist.api.oppgave.Oppgavetype.REVURDERING -> Oppgavetype.REVURDERING
        no.nav.helse.spesialist.api.oppgave.Oppgavetype.FORTROLIG_ADRESSE -> Oppgavetype.FORTROLIG_ADRESSE
        no.nav.helse.spesialist.api.oppgave.Oppgavetype.UTBETALING_TIL_SYKMELDT -> Oppgavetype.UTBETALING_TIL_SYKMELDT
        no.nav.helse.spesialist.api.oppgave.Oppgavetype.DELVIS_REFUSJON -> Oppgavetype.DELVIS_REFUSJON
        no.nav.helse.spesialist.api.oppgave.Oppgavetype.UTBETALING_TIL_ARBEIDSGIVER -> Oppgavetype.UTBETALING_TIL_ARBEIDSGIVER
        no.nav.helse.spesialist.api.oppgave.Oppgavetype.INGEN_UTBETALING -> Oppgavetype.INGEN_UTBETALING
    }

internal fun no.nav.helse.spesialist.api.vedtaksperiode.Periodetype.tilPeriodetype(): Periodetype =
    when (this) {
        no.nav.helse.spesialist.api.vedtaksperiode.Periodetype.FØRSTEGANGSBEHANDLING -> Periodetype.FORSTEGANGSBEHANDLING
        no.nav.helse.spesialist.api.vedtaksperiode.Periodetype.FORLENGELSE -> Periodetype.FORLENGELSE
        no.nav.helse.spesialist.api.vedtaksperiode.Periodetype.INFOTRYGDFORLENGELSE -> Periodetype.INFOTRYGDFORLENGELSE
        no.nav.helse.spesialist.api.vedtaksperiode.Periodetype.OVERGANG_FRA_IT -> Periodetype.OVERGANG_FRA_IT
    }

internal fun List<FerdigstiltOppgaveDto>.tilFerdigstilteOppgaver(): List<FerdigstiltOppgave> =
    map {
        FerdigstiltOppgave(
            id = it.id,
            type = it.type.tilOppgavetype(),
            ferdigstiltAv = it.ferdigstiltAv,
            ferdigstiltTidspunkt = it.ferdigstiltTidspunkt.format(DateTimeFormatter.ISO_DATE_TIME),
            personnavn = Personnavn(
                fornavn = it.personinfo.fornavn,
                etternavn = it.personinfo.etternavn,
                mellomnavn = it.personinfo.mellomnavn,
            ),
            aktorId = it.aktørId,
            periodetype = it.periodetype.tilPeriodetype(),
            inntektstype = when (it.inntektskilde) {
                Inntektskilde.EN_ARBEIDSGIVER -> Inntektstype.ENARBEIDSGIVER
                Inntektskilde.FLERE_ARBEIDSGIVERE -> Inntektstype.FLEREARBEIDSGIVERE
            },
            bosted = it.bosted,
        )
    }

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
}

data class Boenhet(
    val id: String,
    val navn: String,
)

data class OppgaveForOversiktsvisning(
    val id: UUIDString,
    val type: Oppgavetype,
    val opprettet: DateTimeString,
    val vedtaksperiodeId: UUIDString,
    val personinfo: Personinfo,
    val aktorId: String,
    val fodselsnummer: String,
    val antallVarsler: Int,
    val flereArbeidsgivere: Boolean,
    val boenhet: Boenhet,
    val erBeslutter: Boolean,
    val erRetur: Boolean,
    val trengerTotrinnsvurdering: Boolean,
    val tildeling: Tildeling?,
    val periodetype: Periodetype?,
    val tidligereSaksbehandler: UUIDString?,
    val sistSendt: DateTimeString?,
)

data class OppgaveForPeriodevisning(
    val id: String,
    val erBeslutter: Boolean,
    val erRetur: Boolean,
    val trengerTotrinnsvurdering: Boolean,
    val tidligereSaksbehandler: String?,
)

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
    val antallVarsler: Int,
    val periodetype: Periodetype,
    val inntektstype: Inntektstype,
    val bosted: String,
    val ferdigstiltAv: String?,
)

data class Paginering(
    val side: Int,
    val elementerPerSide: Int,
    val antallSider: Int,
)

data class Oppgaver(
    val oppgaver: List<OppgaveForOversiktsvisning>,
    val paginering: Paginering,
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
            antallVarsler = it.antallVarsler,
            periodetype = it.periodetype.tilPeriodetype(),
            inntektstype = when (it.inntektskilde) {
                Inntektskilde.EN_ARBEIDSGIVER -> Inntektstype.ENARBEIDSGIVER
                Inntektskilde.FLERE_ARBEIDSGIVERE -> Inntektstype.FLEREARBEIDSGIVERE
            },
            bosted = it.bosted,
        )
    }
package no.nav.helse.mediator.api.graphql.schema

import java.time.format.DateTimeFormatter
import no.nav.helse.mediator.graphql.LocalDateTime
import no.nav.helse.mediator.graphql.UUID
import no.nav.helse.spesialist.api.oppgave.FerdigstiltOppgaveDto
import no.nav.helse.spesialist.api.oppgave.OppgaveForOversiktsvisningDto
import no.nav.helse.spesialist.api.person.Kjønn
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
    val id: UUID,
    val type: Oppgavetype,
    val opprettet: LocalDateTime,
    val vedtaksperiodeId: UUID,
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
    val tidligereSaksbehandler: UUID?,
    val sistSendt: LocalDateTime?,
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
    val ferdigstiltTidspunkt: LocalDateTime,
    val personnavn: Personnavn,
    val aktorId: String,
    val antallVarsler: Int,
    val periodetype: Periodetype,
    val inntektstype: Inntektstype,
    val bosted: String,
    val ferdigstiltAv: String?,
)

data class Paginering(
    val peker: String?, // Base64-encoded datetime
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

internal fun List<OppgaveForOversiktsvisningDto>.tilOppgaver(): List<OppgaveForOversiktsvisning> {
    return map { oppgave ->
        OppgaveForOversiktsvisning(
            id = oppgave.oppgavereferanse,
            type = no.nav.helse.spesialist.api.oppgave.Oppgavetype.valueOf(oppgave.oppgavetype).tilOppgavetype(),
            opprettet = oppgave.opprettet.format(DateTimeFormatter.ISO_DATE_TIME),
            vedtaksperiodeId = oppgave.vedtaksperiodeId.toString(),
            personinfo = Personinfo(
                fornavn = oppgave.personinfo.fornavn,
                mellomnavn = oppgave.personinfo.mellomnavn,
                etternavn = oppgave.personinfo.etternavn,
                fodselsdato = oppgave.personinfo.fødselsdato?.format(DateTimeFormatter.ISO_DATE),
                kjonn = when (oppgave.personinfo.kjønn) {
                    Kjønn.Mann -> Kjonn.Mann
                    Kjønn.Kvinne -> Kjonn.Kvinne
                    Kjønn.Ukjent -> Kjonn.Ukjent
                    null -> Kjonn.Ukjent
                },
                adressebeskyttelse = oppgave.personinfo.adressebeskyttelse,
                reservasjon = null,
            ),
            aktorId = oppgave.aktørId,
            fodselsnummer = oppgave.fødselsnummer,
            antallVarsler = oppgave.antallVarsler,
            periodetype = when (oppgave.type) {
                no.nav.helse.spesialist.api.vedtaksperiode.Periodetype.FØRSTEGANGSBEHANDLING -> Periodetype.FORSTEGANGSBEHANDLING
                no.nav.helse.spesialist.api.vedtaksperiode.Periodetype.FORLENGELSE -> Periodetype.FORLENGELSE
                no.nav.helse.spesialist.api.vedtaksperiode.Periodetype.INFOTRYGDFORLENGELSE -> Periodetype.INFOTRYGDFORLENGELSE
                no.nav.helse.spesialist.api.vedtaksperiode.Periodetype.OVERGANG_FRA_IT -> Periodetype.OVERGANG_FRA_IT
                null -> null
            },
            flereArbeidsgivere = when (oppgave.inntektskilde) {
                Inntektskilde.EN_ARBEIDSGIVER -> false
                Inntektskilde.FLERE_ARBEIDSGIVERE -> true
                null -> false
            },
            boenhet = Boenhet(
                id = oppgave.boenhet.id,
                navn = oppgave.boenhet.navn,
            ),
            tildeling = oppgave.tildeling?.let { tildeling ->
                Tildeling(
                    navn = tildeling.navn,
                    epost = tildeling.epost,
                    oid = tildeling.oid.toString(),
                    reservert = tildeling.påVent,
                )
            },
            erRetur = oppgave.erReturOppgave,
            erBeslutter = oppgave.erBeslutterOppgave,
            trengerTotrinnsvurdering = oppgave.trengerTotrinnsvurdering,
            tidligereSaksbehandler = oppgave.tidligereSaksbehandlerOid?.toString(),
            sistSendt = oppgave.sistSendt?.format(DateTimeFormatter.ISO_DATE_TIME),
        )
    }
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
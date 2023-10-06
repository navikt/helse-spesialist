package no.nav.helse.spesialist.api.oppgave

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.spesialist.api.person.PersoninfoApiDto
import no.nav.helse.spesialist.api.tildeling.TildelingApiDto
import no.nav.helse.spesialist.api.vedtaksperiode.EnhetDto
import no.nav.helse.spesialist.api.vedtaksperiode.Inntektskilde
import no.nav.helse.spesialist.api.vedtaksperiode.Periodetype

// TODO: Ta i bruk denne i DAO
data class OppgaveForOversiktsvisningDto(
    val oppgavereferanse: String,
    val oppgavetype: String,
    val opprettet: LocalDateTime,
    val vedtaksperiodeId: UUID,
    val personinfo: PersoninfoApiDto,
    val fødselsnummer: String,
    val aktørId: String,
    val type: Periodetype?,
    val inntektskilde: Inntektskilde?,
    var boenhet: EnhetDto,
    val tildeling: TildelingApiDto?,
    val erBeslutterOppgave: Boolean,
    val erReturOppgave: Boolean,
    val trengerTotrinnsvurdering: Boolean,
    val tidligereSaksbehandlerOid: UUID?,
    val sistSendt: LocalDateTime?
)

data class OppgaveForPeriodevisningDto(
    val id: String,
    val kanAvvises: Boolean,
)

enum class Oppgavestatus {
    AvventerSystem,
    AvventerSaksbehandler,
    Invalidert,
    Ferdigstilt
}

data class Personnavn(
    val fornavn: String,
    val etternavn: String,
    val mellomnavn: String?,
)

data class FerdigstiltOppgaveDto(
    val id: String,
    val type: Oppgavetype,
    val ferdigstiltTidspunkt: LocalDateTime,
    val personinfo: Personnavn,
    val aktørId: String,
    val periodetype: Periodetype,
    val inntektskilde: Inntektskilde,
    val bosted: String,
    val ferdigstiltAv: String?,
)

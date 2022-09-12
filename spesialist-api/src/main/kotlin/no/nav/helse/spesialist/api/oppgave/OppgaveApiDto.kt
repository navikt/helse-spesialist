package no.nav.helse.spesialist.api.oppgave

import no.nav.helse.spesialist.api.person.PersoninfoApiDto
import no.nav.helse.spesialist.api.tildeling.TildelingApiDto
import no.nav.helse.spesialist.api.vedtaksperiode.EnhetDto
import no.nav.helse.spesialist.api.vedtaksperiode.Inntektskilde
import no.nav.helse.spesialist.api.vedtaksperiode.Periodetype
import java.time.LocalDateTime
import java.util.*

data class PaginertOppgave(
    val oppgave: OppgaveForOversiktsvisningDto,
    val radnummer: Int,
)

data class OppgaveForOversiktsvisningDto (
    val oppgavereferanse: String,
    val oppgavetype: String,
    val opprettet: LocalDateTime,
    val vedtaksperiodeId: UUID,
    val personinfo: PersoninfoApiDto,
    val fødselsnummer: String,
    val aktørId: String,
    val antallVarsler: Int,
    val type: Periodetype?,
    val inntektskilde: Inntektskilde?,
    var boenhet: EnhetDto,
    val tildeling: TildelingApiDto?,
    val erBeslutterOppgave: Boolean,
    val erReturOppgave: Boolean,
    val trengerTotrinnsvurdering: Boolean,
    val tidligereSaksbehandlerOid: UUID?
)

data class OppgaveForPeriodevisningDto(
    val id: String,
    val erBeslutter: Boolean,
    val erRetur: Boolean,
    val trengerTotrinnsvurdering: Boolean,
    val tidligereSaksbehandler: String?,
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
    val antallVarsler: Int,
    val periodetype: Periodetype,
    val inntektskilde: Inntektskilde,
    val bosted: String,
    val ferdigstiltAv: String,
)
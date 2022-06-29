package no.nav.helse.spesialist.api.oppgave

import no.nav.helse.spesialist.api.person.PersoninfoApiDto
import no.nav.helse.spesialist.api.tildeling.TildelingApiDto
import no.nav.helse.spesialist.api.vedtaksperiode.EnhetDto
import no.nav.helse.spesialist.api.vedtaksperiode.Inntektskilde
import no.nav.helse.spesialist.api.vedtaksperiode.Periodetype
import java.time.LocalDateTime
import java.util.*

data class OppgaveDto (
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

data class OppgavereferanseDto(
    val oppgavereferanse: Long
)

enum class Oppgavestatus {
    AvventerSystem,
    AvventerSaksbehandler,
    Invalidert,
    Ferdigstilt
}

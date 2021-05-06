package no.nav.helse.oppgave

import no.nav.helse.person.PersoninfoApiDto
import no.nav.helse.tildeling.TildelingApiDto
import no.nav.helse.vedtaksperiode.EnhetDto
import no.nav.helse.vedtaksperiode.Inntektskilde
import no.nav.helse.vedtaksperiode.Periodetype
import java.time.LocalDateTime
import java.util.*

data class OppgaveDto (
    val oppgavereferanse: String,
    val oppgavetype: String,
    val opprettet: LocalDateTime,
    val vedtaksperiodeId: UUID,
    val personinfoApi: PersoninfoApiDto,
    val fødselsnummer: String,
    val aktørId: String,
    val antallVarsler: Int,
    val type: Periodetype?,
    val inntektskilde: Inntektskilde?,
    var boenhet: EnhetDto,
    val tildeling: TildelingApiDto?
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

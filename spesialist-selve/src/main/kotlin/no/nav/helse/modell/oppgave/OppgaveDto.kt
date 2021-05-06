package no.nav.helse.modell.oppgave

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.helse.mediator.meldinger.Kjønn
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.tildeling.TildelingApiDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class PersoninfoDto(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val fødselsdato: LocalDate?,
    val kjønn: Kjønn?
)

data class EnhetDto(@JsonProperty("id") private val _id: String, val navn: String) {
    val id get() = if (_id.length == 3) "0$_id" else _id
}

data class OppgaveDto (
    val oppgavereferanse: String,
    val oppgavetype: String,
    val opprettet: LocalDateTime,
    val vedtaksperiodeId: UUID,
    val personinfo: PersoninfoDto,
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

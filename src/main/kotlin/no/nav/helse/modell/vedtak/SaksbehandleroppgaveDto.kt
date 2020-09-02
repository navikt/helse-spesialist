package no.nav.helse.modell.vedtak

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.helse.modell.person.Kjønn
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

data class SaksbehandleroppgaveDto(
    val oppgavereferanse: UUID,
    val saksbehandlerOid: UUID?,
    val opprettet: LocalDateTime,
    val vedtaksperiodeId: UUID,
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
    val personinfo: PersoninfoDto,
    val fødselsnummer: String,
    val aktørId: String,
    val antallVarsler: Int,
    val type: Saksbehandleroppgavetype?,
    var boenhet: EnhetDto
)

data class SaksbehandleroppgavereferanseDto(
    val oppgavereferanse: UUID
)

enum class Saksbehandleroppgavetype {
    FØRSTEGANGSBEHANDLING,
    FORLENGELSE,
    INFOTRYGDFORLENGELSE,
    OVERGANG_FRA_IT
}

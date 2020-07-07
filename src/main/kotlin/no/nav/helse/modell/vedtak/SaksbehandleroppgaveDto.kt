package no.nav.helse.modell.vedtak

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class NavnDto(val fornavn: String, val mellomnavn: String?, val etternavn: String)

data class EnhetDto(@JsonProperty("id") private val _id: String, val navn: String) {
    val id get() = if (_id.length == 3) "0$_id" else _id
}

data class SaksbehandleroppgaveDto(
    val oppgavereferanse: UUID,
    val opprettet: LocalDateTime,
    val vedtaksperiodeId: UUID,
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
    val navn: NavnDto,
    val fødselsnummer: String,
    val aktørId: String,
    val antallVarsler: Int,
    val type: Saksbehandleroppgavetype?,
    var boenhet: EnhetDto
)

enum class Saksbehandleroppgavetype {
    FØRSTEGANGSBEHANDLING,
    FORLENGELSE,
    INFOTRYGDFORLENGELSE,
    OVERGANG_FRA_IT
}

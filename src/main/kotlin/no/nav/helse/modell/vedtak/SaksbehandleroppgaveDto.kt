package no.nav.helse.modell.vedtak

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class NavnDto(val fornavn: String, val mellomnavn: String?, val etternavn: String)
data class EnhetDto(val id: String, val navn: String)
data class SaksbehandleroppgaveDto(
    val spleisbehovId: UUID,
    val opprettet: LocalDateTime,
    val vedtaksperiodeId: UUID,
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
    val navn: NavnDto,
    val fødselsnummer: String,
    val aktørId: String,
    val antallVarsler: Int
)

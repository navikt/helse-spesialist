package no.nav.helse.db

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class OverstyrtTidslinjeForDatabase(
    val id: UUID,
    val aktørId: String,
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val dager: List<OverstyrtTidslinjedagForDatabase>,
    val begrunnelse: String,
    val opprettet: LocalDateTime
)
data class OverstyrtTidslinjedagForDatabase(
    val dato: LocalDate,
    val type: String,
    val fraType: String,
    val grad: Int?,
    val fraGrad: Int?,
    val subsumsjon: SubsumsjonForDatabase?,
)
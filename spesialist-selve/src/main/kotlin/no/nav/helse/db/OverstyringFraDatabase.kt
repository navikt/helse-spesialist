package no.nav.helse.db

import java.time.LocalDate
import java.util.UUID

class OverstyrtTidslinjeForDatabase(
    val id: UUID,
    val aktørId: String,
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val dager: List<OverstyrtTidslinjedagForDatabase>,
    val begrunnelse: String
)
class OverstyrtTidslinjedagForDatabase(
    val dato: LocalDate,
    val type: String,
    val fraType: String,
    val grad: Int?,
    val fraGrad: Int?,
    val subsumsjon: SubsumsjonForDatabase?,
)
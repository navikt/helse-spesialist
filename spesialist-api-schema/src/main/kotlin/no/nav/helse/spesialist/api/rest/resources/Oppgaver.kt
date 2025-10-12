@file:kotlinx.serialization.UseContextualSerialization(
    BigDecimal::class,
    Boolean::class,
    Instant::class,
    LocalDate::class,
    LocalDateTime::class,
    UUID::class,
)

package no.nav.helse.spesialist.api.rest.resources

import io.ktor.resources.Resource
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgaveSorteringsfelt
import no.nav.helse.spesialist.api.graphql.schema.ApiSorteringsrekkefolge
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Resource("oppgaver")
data class Oppgaver(
    val minstEnAvEgenskapene: List<String> = emptyList(), // Kommaseparerte
    val ingenAvEgenskapene: String? = null, // Kommaseparert
    val erTildelt: Boolean? = null,
    val tildeltTilOid: UUID? = null,
    val erPaaVent: Boolean? = null,
    val sorteringsfelt: ApiOppgaveSorteringsfelt? = null,
    val sorteringsrekkefoelge: ApiSorteringsrekkefolge? = null,
    val sidetall: Int? = null,
    val sidestoerrelse: Int? = null,
)

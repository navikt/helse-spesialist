@file:kotlinx.serialization.UseContextualSerialization(
    BigDecimal::class,
    Boolean::class,
    Instant::class,
    LocalDate::class,
    LocalDateTime::class,
    UUID::class,
)

package no.nav.helse.spesialist.api.rest

import io.github.smiley4.schemakenerator.core.annotations.Name
import kotlinx.serialization.Serializable
import no.nav.helse.spesialist.api.graphql.schema.ApiEgenskap
import no.nav.helse.spesialist.api.graphql.schema.ApiPersonnavn
import no.nav.helse.spesialist.api.graphql.schema.ApiTildeling
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Serializable
@Name("OppgaveProjeksjon")
data class ApiOppgaveProjeksjon(
    val id: String,
    val aktorId: String,
    val navn: ApiPersonnavn,
    val egenskaper: List<ApiEgenskap>,
    val tildeling: ApiTildeling?,
    val opprettetTidspunkt: Instant,
    val opprinneligSoeknadstidspunkt: Instant,
    val paVentInfo: PaaVentInfo?,
) {
    @Serializable
    @Name("OppgaveProjeksjonPaaVentInfo")
    data class PaaVentInfo(
        val arsaker: List<String>,
        val tekst: String?,
        val dialogRef: Long,
        val saksbehandler: String,
        val opprettet: LocalDateTime,
        val tidsfrist: LocalDate,
        val kommentarer: List<Kommentar>,
    ) {
        @Serializable
        @Name("OppgaveProjeksjonPaaVentInfoKommentar")
        data class Kommentar(
            val id: Int,
            val tekst: String,
            val opprettet: LocalDateTime,
            val saksbehandlerident: String,
            val feilregistrert_tidspunkt: LocalDateTime?,
        )
    }
}

@Serializable
@Name("OppgaveProjeksjonSide")
data class ApiOppgaveProjeksjonSide(
    val totaltAntall: Long,
    val sidetall: Int,
    val sidestoerrelse: Int,
    val elementer: List<ApiOppgaveProjeksjon>,
) {
    val totaltAntallSider: Long
        get() = (totaltAntall + (sidestoerrelse - 1)) / sidestoerrelse
}

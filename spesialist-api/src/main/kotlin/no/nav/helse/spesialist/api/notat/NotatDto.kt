package no.nav.helse.spesialist.api.notat

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import java.time.LocalDateTime
import java.util.UUID

data class KommentarDto(
    val id: Int,
    val tekst: String,
    val opprettet: LocalDateTime,
    val saksbehandlerident: String,
    val feilregistrertTidspunkt: LocalDateTime?,
)

@JsonIgnoreProperties
data class NotatDto(
    val id: Int,
    val dialogRef: Int,
    val tekst: String,
    val type: NotatType,
    val opprettet: LocalDateTime,
    val saksbehandlerOid: UUID,
    val saksbehandlerNavn: String,
    val saksbehandlerEpost: String,
    val saksbehandlerIdent: String,
    val vedtaksperiodeId: UUID,
    val feilregistrert: Boolean,
    val feilregistrert_tidspunkt: LocalDateTime?,
    val kommentarer: List<KommentarDto>,
)

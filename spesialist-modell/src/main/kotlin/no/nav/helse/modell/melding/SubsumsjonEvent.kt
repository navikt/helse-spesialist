package no.nav.helse.modell.melding

import java.time.LocalDateTime
import java.util.UUID

data class SubsumsjonEvent(
    val id: UUID,
    val fødselsnummer: String,
    val paragraf: String,
    val ledd: String?,
    val bokstav: String?,
    val lovverk: String,
    val lovverksversjon: String,
    val utfall: String,
    val input: Map<String, Any>,
    val output: Map<String, Any>,
    val sporing: Map<String, List<String>>,
    val tidsstempel: LocalDateTime,
    val kilde: String,
)

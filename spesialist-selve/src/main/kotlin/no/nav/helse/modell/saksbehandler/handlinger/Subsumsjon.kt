package no.nav.helse.modell.saksbehandler.handlinger

import java.time.LocalDateTime
import java.util.UUID

internal class Subsumsjon(
    private val lovhjemmel: Lovhjemmel,
    private val fødselsnummer: String,
    private val input: Map<String, Any>,
    private val output: Map<String, Any>,
    private val utfall: Utfall,
    private val sporing: Sporing,
) {
    private val id = UUID.randomUUID()
    private val lovverk = "folketrygdloven"
    private val lovverksversjon = "2019-06-21"
    private val tidsstempel = LocalDateTime.now()
    private val kilde = "spesialist"

    internal fun byggEvent(): SubsumsjonEvent {
        val lovhjemmelEvent = lovhjemmel.byggEvent()
        return SubsumsjonEvent(
            id = id,
            fødselsnummer = fødselsnummer,
            paragraf = lovhjemmelEvent.paragraf,
            ledd = lovhjemmelEvent.ledd,
            bokstav = lovhjemmelEvent.bokstav,
            lovverk = lovverk,
            lovverksversjon = lovverksversjon,
            utfall = utfall.name,
            input = input,
            output = output,
            sporing = sporing.byggEvent(),
            tidsstempel = tidsstempel,
            kilde = kilde,
        )
    }
    internal class Sporing(
        private val vedtaksperioder: List<UUID>,
        private val organisasjonsnummer: List<String>,
        private val saksbehandler: UUID,
    ) {
        internal fun byggEvent() = mapOf(
            "vedtaksperiode" to vedtaksperioder.map { it.toString() },
            "organisasjonsnummer" to organisasjonsnummer
        )
    }

    internal enum class Utfall {
        VILKAR_BEREGNET
    }
}

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
package no.nav.helse.modell.vilkårsprøving

import java.time.LocalDateTime
import java.util.UUID

class Subsumsjon(
    private val lovhjemmel: Lovhjemmel,
    private val fødselsnummer: String,
    private val input: Map<String, Any>,
    private val output: Map<String, Any>,
    private val utfall: Utfall,
    private val sporing: Sporing,
) {
    private val id = UUID.randomUUID()
    private val tidsstempel = LocalDateTime.now()
    private val kilde = "spesialist"

    fun byggEvent(): SubsumsjonEvent {
        val lovhjemmelEvent = lovhjemmel.byggEvent()
        return SubsumsjonEvent(
            id = id,
            fødselsnummer = fødselsnummer,
            paragraf = lovhjemmelEvent.paragraf,
            ledd = lovhjemmelEvent.ledd,
            bokstav = lovhjemmelEvent.bokstav,
            lovverk = lovhjemmelEvent.lovverk,
            lovverksversjon = lovhjemmelEvent.lovverksversjon,
            utfall = utfall.name,
            input = input,
            output = output,
            sporing = sporing.byggEvent(),
            tidsstempel = tidsstempel,
            kilde = kilde,
        )
    }

    abstract class Sporing(
        private val vedtaksperioder: List<UUID>,
        private val organisasjonsnummer: List<String>,
        private val saksbehandler: List<String>,
    ) {
        protected abstract fun ekstraSporing(): Map<String, List<String>>
        fun byggEvent(): Map<String, List<String>> = mapOf(
            "vedtaksperiode" to vedtaksperioder.map { it.toString() },
            "organisasjonsnummer" to organisasjonsnummer,
            "saksbehandler" to saksbehandler,
        ) + ekstraSporing()
    }

    class SporingOverstyrtTidslinje(
        vedtaksperioder: List<UUID>,
        organisasjonsnummer: List<String>,
        saksbehandler: List<String>,
        private val overstyrtTidslinjeId: UUID
    ): Sporing(vedtaksperioder, organisasjonsnummer, saksbehandler) {
        override fun ekstraSporing(): Map<String, List<String>> =
            mapOf("overstyrtidslinje" to listOf(overstyrtTidslinjeId.toString()))

    }

    enum class Utfall {
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
package no.nav.helse.mediator.meldinger

import no.nav.helse.spesialist.kafka.objectMapper
import org.intellij.lang.annotations.Language

internal class Risikofunn(
    private val kategori: List<String>,
    private val beskrivelse: String
) {

    @Language("JSON")
    private fun tilJson() = """
    {
        "kategori": ${kategori.map { "\"$it\"" }},
        "beskrivelse": "$beskrivelse"
    }
    """.trimIndent()

    internal companion object {
        internal fun Iterable<Risikofunn>.tilJson() = objectMapper.readTree("${map { it.tilJson() }}")
    }
}

package no.nav.helse.mediator.meldinger

import no.nav.helse.objectMapper
import org.intellij.lang.annotations.Language

internal class Risikofunn(
    private val kategori: List<String>,
    private val beskrivelse: String,
    private val kreverSupersaksbehandler: Boolean,
) {

    @Language("JSON")
    private fun tilJson() = """
    {
        "kategori": ${kategori.map { "\"$it\"" }},
        "beskrivelse": "$beskrivelse",
        "kreverSupersaksbehandler": $kreverSupersaksbehandler
    }
    """.trimIndent()

    internal companion object {
        internal fun Iterable<Risikofunn>.tilJson() = objectMapper.readTree("${map { it.tilJson() }}")
    }
}

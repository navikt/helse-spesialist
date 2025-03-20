package no.nav.helse.mediator.meldinger

import no.nav.helse.objectMapper
import org.intellij.lang.annotations.Language

class Risikofunn(
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

    companion object {
        fun Iterable<Risikofunn>.tilJson() = objectMapper.readTree("${map { it.tilJson() }}")
    }
}

package no.nav.helse.mediator.kafka.meldinger

import org.intellij.lang.annotations.Language
import java.util.*

class Testmeldingfabrikk(private val fødselsnummer: String) {
    @Language("JSON")
    fun lagVedtaksperiodeEndret(id: UUID = UUID.randomUUID(), vedtaksperiodeId: UUID = UUID.randomUUID()) = """
        {
            "@event_name": "vedtaksperiode_endret",
            "@id": "$id",
            "vedtaksperiodeId": "$vedtaksperiodeId",
            "fødselsnummer": "$fødselsnummer"
        }"""


}

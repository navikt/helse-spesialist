package no.nav.helse.mediator.kafka.meldinger

import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
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

    @Language("JSON")
    fun lagVedtaksperiodeForkastet(id: UUID = UUID.randomUUID(), vedtaksperiodeId: UUID = UUID.randomUUID()) = """
        {
          "@event_name": "vedtaksperiode_forkastet",
          "@id": "$id",
          "@opprettet": "${LocalDateTime.now()}",
          "vedtaksperiodeId": "$vedtaksperiodeId",
          "fødselsnummer": "$fødselsnummer"
        }"""

}

package no.nav.helse.mediator.kafka

import no.nav.helse.TestPerson
import no.nav.helse.setupDataSourceMedFlyway
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

internal class OppdaterVedtaksperioderTest {

    private val dataSource= setupDataSourceMedFlyway()
    private val person = TestPerson(dataSource)

    @Test
    fun `Oppdater vedtaksperioder`() {
        val vedtaksperiodeId = UUID.randomUUID()
        person.tilSaksbehandlerGodkjenning(vedtaksperiodeId = vedtaksperiodeId)
        person.oppdaterVedtaksperioder(person.aktørId)
        val message = person.rapid.inspektør.message(person.rapid.inspektør.size - 1)
        assertEquals("vedtaksperiode_endret", message["@event_name"].asText())
        assertEquals(person.fødselsnummer, message["fødselsnummer"].asText())
        assertEquals(person.aktørId, message["aktørId"].asText())
        assertEquals(vedtaksperiodeId, UUID.fromString(message["vedtaksperiodeId"].asText()))
    }
}


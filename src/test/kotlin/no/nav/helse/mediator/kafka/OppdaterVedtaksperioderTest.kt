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
        val eventId = UUID.randomUUID()
        person.sendGodkjenningMessage(eventId = eventId, vedtaksperiodeId = vedtaksperiodeId)
        person.sendPersoninfo(eventId = eventId)
        person.oppdaterVedtaksperioder(person.aktørId)
        val message = person.rapid.inspektør.message(person.rapid.inspektør.size - 1)
        assertEquals("vedtaksperiode_endret_manuelt", message["@event_name"].asText())
        assertEquals(person.fødselsnummer, message["fødselsnummer"].asText())
        assertEquals(person.aktørId, message["aktørId"].asText())
        assertEquals(vedtaksperiodeId, UUID.fromString(message["vedtaksperiodeId"].asText()))
    }
}


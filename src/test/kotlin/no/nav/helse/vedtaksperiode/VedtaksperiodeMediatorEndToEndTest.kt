package no.nav.helse.vedtaksperiode

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.helse.TestPerson
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.vedtak.snapshot.SnapshotDao
import no.nav.helse.objectMapper
import no.nav.helse.setupDataSourceMedFlyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class VedtaksperiodeMediatorEndToEndTest {
    private val dataSource = setupDataSourceMedFlyway()
    private val arbeidsgiverDao = ArbeidsgiverDao(dataSource)
    private val snapshotDao = SnapshotDao(dataSource)
    private val personDao = PersonDao(dataSource)
    private val vedtaksperiodeDao = VedtaksperiodeDao(dataSource)
    private val vedtaksperiodeMediator = VedtaksperiodeMediator(
        vedtaksperiodeDao = vedtaksperiodeDao,
        arbeidsgiverDao = arbeidsgiverDao,
        snapshotDao = snapshotDao,
        personDao = personDao,
        dataSource = dataSource
    )

    @Test
    fun `bygger forventet spapspot`() {
        val infotrygdUtbetalinger = """{"spapp":"spot"}"""
        val vedtaksperiodeId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val person = TestPerson(dataSource)
        person.sendGodkjenningMessage(vedtaksperiodeId = vedtaksperiodeId, eventId = eventId)
        person.sendPersoninfo(eventId = eventId, infotrygdUtbetalingerJson = infotrygdUtbetalinger)
        val personDto = vedtaksperiodeMediator.byggSpeilSnapshotForFnr(person.fødselsnummer)
        assertNotNull(personDto)
        assertEquals(objectMapper.convertValue<JsonNode>(infotrygdUtbetalinger), personDto?.infotrygdutbetalinger)
    }

    @Test
    fun `ber om infotrygdutbetaling oppdatering for person med utdatert infotrygdutbetaling`() {
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val person = TestPerson(dataSource)
        person.sendGodkjenningMessage(eventId = eventId, vedtaksperiodeId = vedtaksperiodeId1)
        person.sendPersoninfo(eventId = eventId)
        person.settInfotrygdSistOppdatert(LocalDate.now().minusYears(1))
        person.sendGodkjenningMessage(eventId = UUID.randomUUID(), vedtaksperiodeId = vedtaksperiodeId2)

        val infotrygdutbetalingerBehov = person.finnBehov(vedtaksperiodeId2)
            .filter { "HentInfotrygdutbetalinger" in it["@behov"].map(JsonNode::asText) }

        assertEquals(1, infotrygdutbetalingerBehov.size)
    }
}

package no.nav.helse.e2e

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.sessionOf
import no.nav.helse.TestPerson
import no.nav.helse.mediator.kafka.meldinger.Dagtype
import no.nav.helse.mediator.kafka.meldinger.OverstyringMessage
import no.nav.helse.modell.command.findSaksbehandlerOppgaver
import no.nav.helse.modell.overstyring.finnOverstyring
import no.nav.helse.setupDataSourceMedFlyway
import no.nav.helse.vedtaksperiode.VedtaksperiodeMediator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SaksbehandlerOverstyrerDagerTest {
    private val dataSource = setupDataSourceMedFlyway()

    private val session = sessionOf(dataSource, returnGeneratedKey = true)
    private val vedtaksperiodeMediator = VedtaksperiodeMediator(dataSource)

    private lateinit var eventId: UUID
    private lateinit var vedtaksperiodeId: UUID
    private lateinit var person:TestPerson

    @BeforeEach
    fun setup() {
        eventId = UUID.randomUUID()
        vedtaksperiodeId = UUID.randomUUID()
        person = TestPerson(dataSource)
    }

    @Test
    fun `mottar, lagrer og videresender overstyring`() {
        // Fiks så vi har en sak til godkjenning
        person.sendGodkjenningMessage(
            eventId = eventId,
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 31)
        )
        person.sendPersoninfo(eventId)
        assertEquals(1, session.findSaksbehandlerOppgaver().filter { it.oppgavereferanse == eventId }.size)
        assertTrue(session.findSaksbehandlerOppgaver().any { it.oppgavereferanse == eventId })
        saksbehandlerOverstyrer(
            OverstyringMessage.OverstyringMessageDag(
                dato = LocalDate.of(2018, 1, 20),
                type = Dagtype.Feriedag,
                grad = null
            )
        )
        assertTrue(session.finnOverstyring(person.fødselsnummer, person.orgnummer).isNotEmpty())
        assertTrue(meldinger().any { it.hasNonNull("@event_name") && it["@event_name"].textValue() == "overstyr_tidslinje" })
        assertTrue(session.findSaksbehandlerOppgaver().none { it.oppgavereferanse == eventId })
    }

    @Test
    fun `legger ved overstyringer i speil snapshot`() {
        val vedtaksperiodeId = UUID.randomUUID()
        person.sendGodkjenningMessage(
            eventId = eventId,
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 31),
            vedtaksperiodeId = vedtaksperiodeId
        )
        person.sendPersoninfo(eventId)
        saksbehandlerOverstyrer(
            OverstyringMessage.OverstyringMessageDag(
                dato = LocalDate.of(2018, 1, 20),
                type = Dagtype.Feriedag,
                grad = null
            )
        )

        val eventId2 = UUID.randomUUID()
        person.sendGodkjenningMessage(
            eventId = eventId2,
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 31),
            vedtaksperiodeId = vedtaksperiodeId
        )
        person.sendVedtaksperiodeEndret(vedtaksperiodeId)

        assertTrue(session.findSaksbehandlerOppgaver().any { it.fødselsnummer == person.fødselsnummer })

        val snapshot = vedtaksperiodeMediator.byggSpeilSnapshotForFnr(person.fødselsnummer)
        assertNotNull(snapshot)
        val overstyringer = snapshot.arbeidsgivere.first().overstyringer
        assertEquals(1, overstyringer.size)
        assertEquals(1, overstyringer.first().overstyrteDager.size)
    }

    private fun saksbehandlerOverstyrer(vararg dager :OverstyringMessage.OverstyringMessageDag) {
        person.sendOverstyrteDager(dager.toList())
    }

    fun meldinger(): List<JsonNode> {
        return (0.until(person.rapid.inspektør.size)).map {
            person.rapid.inspektør.message(it)
        }
    }



}

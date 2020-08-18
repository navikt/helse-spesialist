package no.nav.helse.e2e

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.sessionOf
import no.nav.helse.SpleisMockClient
import no.nav.helse.TestPerson
import no.nav.helse.accessTokenClient
import no.nav.helse.mediator.kafka.SpleisbehovMediator
import no.nav.helse.mediator.kafka.meldinger.Dagtype
import no.nav.helse.mediator.kafka.meldinger.OverstyringMessage
import no.nav.helse.modell.command.findSaksbehandlerOppgaver
import no.nav.helse.modell.overstyring.finnOverstyring
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.setupDataSourceMedFlyway
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SaksbehandlerOverstyrerDagerTest {
    private val spleisMockClient = SpleisMockClient()
    private val accessTokenClient = accessTokenClient()
    private val dataSource = setupDataSourceMedFlyway()

    private val session = sessionOf(dataSource, returnGeneratedKey = true)
    private val speilSnapshotRestClient = SpeilSnapshotRestClient(
        spleisMockClient.client,
        accessTokenClient,
        "spleisClientId"
    )
    private val spesialistOID: UUID = UUID.randomUUID()
    private lateinit var spleisbehovMediator: SpleisbehovMediator

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
        sakTilGodkjenning(
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 31)
        )
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

    fun `TODO e2e med response fra spleis`() {
        sakTilGodkjenning(
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 31)
        )
        saksbehandlerOverstyrer(
            OverstyringMessage.OverstyringMessageDag(
                dato = LocalDate.of(2018, 1, 20),
                type = Dagtype.Feriedag,
                grad = null
            )
        )
        // Mock svar fra spleis

        // populer kall til speil
        // Assert på innhold til speil
    }

    private fun saksbehandlerOverstyrer(vararg dager :OverstyringMessage.OverstyringMessageDag) {
        person.sendOverstyrteDager(dager.toList())
        //ingen oppgave til godkjenning for denne personen
    }

    fun meldinger(): List<JsonNode> {
        return (0.until(person.rapid.inspektør.size)).map {
            person.rapid.inspektør.message(it)
        }
    }

    private fun sakTilGodkjenning(periodeFom: LocalDate, periodeTom: LocalDate) {
        person.sendGodkjenningMessage(
            eventId = eventId,
            periodeFom = periodeFom,
            periodeTom = periodeTom
        )
        person.sendPersoninfo(eventId)
    }

}

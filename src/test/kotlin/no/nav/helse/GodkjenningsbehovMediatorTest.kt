package no.nav.helse

import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.mediator.kafka.SpleisbehovMediator
import no.nav.helse.mediator.kafka.meldinger.GodkjenningMessage
import no.nav.helse.modell.command.findBehov
import no.nav.helse.modell.command.findNåværendeOppgave
import no.nav.helse.modell.vedtak.findVedtak
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GodkjenningsbehovMediatorTest {
    private val dataSource = setupDataSourceMedFlyway()
    private val session = sessionOf(dataSource, returnGeneratedKey = true)

    private val spleisMockClient = SpleisMockClient()
    private val accessTokenClient = accessTokenClient()
    private val speilSnapshotRestClient = SpeilSnapshotRestClient(
        spleisMockClient.client,
        accessTokenClient,
        "spleisClientId"
    )

    private val spesialistOID: UUID = UUID.randomUUID()

    @AfterAll
    fun tearDown() {
        session.close()
    }

    @Test
    fun `Spleisbehov persisteres`() {
        val spleisbehovMediator = SpleisbehovMediator(
            dataSource = dataSource,
            speilSnapshotRestClient = speilSnapshotRestClient,
            spesialistOID = spesialistOID
        ).apply { init(TestRapid()) }
        val spleisbehovId = UUID.randomUUID()
        val godkjenningMessage = GodkjenningMessage(
            id = spleisbehovId,
            fødselsnummer = "12345",
            aktørId = "12345",
            organisasjonsnummer = "89123",
            vedtaksperiodeId = UUID.randomUUID(),
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 31),
            warnings = emptyList()
        )
        spleisbehovMediator.håndter(godkjenningMessage, "{}")
        assertNotNull(session.findBehov(spleisbehovId))
    }

    @Test
    fun `invaliderer oppgaver for vedtaksperioder som er rullet tilbake`() {
        val eventId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val person = TestPerson(dataSource)
        person.sendGodkjenningMessage(eventId = eventId, vedtaksperiodeId = vedtaksperiodeId)
        person.sendPersoninfo(eventId = eventId)

        using(sessionOf(dataSource)) { session ->
            assertNotEquals(Oppgavestatus.Invalidert, session.findNåværendeOppgave(eventId)?.status)
            assertNotNull(session.findVedtak(vedtaksperiodeId))
            person.rullTilbake(UUID.randomUUID(), vedtaksperiodeId)
            assertEquals(Oppgavestatus.Invalidert, session.findNåværendeOppgave(eventId)?.status)
            assertNull(session.findVedtak(vedtaksperiodeId))
        }
    }

    @Test
    fun `behandler vedtaksperiode etter rollback`() {
        val eventId1 = UUID.randomUUID()
        val vedtaksperiodeId1 = UUID.randomUUID()
        val eventId2 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val person = TestPerson(dataSource)

        person.sendGodkjenningMessage(eventId = eventId1, vedtaksperiodeId = vedtaksperiodeId1)
        person.sendPersoninfo(eventId = eventId1)
        person.rullTilbake(UUID.randomUUID(), vedtaksperiodeId1)
        person.sendGodkjenningMessage(eventId = eventId2, vedtaksperiodeId = vedtaksperiodeId2)

        using(sessionOf(dataSource)) { session ->
            assertNull(session.findVedtak(vedtaksperiodeId1))
            assertNotNull(session.findVedtak(vedtaksperiodeId2))
        }
    }
}

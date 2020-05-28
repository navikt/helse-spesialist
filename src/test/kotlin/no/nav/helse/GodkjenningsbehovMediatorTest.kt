package no.nav.helse

import no.nav.helse.mediator.kafka.SpleisbehovMediator
import no.nav.helse.mediator.kafka.meldinger.GodkjenningMessage
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.command.OppgaveDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.vedtak.snapshot.SnapshotDao
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestDao
import no.nav.helse.modell.command.SpleisbehovDao
import no.nav.helse.modell.risiko.RisikoDao
import no.nav.helse.modell.vedtak.VedtakDao
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GodkjenningsbehovMediatorTest {
    private lateinit var dataSource: DataSource
    private lateinit var personDao: PersonDao
    private lateinit var arbeidsgiverDao: ArbeidsgiverDao
    private lateinit var vedtakDao: VedtakDao
    private lateinit var snapshotDao: SnapshotDao
    private lateinit var oppgaveDao: OppgaveDao
    private lateinit var speilSnapshotRestDao: SpeilSnapshotRestDao
    private lateinit var spleisbehovDao: SpleisbehovDao
    private lateinit var risikoDao: RisikoDao
    private lateinit var testDao: TestPersonDao

    private val spleisMockClient = SpleisMockClient()
    private val accessTokenClient = accessTokenClient()

    private val spesialistOID: UUID = UUID.randomUUID()

    @BeforeAll
    fun setup() {
        dataSource = setupDataSourceMedFlyway()
        personDao = PersonDao(dataSource)
        arbeidsgiverDao = ArbeidsgiverDao(dataSource)
        vedtakDao = VedtakDao(dataSource)
        snapshotDao = SnapshotDao(dataSource)
        oppgaveDao = OppgaveDao(dataSource)
        speilSnapshotRestDao = SpeilSnapshotRestDao(
            spleisMockClient.client,
            accessTokenClient,
            "spleisClientId"
        )
        spleisbehovDao = SpleisbehovDao(dataSource)
        risikoDao = RisikoDao(dataSource)
        testDao = TestPersonDao(dataSource)
    }

    @Test
    fun `Spleisbehov persisteres`() {
        val spleisbehovMediator = SpleisbehovMediator(
            spleisbehovDao = spleisbehovDao, personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao,
            vedtakDao = vedtakDao,
            snapshotDao = snapshotDao,
            speilSnapshotRestDao = speilSnapshotRestDao,
            oppgaveDao = oppgaveDao,
            risikoDao = risikoDao,
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
        assertNotNull(spleisbehovDao.findBehov(spleisbehovId))
    }

    @Test
    fun `invaliderer oppgaver for vedtaksperioder som er rullet tilbake`() {
        val eventId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val person = TestPerson(dataSource)
        person.tilSaksbehandlerGodkjenning(eventId = eventId, vedtaksperiodeId = vedtaksperiodeId)

        assertNotEquals(Oppgavestatus.Invalidert, oppgaveDao.findNåværendeOppgave(eventId)?.status)
        assertNotNull(vedtakDao.findVedtak(vedtaksperiodeId))
        person.rullTilbake(UUID.randomUUID(), vedtaksperiodeId)
        assertEquals(Oppgavestatus.Invalidert, oppgaveDao.findNåværendeOppgave(eventId)?.status)
        assertNull(vedtakDao.findVedtak(vedtaksperiodeId))
    }

    @Test
    fun `behandler vedtaksperiode etter rollback`() {
        val eventId1 = UUID.randomUUID()
        val vedtaksperiodeId1 = UUID.randomUUID()
        val eventId2 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val person = TestPerson(dataSource)
        person.tilSaksbehandlerGodkjenning(eventId = eventId1, vedtaksperiodeId = vedtaksperiodeId1)
        person.rullTilbake(UUID.randomUUID(), vedtaksperiodeId1)

        person.tilSaksbehandlerGodkjenning(eventId = eventId2, vedtaksperiodeId = vedtaksperiodeId2)

        assertNull(vedtakDao.findVedtak(vedtaksperiodeId1))
        assertNotNull(vedtakDao.findVedtak(vedtaksperiodeId2))
    }
}

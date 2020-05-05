package no.nav.helse

import no.nav.helse.mediator.kafka.SpleisbehovMediator
import no.nav.helse.modell.Spleisbehov
import no.nav.helse.modell.dao.ArbeidsgiverDao
import no.nav.helse.modell.dao.OppgaveDao
import no.nav.helse.modell.dao.PersonDao
import no.nav.helse.modell.dao.SnapshotDao
import no.nav.helse.modell.dao.SpeilSnapshotRestDao
import no.nav.helse.modell.dao.SpleisbehovDao
import no.nav.helse.modell.dao.VedtakDao
import no.nav.helse.modell.løsning.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SpleisbehovTest {
    private lateinit var dataSource: DataSource
    private lateinit var personDao: PersonDao
    private lateinit var arbeidsgiverDao: ArbeidsgiverDao
    private lateinit var vedtakDao: VedtakDao
    private lateinit var snapshotDao: SnapshotDao
    private lateinit var oppgaveDao: OppgaveDao
    private lateinit var speilSnapshotRestDao: SpeilSnapshotRestDao
    private lateinit var testDao: TestPersonDao
    private lateinit var spleisbehovDao: SpleisbehovDao

    private val httpClientForSpleis = httpClientForSpleis()
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
        speilSnapshotRestDao = SpeilSnapshotRestDao(httpClientForSpleis, accessTokenClient, "spleisClientId")
        testDao = TestPersonDao(dataSource)
        spleisbehovDao = SpleisbehovDao(dataSource)

        SpleisbehovMediator(
            spleisbehovDao,
            personDao,
            arbeidsgiverDao,
            vedtakDao,
            snapshotDao,
            speilSnapshotRestDao,
            oppgaveDao,
            spesialistOID
        )
    }

    @Test
    fun `godkjenningsbehov for ny person legger inn ny person i DB`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val spleisBehov = Spleisbehov(
            id = UUID.randomUUID(),
            fødselsnummer = "12345",
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 20),
            vedtaksperiodeId = vedtaksperiodeId,
            aktørId = "123455",
            orgnummer = "98765432",
            vedtaksperiodeReferanse = null,
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao,
            vedtakDao = vedtakDao,
            snapshotDao = snapshotDao,
            speilSnapshotRestDao = speilSnapshotRestDao,
            oppgaveDao = oppgaveDao,
            nåværendeOppgave = null
        )
        spleisBehov.execute()
        spleisBehov.fortsett(HentPersoninfoLøsning("Test", "Mellomnavn", "Etternavnsen", LocalDate.now(), Kjønn.Mann))
        spleisBehov.fortsett(HentEnhetLøsning("3417"))
        spleisBehov.execute()

        assertNotNull(personDao.findPersonByFødselsnummer(12345))
    }

    @Test
    fun `godkjenningsbehov for person oppdaterer person i DB`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val spleisBehov = Spleisbehov(
            id = UUID.randomUUID(),
            fødselsnummer = "13245",
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 20),
            vedtaksperiodeId = vedtaksperiodeId,
            aktørId = "13245",
            orgnummer = "98765432",
            vedtaksperiodeReferanse = null,
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao,
            vedtakDao = vedtakDao,
            snapshotDao = snapshotDao,
            speilSnapshotRestDao = speilSnapshotRestDao,
            oppgaveDao = oppgaveDao,
            nåværendeOppgave = null
        )
        spleisBehov.execute()
        spleisBehov.fortsett(HentPersoninfoLøsning("Test", "Mellomnavn", "Etternavnsen", LocalDate.now(), Kjønn.Mann))
        spleisBehov.fortsett(HentEnhetLøsning("3417"))
        testDao.setEnhetOppdatert(13245, LocalDate.of(2020, 1, 1))
        spleisBehov.execute()
        spleisBehov.fortsett(HentEnhetLøsning("3117"))

        assertNotNull(personDao.findPersonByFødselsnummer(13245))
        assertEquals(LocalDate.now(), personDao.findEnhetSistOppdatert(13245))
    }

    @Test
    fun `godkjenningsbehov for person med ny arbeidsgiver legger inn ny arbeidsgiver i DB`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val spleisBehov = Spleisbehov(
            id = UUID.randomUUID(),
            fødselsnummer = "23456",
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 20),
            vedtaksperiodeId = vedtaksperiodeId,
            aktørId = "123455",
            orgnummer = "98765432",
            vedtaksperiodeReferanse = null,
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao,
            vedtakDao = vedtakDao,
            snapshotDao = snapshotDao,
            speilSnapshotRestDao = speilSnapshotRestDao,
            oppgaveDao = oppgaveDao,
            nåværendeOppgave = null
        )
        spleisBehov.execute()
        spleisBehov.fortsett(HentPersoninfoLøsning("Test", "Mellomnavn", "Etternavnsen", LocalDate.now(), Kjønn.Mann))
        spleisBehov.fortsett(HentEnhetLøsning("3417"))
        spleisBehov.execute()

        spleisBehov.fortsett(ArbeidsgiverLøsning("NAV IKT"))
        assertNotNull(arbeidsgiverDao.findArbeidsgiverByOrgnummer(98765432))
    }

    @Test
    fun `Ved nytt godkjenningsbehov opprettes et nytt vedtak i DB`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val spleisBehov = Spleisbehov(
            id = UUID.randomUUID(),
            fødselsnummer = "34567",
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 20),
            vedtaksperiodeId = vedtaksperiodeId,
            aktørId = "123455",
            orgnummer = "98765433",
            vedtaksperiodeReferanse = null,
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao,
            vedtakDao = vedtakDao,
            snapshotDao = snapshotDao,
            speilSnapshotRestDao = speilSnapshotRestDao,
            oppgaveDao = oppgaveDao,
            nåværendeOppgave = null
        )
        spleisBehov.execute()
        spleisBehov.fortsett(HentPersoninfoLøsning("Test", "Mellomnavn", "Etternavnsen", LocalDate.now(), Kjønn.Mann))
        spleisBehov.fortsett(HentEnhetLøsning("3417"))
        spleisBehov.execute()
        spleisBehov.fortsett(ArbeidsgiverLøsning("NAV IKT"))
        spleisBehov.execute()

        assertNotNull(vedtakDao.findVedtaksperiode(vedtaksperiodeId))
        val saksbehandlerOppgaver = oppgaveDao.findSaksbehandlerOppgaver()
        assertFalse(saksbehandlerOppgaver.isNullOrEmpty())
    }

    @Test
    fun `ved godkjenning har spleisbehovet en løsning`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val spleisBehov = Spleisbehov(
            id = UUID.randomUUID(),
            fødselsnummer = "3457812",
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 20),
            vedtaksperiodeId = vedtaksperiodeId,
            aktørId = "123455",
            orgnummer = "98765433",
            vedtaksperiodeReferanse = null,
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao,
            vedtakDao = vedtakDao,
            snapshotDao = snapshotDao,
            speilSnapshotRestDao = speilSnapshotRestDao,
            oppgaveDao = oppgaveDao,
            nåværendeOppgave = null
        )
        spleisBehov.execute()
        spleisBehov.fortsett(HentPersoninfoLøsning("Test", "Mellomnavn", "Etternavnsen", LocalDate.now(), Kjønn.Mann))
        spleisBehov.fortsett(HentEnhetLøsning("3417"))
        spleisBehov.execute()
        spleisBehov.fortsett(ArbeidsgiverLøsning("NAV IKT"))
        spleisBehov.execute()
        spleisBehov.fortsett(
            SaksbehandlerLøsning(
                godkjent = true,
                saksbehandlerIdent = "abcd",
                godkjenttidspunkt = LocalDateTime.now(),
                oid = UUID.randomUUID(),
                epostadresse = "epost"
            )
        )

        assertNotNull(spleisBehov.løsning())
    }

    @Test
    fun `ved feilende command lagres den siste commanden som ikke ble ferdigstilt`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val spleisbehovId = UUID.randomUUID()

        val failingSpeilSnapshotDao = SpeilSnapshotRestDao(failingHttpClient(), accessTokenClient, "spleisClientId")

        val spleisBehov = Spleisbehov(
            id = spleisbehovId,
            fødselsnummer = "134896",
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 20),
            vedtaksperiodeId = vedtaksperiodeId,
            aktørId = "47839",
            orgnummer = "98765433",
            vedtaksperiodeReferanse = null,
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao,
            vedtakDao = vedtakDao,
            snapshotDao = snapshotDao,
            speilSnapshotRestDao = failingSpeilSnapshotDao,
            oppgaveDao = oppgaveDao,
            nåværendeOppgave = null
        )
        spleisBehov.execute()
        spleisBehov.fortsett(HentPersoninfoLøsning("Test", "Mellomnavn", "Etternavnsen", LocalDate.now(), Kjønn.Mann))
        spleisBehov.fortsett(HentEnhetLøsning("3417"))
        assertThrows<Exception> {
            spleisBehov.execute()
        }

        assertEquals("OpprettVedtakCommand", oppgaveDao.findNåværendeOppgave(spleisbehovId)?.oppgaveType)
    }
}

package no.nav.helse

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.modell.Godkjenningsbehov
import no.nav.helse.modell.arbeidsgiver.findArbeidsgiverByOrgnummer
import no.nav.helse.modell.command.*
import no.nav.helse.modell.person.*
import no.nav.helse.modell.vedtak.SaksbehandlerLøsning
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GodkjenningsbehovTest {
    private val spleisMockClient = SpleisMockClient()
    private val accessTokenClient = accessTokenClient()

    private val dataSource: DataSource = setupDataSourceMedFlyway()
    private val session = sessionOf(dataSource, returnGeneratedKey = true)
    private val speilSnapshotRestClient: SpeilSnapshotRestClient =
        SpeilSnapshotRestClient(
            spleisMockClient.client,
            accessTokenClient,
            "spleisClientId"
        )
    private val testDao: TestPersonDao = TestPersonDao(dataSource)

    @AfterAll
    fun cleanup() {
        session.close()
    }

    @Test
    fun `godkjenningsbehov for ny person legger inn ny person i DB`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val spleisExecutor = CommandExecutor(
            session = session,
            command = Godkjenningsbehov(
                id = eventId,
                fødselsnummer = "12345",
                periodeFom = LocalDate.of(2018, 1, 1),
                periodeTom = LocalDate.of(2018, 1, 20),
                vedtaksperiodeId = vedtaksperiodeId,
                aktørId = "123455",
                orgnummer = "98765432",
                speilSnapshotRestClient = speilSnapshotRestClient
            ),
            spesialistOid = UUID.randomUUID(),
            eventId = eventId,
            nåværendeOppgave = null,
            loggingData = *arrayOf()
        )
        spleisExecutor.execute()
        spleisExecutor.resume(
            session,
            løsningify(
                HentPersoninfoLøsning(
                    "Test",
                    "Mellomnavn",
                    "Etternavnsen",
                    LocalDate.now(),
                    Kjønn.Mann
                ),
                HentEnhetLøsning("3417"),
                HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning())
            )
        )
        assertNotNull(session.findPersonByFødselsnummer(12345))
    }

    fun løsningify(vararg løsninger: Any) = Løsninger().also { løsninger.forEach { løsning -> it.add(løsning) } }

    @Test
    fun `godkjenningsbehov for person oppdaterer person i DB`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val spleisExecutor = CommandExecutor(
            session = session,
            command = Godkjenningsbehov(
                id = eventId,
                fødselsnummer = "13245",
                periodeFom = LocalDate.of(2018, 1, 1),
                periodeTom = LocalDate.of(2018, 1, 20),
                vedtaksperiodeId = vedtaksperiodeId,
                aktørId = "13245",
                orgnummer = "98765432",
                speilSnapshotRestClient = speilSnapshotRestClient
            ),
            spesialistOid = UUID.randomUUID(),
            eventId = eventId,
            nåværendeOppgave = null,
            loggingData = *arrayOf()
        )
        spleisExecutor.execute()
        spleisExecutor.resume(
            session,
            løsningify(
                HentPersoninfoLøsning(
                    "Test",
                    "Mellomnavn",
                    "Etternavnsen",
                    LocalDate.now(),
                    Kjønn.Mann
                ),
                HentEnhetLøsning("3417"),
                HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning())
            )
        )
        testDao.setEnhetOppdatert(13245, LocalDate.of(2020, 1, 1))
        spleisExecutor.execute()
        spleisExecutor.resume(session, løsningify(HentEnhetLøsning("1119")))
        spleisExecutor.execute()

        assertNotNull(session.findPersonByFødselsnummer(13245))
        assertEquals(LocalDate.now(), session.findEnhetSistOppdatert(13245))
    }

    @Test
    fun `godkjenningsbehov for person med ny arbeidsgiver legger inn ny arbeidsgiver i DB`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val spleisExecutor = CommandExecutor(
            session = session,
            command = Godkjenningsbehov(
                id = eventId,
                fødselsnummer = "23456",
                periodeFom = LocalDate.of(2018, 1, 1),
                periodeTom = LocalDate.of(2018, 1, 20),
                vedtaksperiodeId = vedtaksperiodeId,
                aktørId = "123455",
                orgnummer = "98765432",
                speilSnapshotRestClient = speilSnapshotRestClient
            ),
            spesialistOid = UUID.randomUUID(),
            eventId = eventId,
            nåværendeOppgave = null,
            loggingData = *arrayOf()
        )
        spleisExecutor.execute()
        spleisExecutor.resume(
            session,
            løsningify(
                HentPersoninfoLøsning(
                    "Test",
                    "Mellomnavn",
                    "Etternavnsen",
                    LocalDate.now(),
                    Kjønn.Mann
                ),
                HentEnhetLøsning("3417"),
                HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning())
            )
        )
        spleisExecutor.execute()

        assertNotNull(session.findArbeidsgiverByOrgnummer(98765432))
    }

    @Test
    fun `Ved nytt godkjenningsbehov opprettes et nytt vedtak i DB`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val spleisExecutor = CommandExecutor(
            session = session,
            command = Godkjenningsbehov(
                id = eventId,
                fødselsnummer = "34567",
                periodeFom = LocalDate.of(2018, 1, 1),
                periodeTom = LocalDate.of(2018, 1, 20),
                vedtaksperiodeId = vedtaksperiodeId,
                aktørId = "123455",
                orgnummer = "98765433",
                speilSnapshotRestClient = speilSnapshotRestClient
            ),
            spesialistOid = UUID.randomUUID(),
            eventId = eventId,
            nåværendeOppgave = null,
            loggingData = *arrayOf()
        )
        spleisExecutor.execute()
        spleisExecutor.resume(
            session,
            løsningify(
                HentPersoninfoLøsning(
                    "Test",
                    "Mellomnavn",
                    "Etternavnsen",
                    LocalDate.now(),
                    Kjønn.Mann
                ),
                HentEnhetLøsning("3417"),
                HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning())
            )
        )
        spleisExecutor.execute()

        assertNotNull(findVedtaksperiode(vedtaksperiodeId))
        val saksbehandlerOppgaver = using(sessionOf(dataSource)) { it.findSaksbehandlerOppgaver() }
        assertFalse(saksbehandlerOppgaver.isNullOrEmpty())
    }

    @Test
    fun `ved godkjenning har spleisbehovet en løsning`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val spleisExecutor = CommandExecutor(
            session = session,
            command = Godkjenningsbehov(
                id = eventId,
                fødselsnummer = "3457812",
                periodeFom = LocalDate.of(2018, 1, 1),
                periodeTom = LocalDate.of(2018, 1, 20),
                vedtaksperiodeId = vedtaksperiodeId,
                aktørId = "123455",
                orgnummer = "98765433",
                speilSnapshotRestClient = speilSnapshotRestClient
            ),
            spesialistOid = UUID.randomUUID(),
            eventId = eventId,
            nåværendeOppgave = null,
            loggingData = *arrayOf()
        )
        spleisExecutor.execute()
        spleisExecutor.resume(
            session,
            løsningify(
                HentPersoninfoLøsning(
                    "Test",
                    "Mellomnavn",
                    "Etternavnsen",
                    LocalDate.now(),
                    Kjønn.Mann
                ),
                HentEnhetLøsning("3417"),
                HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning())
            )
        )
        spleisExecutor.execute()
        spleisExecutor.resume(
            session,
            løsningify(
                SaksbehandlerLøsning(
                    godkjent = true,
                    saksbehandlerIdent = "abcd",
                    godkjenttidspunkt = LocalDateTime.now(),
                    oid = UUID.randomUUID(),
                    epostadresse = "epost",
                    årsak = null,
                    begrunnelser = null,
                    kommentar = null
                )
            )
        )

        val resultat = spleisExecutor.execute()

        assertTrue(resultat.any { it is Command.Resultat.Ok.Løst })
    }

    @Test
    fun `ved feilende command lagres den siste commanden som ikke ble ferdigstilt`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        val failingSpeilSnapshotDao = SpeilSnapshotRestClient(
            failingHttpClient(),
            accessTokenClient,
            "spleisClientId"
        )

        val spleisExecutor = CommandExecutor(
            session = session,
            command = Godkjenningsbehov(
                id = eventId,
                fødselsnummer = "134896",
                periodeFom = LocalDate.of(2018, 1, 1),
                periodeTom = LocalDate.of(2018, 1, 20),
                vedtaksperiodeId = vedtaksperiodeId,
                aktørId = "47839",
                orgnummer = "98765433",
                speilSnapshotRestClient = failingSpeilSnapshotDao
            ),
            spesialistOid = UUID.randomUUID(),
            eventId = eventId,
            nåværendeOppgave = null,
            loggingData = *arrayOf()
        )
        spleisExecutor.execute()
        spleisExecutor.resume(
            session,
            løsningify(
                HentPersoninfoLøsning(
                    "Test",
                    "Mellomnavn",
                    "Etternavnsen",
                    LocalDate.now(),
                    Kjønn.Mann
                ),
                HentEnhetLøsning("3417"),
                HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning())
            )
        )
        assertThrows<Exception> {
            spleisExecutor.execute()
        }

        using(sessionOf(dataSource)) { session ->
            assertEquals("OpprettVedtakCommand", session.findNåværendeOppgave(eventId)?.oppgaveType)
        }
    }

    private fun findVedtaksperiode(vedtaksperiodeId: UUID): Int? = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf("SELECT id FROM vedtak WHERE vedtaksperiode_id=?;", vedtaksperiodeId)
                .map { it.int("id") }
                .asSingle
        )
    }
}

private fun infotrygdutbetalingerLøsning(
    fom: LocalDate = LocalDate.of(2020, 1, 1),
    tom: LocalDate = LocalDate.of(2020, 1, 1),
    grad: Int = 100,
    dagsats: Double = 1200.0,
    typetekst: String = "ArbRef",
    orgnr: String = "89123"
) = objectMapper.readTree(
    """
            [
                {
                    "fom": "$fom",
                    "tom": "$tom",
                    "grad": "$grad",
                    "dagsats": $dagsats,
                    "typetekst": "$typetekst",
                    "organisasjonsnummer": "$orgnr"
                }
            ]
        """.trimIndent()
)

package no.nav.helse

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.modell.Godkjenningsbehov
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverLøsning
import no.nav.helse.modell.command.Command
import no.nav.helse.modell.command.CommandExecutor
import no.nav.helse.modell.command.OppgaveDao
import no.nav.helse.modell.person.*
import no.nav.helse.modell.vedtak.snapshot.SnapshotDao
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestDao
import no.nav.helse.modell.vedtak.SaksbehandlerLøsning
import no.nav.helse.modell.vedtak.VedtakDao
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
    private val personDao: PersonDao =
        PersonDao(dataSource)
    private val arbeidsgiverDao: ArbeidsgiverDao =
        ArbeidsgiverDao(dataSource)
    private val vedtakDao: VedtakDao =
        VedtakDao(dataSource)
    private val snapshotDao: SnapshotDao =
        SnapshotDao(dataSource)
    private val oppgaveDao: OppgaveDao =
        OppgaveDao(dataSource)
    private val speilSnapshotRestDao: SpeilSnapshotRestDao =
        SpeilSnapshotRestDao(
            spleisMockClient.client,
            accessTokenClient,
            "spleisClientId"
        )
    private val testDao: TestPersonDao = TestPersonDao(dataSource)

    @Test
    fun `godkjenningsbehov for ny person legger inn ny person i DB`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val spleisExecutor = CommandExecutor(
            command = Godkjenningsbehov(
                id = eventId,
                fødselsnummer = "12345",
                periodeFom = LocalDate.of(2018, 1, 1),
                periodeTom = LocalDate.of(2018, 1, 20),
                vedtaksperiodeId = vedtaksperiodeId,
                aktørId = "123455",
                orgnummer = "98765432",
                personDao = personDao,
                arbeidsgiverDao = arbeidsgiverDao,
                vedtakDao = vedtakDao,
                snapshotDao = snapshotDao,
                speilSnapshotRestDao = speilSnapshotRestDao
            ),
            spesialistOid = UUID.randomUUID(),
            eventId = eventId,
            nåværendeOppgave = null,
            oppgaveDao = oppgaveDao,
            vedtakDao = vedtakDao,
            loggingData = *arrayOf()
        )
        spleisExecutor.execute()
        spleisExecutor.fortsett(
            HentPersoninfoLøsning(
                "Test",
                "Mellomnavn",
                "Etternavnsen",
                LocalDate.now(),
                Kjønn.Mann
            )
        )
        spleisExecutor.fortsett(HentEnhetLøsning("3417"))
        spleisExecutor.execute()

        spleisExecutor.fortsett(HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning()))
        spleisExecutor.execute()

        assertNotNull(personDao.findPersonByFødselsnummer(12345))
    }

    @Test
    fun `godkjenningsbehov for person oppdaterer person i DB`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val spleisExecutor = CommandExecutor(
            command = Godkjenningsbehov(
                id = eventId,
                fødselsnummer = "13245",
                periodeFom = LocalDate.of(2018, 1, 1),
                periodeTom = LocalDate.of(2018, 1, 20),
                vedtaksperiodeId = vedtaksperiodeId,
                aktørId = "13245",
                orgnummer = "98765432",
                personDao = personDao,
                arbeidsgiverDao = arbeidsgiverDao,
                vedtakDao = vedtakDao,
                snapshotDao = snapshotDao,
                speilSnapshotRestDao = speilSnapshotRestDao
            ),
            spesialistOid = UUID.randomUUID(),
            eventId = eventId,
            nåværendeOppgave = null,
            oppgaveDao = oppgaveDao,
            vedtakDao = vedtakDao,
            loggingData = *arrayOf()
        )
        spleisExecutor.execute()
        spleisExecutor.fortsett(
            HentPersoninfoLøsning(
                "Test",
                "Mellomnavn",
                "Etternavnsen",
                LocalDate.now(),
                Kjønn.Mann
            )
        )
        spleisExecutor.fortsett(HentEnhetLøsning("3417"))
        spleisExecutor.fortsett(HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning()))
        testDao.setEnhetOppdatert(13245, LocalDate.of(2020, 1, 1))
        spleisExecutor.execute()
        spleisExecutor.fortsett(HentEnhetLøsning("3117"))
        spleisExecutor.execute()

        assertNotNull(personDao.findPersonByFødselsnummer(13245))
        assertEquals(LocalDate.now(), personDao.findEnhetSistOppdatert(13245))
    }

    @Test
    fun `godkjenningsbehov for person med ny arbeidsgiver legger inn ny arbeidsgiver i DB`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val spleisExecutor = CommandExecutor(
            command = Godkjenningsbehov(
                id = eventId,
                fødselsnummer = "23456",
                periodeFom = LocalDate.of(2018, 1, 1),
                periodeTom = LocalDate.of(2018, 1, 20),
                vedtaksperiodeId = vedtaksperiodeId,
                aktørId = "123455",
                orgnummer = "98765432",
                personDao = personDao,
                arbeidsgiverDao = arbeidsgiverDao,
                vedtakDao = vedtakDao,
                snapshotDao = snapshotDao,
                speilSnapshotRestDao = speilSnapshotRestDao
            ),
            spesialistOid = UUID.randomUUID(),
            eventId = eventId,
            nåværendeOppgave = null,
            oppgaveDao = oppgaveDao,
            vedtakDao = vedtakDao,
            loggingData = *arrayOf()
        )
        spleisExecutor.execute()
        spleisExecutor.fortsett(
            HentPersoninfoLøsning(
                "Test",
                "Mellomnavn",
                "Etternavnsen",
                LocalDate.now(),
                Kjønn.Mann
            )
        )
        spleisExecutor.fortsett(HentEnhetLøsning("3417"))
        spleisExecutor.fortsett(HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning()))
        spleisExecutor.execute()

        spleisExecutor.fortsett(ArbeidsgiverLøsning("NAV IKT"))
        assertNotNull(arbeidsgiverDao.findArbeidsgiverByOrgnummer(98765432))
    }

    @Test
    fun `Ved nytt godkjenningsbehov opprettes et nytt vedtak i DB`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val spleisExecutor = CommandExecutor(
            command = Godkjenningsbehov(
                id = eventId,
                fødselsnummer = "34567",
                periodeFom = LocalDate.of(2018, 1, 1),
                periodeTom = LocalDate.of(2018, 1, 20),
                vedtaksperiodeId = vedtaksperiodeId,
                aktørId = "123455",
                orgnummer = "98765433",
                personDao = personDao,
                arbeidsgiverDao = arbeidsgiverDao,
                vedtakDao = vedtakDao,
                snapshotDao = snapshotDao,
                speilSnapshotRestDao = speilSnapshotRestDao
            ),
            spesialistOid = UUID.randomUUID(),
            eventId = eventId,
            nåværendeOppgave = null,
            oppgaveDao = oppgaveDao,
            vedtakDao = vedtakDao,
            loggingData = *arrayOf()
        )
        spleisExecutor.execute()
        spleisExecutor.fortsett(
            HentPersoninfoLøsning(
                "Test",
                "Mellomnavn",
                "Etternavnsen",
                LocalDate.now(),
                Kjønn.Mann
            )
        )
        spleisExecutor.fortsett(HentEnhetLøsning("3417"))
        spleisExecutor.fortsett(HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning()))
        spleisExecutor.execute()
        spleisExecutor.fortsett(ArbeidsgiverLøsning("NAV IKT"))
        spleisExecutor.execute()

        assertNotNull(findVedtaksperiode(vedtaksperiodeId))
        val saksbehandlerOppgaver = oppgaveDao.findSaksbehandlerOppgaver()
        assertFalse(saksbehandlerOppgaver.isNullOrEmpty())
    }

    @Test
    fun `ved godkjenning har spleisbehovet en løsning`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val spleisExecutor = CommandExecutor(
            command = Godkjenningsbehov(
                id = eventId,
                fødselsnummer = "3457812",
                periodeFom = LocalDate.of(2018, 1, 1),
                periodeTom = LocalDate.of(2018, 1, 20),
                vedtaksperiodeId = vedtaksperiodeId,
                aktørId = "123455",
                orgnummer = "98765433",
                personDao = personDao,
                arbeidsgiverDao = arbeidsgiverDao,
                vedtakDao = vedtakDao,
                snapshotDao = snapshotDao,
                speilSnapshotRestDao = speilSnapshotRestDao
            ),
            spesialistOid = UUID.randomUUID(),
            eventId = eventId,
            nåværendeOppgave = null,
            oppgaveDao = oppgaveDao,
            vedtakDao = vedtakDao,
            loggingData = *arrayOf()
        )
        spleisExecutor.execute()
        spleisExecutor.fortsett(
            HentPersoninfoLøsning(
                "Test",
                "Mellomnavn",
                "Etternavnsen",
                LocalDate.now(),
                Kjønn.Mann
            )
        )
        spleisExecutor.fortsett(HentEnhetLøsning("3417"))
        spleisExecutor.fortsett(HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning()))
        spleisExecutor.execute()
        spleisExecutor.fortsett(ArbeidsgiverLøsning("NAV IKT"))
        spleisExecutor.execute()
        spleisExecutor.fortsett(
            SaksbehandlerLøsning(
                godkjent = true,
                saksbehandlerIdent = "abcd",
                godkjenttidspunkt = LocalDateTime.now(),
                oid = UUID.randomUUID(),
                epostadresse = "epost"
            )
        )

        val resultat = spleisExecutor.execute()

        assertTrue(resultat.any { it is Command.Resultat.Ok.Løst })
    }

    @Test
    fun `ved feilende command lagres den siste commanden som ikke ble ferdigstilt`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        val failingSpeilSnapshotDao = SpeilSnapshotRestDao(
            failingHttpClient(),
            accessTokenClient,
            "spleisClientId"
        )

        val spleisExecutor = CommandExecutor(
            command = Godkjenningsbehov(
                id = eventId,
                fødselsnummer = "134896",
                periodeFom = LocalDate.of(2018, 1, 1),
                periodeTom = LocalDate.of(2018, 1, 20),
                vedtaksperiodeId = vedtaksperiodeId,
                aktørId = "47839",
                orgnummer = "98765433",
                personDao = personDao,
                arbeidsgiverDao = arbeidsgiverDao,
                vedtakDao = vedtakDao,
                snapshotDao = snapshotDao,
                speilSnapshotRestDao = failingSpeilSnapshotDao
            ),
            spesialistOid = UUID.randomUUID(),
            eventId = eventId,
            nåværendeOppgave = null,
            oppgaveDao = oppgaveDao,
            vedtakDao = vedtakDao,
            loggingData = *arrayOf()
        )
        spleisExecutor.execute()
        spleisExecutor.fortsett(
            HentPersoninfoLøsning(
                "Test",
                "Mellomnavn",
                "Etternavnsen",
                LocalDate.now(),
                Kjønn.Mann
            )
        )
        spleisExecutor.execute()
        spleisExecutor.fortsett(HentEnhetLøsning("3417"))
        spleisExecutor.execute()
        spleisExecutor.fortsett(HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning()))
        assertThrows<Exception> {
            spleisExecutor.execute()
        }

        assertEquals("OpprettVedtakCommand", oppgaveDao.findNåværendeOppgave(eventId)?.oppgaveType)
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
        """.trimIndent())

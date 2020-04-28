package no.nav.helse

import com.fasterxml.jackson.databind.node.ObjectNode
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.mediator.kafka.SpleisbehovMediator
import no.nav.helse.mediator.kafka.meldinger.GodkjenningMessage
import no.nav.helse.mediator.kafka.meldinger.TilInfotrygdMessage
import no.nav.helse.modell.dao.ArbeidsgiverDao
import no.nav.helse.modell.dao.OppgaveDao
import no.nav.helse.modell.dao.PersonDao
import no.nav.helse.modell.dao.SnapshotDao
import no.nav.helse.modell.dao.SpeilSnapshotRestDao
import no.nav.helse.modell.dao.SpleisbehovDao
import no.nav.helse.modell.dao.VedtakDao
import no.nav.helse.modell.løsning.HentEnhetLøsning
import no.nav.helse.modell.løsning.HentPersoninfoLøsning
import no.nav.helse.modell.løsning.SaksbehandlerLøsning
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SpleisbehovEndToEndTest {
    private lateinit var dataSource: DataSource
    private lateinit var personDao: PersonDao
    private lateinit var arbeidsgiverDao: ArbeidsgiverDao
    private lateinit var vedtakDao: VedtakDao
    private lateinit var snapshotDao: SnapshotDao
    private lateinit var oppgaveDao: OppgaveDao
    private lateinit var speilSnapshotRestDao: SpeilSnapshotRestDao
    private lateinit var spleisbehovDao: SpleisbehovDao
    private lateinit var testDao: TestPersonDao

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
        spleisbehovDao = SpleisbehovDao(dataSource)
        testDao = TestPersonDao(dataSource)
    }

    @ExperimentalContracts
    @Test
    fun `Spleisbehov persisteres`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val rapid = TestRapid()
        val spleisbehovMediator = SpleisbehovMediator(
            spleisbehovDao = spleisbehovDao,
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao,
            vedtakDao = vedtakDao,
            snapshotDao = snapshotDao,
            speilSnapshotRestDao = speilSnapshotRestDao,
            oppgaveDao = oppgaveDao,
            spesialistOID = spesialistOID
        ).apply { init(rapid) }
        val spleisbehovId = UUID.randomUUID()
        val godkjenningMessage = GodkjenningMessage(
            id = spleisbehovId,
            fødselsnummer = "12345",
            aktørId = "12345",
            organisasjonsnummer = "89123",
            vedtaksperiodeId = vedtaksperiodeId,
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 31)
        )
        spleisbehovMediator.håndter(godkjenningMessage, "{}")
        assertEquals(2, rapid.inspektør.size)
        assertNotNull(spleisbehovDao.findBehov(spleisbehovId))
        spleisbehovMediator.håndter(
            spleisbehovId,
            HentEnhetLøsning("1234"),
            HentPersoninfoLøsning("Test", null, "Testsen")
        )
        val saksbehandlerOppgaver = oppgaveDao.findSaksbehandlerOppgaver()
        val vedtakRef = vedtakDao.findVedtaksperiode(vedtaksperiodeId)
        customAssertNotNull(vedtakRef)
        customAssertNotNull(saksbehandlerOppgaver)
        assertTrue(saksbehandlerOppgaver.any { it.vedtaksref == vedtakRef.toLong() })

        spleisbehovMediator.håndter(
            spleisbehovId, SaksbehandlerLøsning(
                godkjent = true,
                saksbehandlerIdent = "abcd",
                godkjenttidspunkt = LocalDateTime.now(),
                oid = UUID.randomUUID(),
                epostadresse = "epost"
            )
        )
        assertEquals(5, rapid.inspektør.size)
        val løsning = 0.until(rapid.inspektør.size)
            .map(rapid.inspektør::message)
            .first { it.hasNonNull("@løsning") }
            .path("@løsning")

        customAssertNotNull(løsning)

        løsning as ObjectNode
        assertEquals(listOf("Godkjenning"), løsning.fieldNames().asSequence().toList())
    }

    @ExperimentalContracts
    @Test
    fun `Vedtaksperioder som går til infotrygd invaliderer oppgaver`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val rapid = TestRapid()
        val spleisbehovMediator = SpleisbehovMediator(
            spleisbehovDao = spleisbehovDao,
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao,
            vedtakDao = vedtakDao,
            snapshotDao = snapshotDao,
            speilSnapshotRestDao = speilSnapshotRestDao,
            oppgaveDao = oppgaveDao,
            spesialistOID = spesialistOID
        ).apply { init(rapid) }
        TilInfotrygdMessage.Factory(rapid, spleisbehovMediator)
        val spleisbehovId = UUID.randomUUID()
        val godkjenningMessage = GodkjenningMessage(
            id = spleisbehovId,
            fødselsnummer = "12345",
            aktørId = "12345",
            organisasjonsnummer = "89123",
            vedtaksperiodeId = vedtaksperiodeId,
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 31)
        )
        spleisbehovMediator.håndter(godkjenningMessage, "{}")

        rapid.sendTestMessage(
            """
            {
            "@event_name": "vedtaksperiode_endret",
            "vedtaksperiodeId": "$vedtaksperiodeId",
            "gjeldendeTilstand": "TIL_INFOTRYGD"
            }
        """
        )
        val oppgavestatus = using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT * FROM oppgave where behov_id=?",
                    spleisbehovId
                ).map { Oppgavestatus.valueOf(it.string("status")) }
                    .asSingle
            )
        }
        assertEquals(Oppgavestatus.Invalidert, oppgavestatus)
    }
}

@ExperimentalContracts
fun customAssertNotNull(value: Any?) {
    contract { returns() implies (value is Any) }
    assertNotNull(value)
}

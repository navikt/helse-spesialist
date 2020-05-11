package no.nav.helse

import com.fasterxml.jackson.databind.node.ObjectNode
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.mediator.kafka.SpleisbehovMediator
import no.nav.helse.mediator.kafka.meldinger.GodkjenningMessage
import no.nav.helse.mediator.kafka.meldinger.TilInfotrygdMessage
import no.nav.helse.mediator.kafka.meldinger.VedtaksperiodeEndretMessage
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.command.OppgaveDao
import no.nav.helse.modell.command.SpleisbehovDao
import no.nav.helse.modell.person.HentEnhetLøsning
import no.nav.helse.modell.person.HentPersoninfoLøsning
import no.nav.helse.modell.person.Kjønn
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.vedtak.SaksbehandlerLøsning
import no.nav.helse.modell.vedtak.VedtakDao
import no.nav.helse.modell.vedtak.snapshot.SnapshotDao
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestDao
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.vedtaksperiode.VedtaksperiodeDao
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

internal class GodkjenningsbehovEndToEndTest {
    private val spleisMockClient = SpleisMockClient()
    private val accessTokenClient = accessTokenClient()

    private val dataSource = setupDataSourceMedFlyway()
    private val personDao = PersonDao(dataSource)
    private val arbeidsgiverDao = ArbeidsgiverDao(dataSource)
    private val vedtakDao = VedtakDao(dataSource)
    private val snapshotDao = SnapshotDao(dataSource)
    private val oppgaveDao = OppgaveDao(dataSource)
    private val speilSnapshotRestDao = SpeilSnapshotRestDao(
        spleisMockClient.client,
        accessTokenClient,
        "spleisClientId"
    )
    private val spleisbehovDao = SpleisbehovDao(dataSource)
    private val vedtaksperiodeDao = VedtaksperiodeDao(dataSource)

    private val spesialistOID: UUID = UUID.randomUUID()

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
        GodkjenningMessage.Factory(rapid, spleisbehovMediator)

        val spleisbehovId = UUID.randomUUID()
        rapid.sendTestMessage(
            """
            {
              "@behov": ["Godkjenning"],
              "@id": "$spleisbehovId",
              "fødselsnummer": "12345",
              "aktørId": "12345",
              "organisasjonsnummer": "89123",
              "vedtaksperiodeId": "$vedtaksperiodeId",
              "periodeFom": "${LocalDate.of(2018, 1, 1)}",
              "periodeTom": "${LocalDate.of(2018, 1, 31)}",
              "warnings": {"aktiviteter": []}
            }
        """
        )
        assertEquals(2, rapid.inspektør.size)
        assertNotNull(spleisbehovDao.findBehov(spleisbehovId))
        spleisbehovMediator.håndter(
            spleisbehovId,
            HentEnhetLøsning("1234"),
            HentPersoninfoLøsning(
                "Test",
                null,
                "Testsen",
                LocalDate.now(),
                Kjønn.Mann
            )
        )
        val saksbehandlerOppgaver = oppgaveDao.findSaksbehandlerOppgaver()
        assertFalse(saksbehandlerOppgaver.isEmpty())
        assertTrue(saksbehandlerOppgaver.any { it.vedtaksperiodeId == vedtaksperiodeId })

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
    fun `Mottar godkjennings message med warning fra topic`() {
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
        GodkjenningMessage.Factory(rapid, spleisbehovMediator)

        val spleisbehovId = UUID.randomUUID()
        val warningTekst = "Infotrygd inneholder utbetalinger med varierende dagsats for en sammenhengende periode"
        val warningsJson = """
            {
              "aktiviteter": [
                {
                  "kontekster": [
                    {
                      "kontekstType": "Ytelser",
                      "kontekstMap": {}
                    },
                    {
                      "kontekstType": "Person",
                      "kontekstMap": {
                        "fødselsnummer": "12345",
                        "aktørId": "12345"
                      }
                    },
                    {
                      "kontekstType": "Arbeidsgiver",
                      "kontekstMap": {
                        "organisasjonsnummer": "89123"
                      }
                    },
                    {
                      "kontekstType": "Vedtaksperiode",
                      "kontekstMap": {
                        "vedtaksperiodeId": "$vedtaksperiodeId"
                      }
                    },
                    {
                      "kontekstType": "Tilstand",
                      "kontekstMap": {
                        "tilstand": "AVVENTER_GAP"
                      }
                    }
                  ],
                  "alvorlighetsgrad": "WARN",
                  "melding": "$warningTekst",
                  "detaljer": {},
                  "tidsstempel": "2020-05-05 09:09:01.797"
                }
              ]
            }
        """
        rapid.sendTestMessage(
            """
            {
              "@behov": ["Godkjenning"],
              "@id": "$spleisbehovId",
              "fødselsnummer": "12345",
              "aktørId": "12345",
              "organisasjonsnummer": "89123",
              "vedtaksperiodeId": "$vedtaksperiodeId",
              "periodeFom": "${LocalDate.of(2018, 1, 1)}",
              "periodeTom": "${LocalDate.of(2018, 1, 31)}",
              "warnings": $warningsJson
            }
        """
        )

        val warnings = using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT * FROM warning where spleisbehov_ref=?",
                    spleisbehovId
                ).map { it.string("melding") }.asList
            )
        }
        assertEquals(listOf(warningTekst), warnings)

        spleisbehovMediator.håndter(
            spleisbehovId,
            HentEnhetLøsning("1234"),
            HentPersoninfoLøsning(
                "Test",
                null,
                "Testsen",
                LocalDate.now(),
                Kjønn.Kvinne
            )
        )
        val saksbehandlerOppgaver = oppgaveDao.findSaksbehandlerOppgaver()
        assertEquals(1, saksbehandlerOppgaver.first { it.vedtaksperiodeId == vedtaksperiodeId }.antallVarsler)
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
        GodkjenningMessage.Factory(rapid, spleisbehovMediator)

        val spleisbehovId = UUID.randomUUID()
        rapid.sendTestMessage(
            """
            {
              "@behov": ["Godkjenning"],
              "@id": "$spleisbehovId",
              "fødselsnummer": "12345",
              "aktørId": "12345",
              "organisasjonsnummer": "89123",
              "vedtaksperiodeId": "$vedtaksperiodeId",
              "periodeFom": "${LocalDate.of(2018, 1, 1)}",
              "periodeTom": "${LocalDate.of(2018, 1, 31)}",
              "warnings": {"aktiviteter": []}
            }
        """
        )

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

    @ExperimentalContracts
    @Test
    fun `ignorerer TIL_INFOTRYGD når et spleisbehov er fullført`() {
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
            fødselsnummer = "456546",
            aktørId = "7345626534",
            organisasjonsnummer = "756876",
            vedtaksperiodeId = vedtaksperiodeId,
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 31),
            warnings = emptyList()
        )
        spleisbehovMediator.håndter(godkjenningMessage, "{}")
        spleisbehovMediator.håndter(
            spleisbehovId,
            HentEnhetLøsning("1234"),
            HentPersoninfoLøsning(
                "Test",
                null,
                "Testsen",
                LocalDate.now(),
                Kjønn.Mann
            )
        )

        spleisbehovMediator.håndter(
            spleisbehovId, SaksbehandlerLøsning(
                godkjent = false,
                saksbehandlerIdent = "abcd",
                godkjenttidspunkt = LocalDateTime.now(),
                oid = UUID.randomUUID(),
                epostadresse = "epost"
            )
        )

        assertDoesNotThrow {
            spleisbehovMediator.håndter(vedtaksperiodeId, TilInfotrygdMessage())
        }
    }

    @Test
    fun `gjør ingen ting om man får en løsning på en invalidert oppgave`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

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
        GodkjenningMessage.Factory(rapid, spleisbehovMediator)

        rapid.sendTestMessage(
            """
            {
              "@behov": ["Godkjenning"],
              "@id": "$eventId",
              "fødselsnummer": "3546756",
              "aktørId": "7653345",
              "organisasjonsnummer": "6546346",
              "vedtaksperiodeId": "$vedtaksperiodeId",
              "periodeFom": "${LocalDate.of(2018, 1, 1)}",
              "periodeTom": "${LocalDate.of(2018, 1, 31)}",
              "warnings": {"aktiviteter": []}
            }
        """
        )
        spleisbehovMediator.håndter(vedtaksperiodeId, TilInfotrygdMessage())
        spleisbehovMediator.håndter(
            eventId,
            HentEnhetLøsning("1234"),
            HentPersoninfoLøsning(
                "Test",
                null,
                "Testsen",
                LocalDate.now(),
                Kjønn.Mann
            )
        )

        assertEquals(Oppgavestatus.Invalidert, oppgaveDao.findNåværendeOppgave(eventId)?.status)
    }

    @ExperimentalContracts
    @Test
    fun `vedtaksperiode_endret fører til oppdatert speil snapshot`() {
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
        GodkjenningMessage.Factory(rapid, spleisbehovMediator)

        val spleisbehovId = UUID.randomUUID()
        val fødselsnummer = "3546756"
        rapid.sendTestMessage(
            """
            {
              "@behov": ["Godkjenning"],
              "@id": "$spleisbehovId",
              "fødselsnummer": "$fødselsnummer",
              "aktørId": "7653345",
              "organisasjonsnummer": "6546346",
              "vedtaksperiodeId": "$vedtaksperiodeId",
              "periodeFom": "${LocalDate.of(2018, 1, 1)}",
              "periodeTom": "${LocalDate.of(2018, 1, 31)}",
              "warnings": {"aktiviteter": []}
            }
        """
        )
        spleisbehovMediator.håndter(
            spleisbehovId,
            HentEnhetLøsning("1234"),
            HentPersoninfoLøsning(
                "Test",
                null,
                "Testsen",
                LocalDate.now(),
                Kjønn.Mann
            )
        )

        val speilSnapshotRef = vedtaksperiodeDao.findVedtakByFnr(fødselsnummer)!!.arbeidsgiverRef
        val snapshotFør = snapshotDao.findSpeilSnapshot(speilSnapshotRef)

        spleisMockClient.enqueueResponses(SpleisMockClient.VEDTAKSPERIODE_UTBETALT)
        spleisbehovMediator.håndter(
            vedtaksperiodeId,
            VedtaksperiodeEndretMessage(vedtaksperiodeId = vedtaksperiodeId, fødselsnummer = fødselsnummer)
        )

        val snapshotEtter = snapshotDao.findSpeilSnapshot(speilSnapshotRef)
        assertNotEquals(snapshotFør, snapshotEtter)
    }
}

@ExperimentalContracts
fun customAssertNotNull(value: Any?) {
    contract { returns() implies (value is Any) }
    assertNotNull(value)
}

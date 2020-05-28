package no.nav.helse

import com.fasterxml.jackson.databind.node.ObjectNode
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.mediator.kafka.SpleisbehovMediator
import no.nav.helse.mediator.kafka.meldinger.*
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.command.OppgaveDao
import no.nav.helse.modell.command.SpleisbehovDao
import no.nav.helse.modell.person.*
import no.nav.helse.modell.risiko.RisikoDao
import no.nav.helse.modell.vedtak.SaksbehandlerLøsning
import no.nav.helse.modell.vedtak.VedtakDao
import no.nav.helse.modell.vedtak.snapshot.SnapshotDao
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestDao
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.vedtaksperiode.VedtaksperiodeDao
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
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
    private val risikoDao = RisikoDao(dataSource)

    private val spesialistOID: UUID = UUID.randomUUID()

    private lateinit var spleisbehovMediator: SpleisbehovMediator
    private lateinit var vedtaksperiodeId: UUID
    private lateinit var spleisbehovId: UUID
    private lateinit var rapid: TestRapid

    @BeforeEach
    fun setup() {
        spleisbehovId = UUID.randomUUID()
        vedtaksperiodeId = UUID.randomUUID()
        rapid = TestRapid()
        spleisbehovMediator = SpleisbehovMediator(
            spleisbehovDao = spleisbehovDao,
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao,
            vedtakDao = vedtakDao,
            snapshotDao = snapshotDao,
            speilSnapshotRestDao = speilSnapshotRestDao,
            oppgaveDao = oppgaveDao,
            risikoDao = risikoDao,
            spesialistOID = spesialistOID
        ).apply { init(rapid) }
    }

    @ExperimentalContracts
    @Test
    fun `Spleisbehov persisteres`() {
        GodkjenningMessage.Factory(rapid, spleisbehovMediator)

        sendGodkjenningsbehov()

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
            ),
            HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning())
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
        GodkjenningMessage.Factory(rapid, spleisbehovMediator)

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
        sendGodkjenningsbehov(warnings = warningsJson)

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
            ),
            HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning())
        )
        val saksbehandlerOppgaver = oppgaveDao.findSaksbehandlerOppgaver()
        assertEquals(1, saksbehandlerOppgaver.first { it.vedtaksperiodeId == vedtaksperiodeId }.antallVarsler)
    }

    @Test
    fun `Persisterer løsning for HentInfotrygdutbetalinger`() {
        GodkjenningMessage.Factory(rapid, spleisbehovMediator)
        rapid.sendTestMessage(godkjenningbehov(spleisbehovId, vedtaksperiodeId))

        PersoninfoLøsningMessage.Factory(rapid, spleisbehovMediator)
        rapid.sendTestMessage(infotrygdutbetalingerLøsningJson(spleisbehovId, vedtaksperiodeId))

        val utbetaling = using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT * FROM infotrygdutbetalinger WHERE id=?",
                    1
                ).map { it.string("data") }.asSingle
            )
        }
        assertNotNull(utbetaling)
    }

    @ExperimentalContracts
    @Test
    fun `Vedtaksperioder som går til infotrygd invaliderer oppgaver`() {
        TilInfotrygdMessage.Factory(rapid, spleisbehovMediator)
        GodkjenningMessage.Factory(rapid, spleisbehovMediator)

        sendGodkjenningsbehov()

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
            ),
            HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning())
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
        GodkjenningMessage.Factory(rapid, spleisbehovMediator)
        sendGodkjenningsbehov(aktørId = "7653345", fødselsnummer = "3546756", organisasjonsnummer = "6546346")
        spleisbehovMediator.håndter(vedtaksperiodeId, TilInfotrygdMessage())
        spleisbehovMediator.håndter(
            spleisbehovId,
            HentEnhetLøsning("1234"),
            HentPersoninfoLøsning(
                "Test",
                null,
                "Testsen",
                LocalDate.now(),
                Kjønn.Mann
            ),
            HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning())
        )

        assertEquals(Oppgavestatus.Invalidert, oppgaveDao.findNåværendeOppgave(spleisbehovId)?.status)
    }

    @ExperimentalContracts
    @Test
    fun `vedtaksperiode_endret fører til oppdatert speil snapshot`() {
        GodkjenningMessage.Factory(rapid, spleisbehovMediator)
        VedtaksperiodeEndretMessage.Factory(rapid, spleisbehovMediator)

        val fødselsnummer = "3546756"
        val aktørId = "7653345"
        val orgnummer = "6546346"

        sendGodkjenningsbehov(aktørId = aktørId, fødselsnummer = fødselsnummer, organisasjonsnummer = orgnummer)

        spleisbehovMediator.håndter(
            spleisbehovId,
            HentEnhetLøsning("1234"),
            HentPersoninfoLøsning(
                "Test",
                null,
                "Testsen",
                LocalDate.now(),
                Kjønn.Mann
            ),
            HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning())
        )

        val speilSnapshotRef = vedtaksperiodeDao.findVedtakByFnr(fødselsnummer)!!.arbeidsgiverRef
        val snapshotFør = snapshotDao.findSpeilSnapshot(speilSnapshotRef)

        spleisMockClient.enqueueResponses(SpleisMockClient.VEDTAKSPERIODE_UTBETALT)
        sendVedtaksperiodeEndretEvent(aktørId, fødselsnummer, orgnummer)

        val snapshotEtter = snapshotDao.findSpeilSnapshot(speilSnapshotRef)
        assertNotEquals(snapshotFør, snapshotEtter)
    }

    @ExperimentalContracts
    @Test
    fun `risikovurderinger persisteres`() {
        RisikovurderingMessage.Factory(rapid, spleisbehovMediator)

        rapid.sendTestMessage(
            """
            {
              "@event_name": "risikovurdering",
              "@id": "${UUID.randomUUID()}",
              "@opprettet": "${LocalDateTime.now()}",
              "vedtaksperiodeId": "$vedtaksperiodeId",
              "samletScore": 10,
              "begrunnelser": ["Detta ser ikke bra ut"],
              "ufullstendig": true
            }
        """
        )

        assertNotNull(using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf("SELECT id FROM risikovurdering WHERE vedtaksperiode_id=?;", vedtaksperiodeId)
                    .map { it.int("id") }
                    .asSingle
            )
        })
    }

    private fun sendVedtaksperiodeEndretEvent(aktørId: String, fødselsnummer: String, organisasjonsnummer: String) {
        rapid.sendTestMessage(
            """
            {
              "vedtaksperiodeId": "$vedtaksperiodeId",
              "gjeldendeTilstand": "AVSLUTTET",
              "forrigeTilstand": "TIL_UTBETALING",
              "@event_name": "vedtaksperiode_endret",
              "@id": "${UUID.randomUUID()}",
              "@opprettet": "${LocalDateTime.now()}",
              "aktørId": "$aktørId",
              "fødselsnummer": "$fødselsnummer",
              "organisasjonsnummer": "$organisasjonsnummer"
            }
        """
        )
    }

    private fun sendGodkjenningsbehov(
        aktørId: String = "12345",
        fødselsnummer: String = "12345",
        organisasjonsnummer: String = "89123",
        warnings: String = "{\"aktiviteter\": []}"
    ) {
        rapid.sendTestMessage(
            """
            {
              "@behov": ["Godkjenning"],
              "@id": "$spleisbehovId",
              "fødselsnummer": "$fødselsnummer",
              "aktørId": "$aktørId",
              "organisasjonsnummer": "$organisasjonsnummer",
              "vedtaksperiodeId": "$vedtaksperiodeId",
              "periodeFom": "${LocalDate.of(2018, 1, 1)}",
              "periodeTom": "${LocalDate.of(2018, 1, 31)}",
              "warnings": $warnings
            }
        """
        )
    }
}

@ExperimentalContracts
fun customAssertNotNull(value: Any?) {
    contract { returns() implies (value is Any) }
    assertNotNull(value)
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

private fun godkjenningbehov(
    spleisbehovId: UUID,
    vedtaksperiodeId: UUID,
    fnr: String = "12345",
    orgnr: String = "89123"
) = """
{
  "@behov": ["Godkjenning"],
  "@id": "$spleisbehovId",
  "fødselsnummer": "$fnr",
  "aktørId": "12345",
  "organisasjonsnummer": "$orgnr",
  "vedtaksperiodeId": "$vedtaksperiodeId",
  "periodeFom": "${LocalDate.of(2018, 1, 1)}",
  "periodeTom": "${LocalDate.of(2018, 1, 31)}",
  "warnings": {"aktiviteter": []}
}
"""

private fun infotrygdutbetalingerLøsningJson(
    spleisbehovId: UUID,
    vedtaksperiodeId: UUID,
    fnr: String = "12345",
    orgnr: String = "89123"
) = """
{
    "@event_name" : "behov",
    "@final": true,
    "@behov" : [ "HentEnhet", "HentPersoninfo", "HentInfotrygdutbetalinger" ],
    "@id" : "id",
    "@opprettet" : "2020-05-18",
    "spleisBehovId" : "$spleisbehovId",
    "vedtaksperiodeId" : "$vedtaksperiodeId",
    "fødselsnummer" : "$fnr",
    "orgnummer" : "$orgnr",
    "HentInfotrygdutbetalinger" : {
        "historikkFom" : "2017-05-18",
        "historikkTom" : "2020-05-18"
    },
    "system_read_count" : 0,
    "@løsning" : {
        "HentInfotrygdutbetalinger" : [ {
            "fom" : "2018-01-19",
            "tom" : "2018-01-23",
            "dagsats" : 870.0,
            "grad" : "100",
            "typetekst" : "ArbRef",
            "organisasjonsnummer" : "80000000"
        } ]
    }
}""".trimIndent()

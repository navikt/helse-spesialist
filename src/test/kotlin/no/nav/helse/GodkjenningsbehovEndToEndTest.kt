package no.nav.helse

import AbstractEndToEndTest
import com.fasterxml.jackson.databind.node.ObjectNode
import io.mockk.every
import io.mockk.mockk
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.mediator.kafka.HendelseMediator
import no.nav.helse.mediator.kafka.MiljøstyrtFeatureToggle
import no.nav.helse.mediator.kafka.meldinger.RisikovurderingLøsning
import no.nav.helse.modell.command.findBehov
import no.nav.helse.modell.command.findSaksbehandlerOppgaver
import no.nav.helse.modell.person.HentEnhetLøsning
import no.nav.helse.modell.person.HentInfotrygdutbetalingerLøsning
import no.nav.helse.modell.person.HentPersoninfoLøsning
import no.nav.helse.modell.person.Kjønn
import no.nav.helse.modell.vedtak.SaksbehandlerLøsning
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.modell.vedtak.snapshot.findSpeilSnapshot
import no.nav.helse.vedtaksperiode.findVedtakByFnr
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@Disabled
class GodkjenningsbehovEndToEndTest : AbstractEndToEndTest() {
    private val spleisMockClient = SpleisMockClient()
    private val accessTokenClient = accessTokenClient()

    private lateinit var session: Session
    private val speilSnapshotRestClient = SpeilSnapshotRestClient(
        spleisMockClient.client,
        accessTokenClient,
        "spleisClientId"
    )
    private val spesialistOID: UUID = UUID.randomUUID()

    private lateinit var spleisbehovMediator: HendelseMediator
    private lateinit var vedtaksperiodeId: UUID
    private lateinit var hendelseId: UUID

    private val featuretoggleMock = mockk<MiljøstyrtFeatureToggle> {
        every { risikovurdering() }.returns(false)
    }

    @BeforeAll
    fun setupAll() {
        spleisbehovMediator = HendelseMediator(
            rapidsConnection = testRapid,
            dataSource = dataSource,
            speilSnapshotRestClient = speilSnapshotRestClient,
            spesialistOID = spesialistOID,
            miljøstyrtFeatureToggle = featuretoggleMock
        )
    }

    @BeforeEach
    fun setup() {
        hendelseId = UUID.randomUUID()
        vedtaksperiodeId = UUID.randomUUID()
        session = sessionOf(dataSource, returnGeneratedKey = true)
        every { featuretoggleMock.risikovurdering() }.returns(false)
    }

    @AfterEach
    fun closeConnection() {
        session.close()
    }

    @ExperimentalContracts
    @Test
    fun `Spleisbehov persisteres`() {
        sendGodkjenningsbehov()

        assertEquals(2, testRapid.inspektør.size)
        assertNotNull(session.findBehov(hendelseId))
        spleisbehovMediator.håndter(
            hendelseId,
            HentEnhetLøsning("1234"),
            hentPersoninfoLøsning(),
            HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning())
        )
        val saksbehandlerOppgaver = sessionOf(dataSource).use { it.findSaksbehandlerOppgaver() }
        assertFalse(saksbehandlerOppgaver.isEmpty())
        assertTrue(saksbehandlerOppgaver.any { it.vedtaksperiodeId == vedtaksperiodeId })

        spleisbehovMediator.håndter(
            hendelseId, SaksbehandlerLøsning(
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
        assertEquals(5, testRapid.inspektør.size)
        val løsning = 0.until(testRapid.inspektør.size)
            .map(testRapid.inspektør::message)
            .first { it.hasNonNull("@løsning") }
            .path("@løsning")

        customAssertNotNull(løsning)

        løsning as ObjectNode
        assertEquals(listOf("Godkjenning"), løsning.fieldNames().asSequence().toList())
    }

    @Test
    fun `godkjenningsbehov med risikovurdering`() {
        every { featuretoggleMock.risikovurdering() }.returns(true)
        sendGodkjenningsbehov()

        spleisbehovMediator.håndter(
            hendelseId,
            HentEnhetLøsning("1234"),
            hentPersoninfoLøsning(),
            HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning())
        )
        spleisbehovMediator.håndter(hendelseId, risikovurderingLøsning(hendelseId, vedtaksperiodeId, listOf("8.4: Feil diagnose")))
        spleisbehovMediator.håndter(hendelseId, saksbehandlerLøsning())

        assertNotNull(finnLøsning("Godkjenning"))
    }


    @ExperimentalContracts
    @Test
    fun `Mottar godkjennings message med warning fra topic`() {
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

        val warnings = sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    "SELECT * FROM warning where hendelse_id=?",
                    hendelseId
                ).map { it.string("melding") }.asList
            )
        }
        assertEquals(listOf(warningTekst), warnings)

        spleisbehovMediator.håndter(
            hendelseId,
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
        val saksbehandlerOppgaver = sessionOf(dataSource).use { it.findSaksbehandlerOppgaver() }
        assertEquals(1, saksbehandlerOppgaver.first { it.vedtaksperiodeId == vedtaksperiodeId }.antallVarsler)
    }

    @Test
    fun `Persisterer og henter saksbehandleroppgavetype`() {
        sendGodkjenningsbehov(periodetype = Saksbehandleroppgavetype.INFOTRYGDFORLENGELSE)

        spleisbehovMediator.håndter(
            hendelseId,
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
        val saksbehandlerOppgaver = sessionOf(dataSource).use { it.findSaksbehandlerOppgaver() }
        assertEquals(
            Saksbehandleroppgavetype.INFOTRYGDFORLENGELSE,
            saksbehandlerOppgaver.first { it.vedtaksperiodeId == vedtaksperiodeId }.type
        )
    }

    @Test
    fun `Saksbehandleroppgavetype kan være null`() {
        sendGodkjenningsbehov(periodetype = null)

        spleisbehovMediator.håndter(
            hendelseId,
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
        val saksbehandlerOppgaver = sessionOf(dataSource).use { it.findSaksbehandlerOppgaver() }
        assertNull(saksbehandlerOppgaver.first { it.vedtaksperiodeId == vedtaksperiodeId }.type)
    }

    @Test
    fun `Advarsler dedupliseres i oppgaver til saksbehandler`() {
        val warningTekst = "Personen tjener alt for mye"
        val duplicatedWarningTekst =
            "Infotrygd inneholder utbetalinger med varierende dagsats for en sammenhengende periode"
        val warningsJson = """
            {
              "aktiviteter": [
                {
                  "alvorlighetsgrad": "WARN",
                  "melding": "$warningTekst",
                  "detaljer": {},
                  "tidsstempel": "2020-05-05 09:09:01.797"
                },
                {
                  "alvorlighetsgrad": "WARN",
                  "melding": "$duplicatedWarningTekst",
                  "detaljer": {},
                  "tidsstempel": "2020-05-05 19:09:01.797"
                },
                {
                  "alvorlighetsgrad": "WARN",
                  "melding": "$duplicatedWarningTekst",
                  "detaljer": {},
                  "tidsstempel": "2020-05-05 19:19:01.797"
                }
              ]
            }
        """
        sendGodkjenningsbehov(warnings = warningsJson)

        val warnings = sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    "SELECT * FROM warning where hendelse_id=?",
                    hendelseId
                ).map { it.string("melding") }.asList
            )
        }
        assertEquals(listOf(warningTekst, duplicatedWarningTekst, duplicatedWarningTekst), warnings)

        spleisbehovMediator.håndter(
            hendelseId,
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
        val saksbehandlerOppgaver = sessionOf(dataSource).use { it.findSaksbehandlerOppgaver() }
        assertEquals(2, saksbehandlerOppgaver.first { it.vedtaksperiodeId == vedtaksperiodeId }.antallVarsler)
    }

    @Test
    fun `Persisterer løsning for HentInfotrygdutbetalinger`() {
        testRapid.sendTestMessage(godkjenningbehov(hendelseId, vedtaksperiodeId))
        testRapid.sendTestMessage(personinfoLøsningJson(hendelseId, vedtaksperiodeId))

        val utbetaling = sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    "SELECT * FROM infotrygdutbetalinger WHERE id=?",
                    1
                ).map { it.string("data") }.asSingle
            )
        }
        assertNotNull(utbetaling)
    }

    @Test
    fun `Ignorerer løsning på behov dersom det ikke finnes noen nåværende oppgave`() {
        sendGodkjenningsbehov()

        spleisbehovMediator.håndter(
            hendelseId,
            HentEnhetLøsning("1234"),
            HentPersoninfoLøsning(
                "Test",
                null,
                "Testesen",
                LocalDate.now(),
                Kjønn.Kvinne
            ),
            HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning())
        )

        spleisbehovMediator.håndter(
            hendelseId, SaksbehandlerLøsning(
                godkjent = true,
                saksbehandlerIdent = "Tester",
                godkjenttidspunkt = LocalDateTime.now(),
                oid = UUID.randomUUID(),
                epostadresse = "test@testesen.test",
                årsak = null,
                begrunnelser = null,
                kommentar = null
            )
        )

        spleisbehovMediator.håndter(
            hendelseId,
            null,
            null,
            HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning())
        )
    }

    @ExperimentalContracts
    @Test
    fun `vedtaksperiode_endret fører til oppdatert speil snapshot`() {
        val fødselsnummer = "3546756"
        val aktørId = "7653345"
        val orgnummer = "6546346"

        sendGodkjenningsbehov(aktørId = aktørId, fødselsnummer = fødselsnummer, organisasjonsnummer = orgnummer)

        spleisbehovMediator.håndter(
            hendelseId,
            HentEnhetLøsning("1234"),
            hentPersoninfoLøsning(),
            HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning())
        )

        val speilSnapshotRef = session.findVedtakByFnr(fødselsnummer)!!.arbeidsgiverRef
        val snapshotFør = session.findSpeilSnapshot(speilSnapshotRef)

        spleisMockClient.enqueueResponses(SpleisMockClient.VEDTAKSPERIODE_UTBETALT)
        sendVedtaksperiodeEndretEvent(aktørId, fødselsnummer, orgnummer)

        val snapshotEtter = session.findSpeilSnapshot(speilSnapshotRef)
        assertNotEquals(snapshotFør, snapshotEtter)
    }

    private fun finnLøsning(behovstype: String): ObjectNode? {
        return 0.until(testRapid.inspektør.size)
            .map(testRapid.inspektør::message)
            .firstOrNull { it.hasNonNull("@behov") && it["@behov"].map { type -> type.textValue() }.contains(behovstype) && it.hasNonNull("@løsning") }
            ?.path("@løsning") as ObjectNode?
    }

    @ExperimentalContracts
    @Test
    fun `vedtaksperiode_forkastet fører til oppdatert speil snapshot`() {
        val fødselsnummer = "3546756"
        val aktørId = "7653345"
        val orgnummer = "6546346"

        sendGodkjenningsbehov(aktørId = aktørId, fødselsnummer = fødselsnummer, organisasjonsnummer = orgnummer)

        spleisbehovMediator.håndter(
            hendelseId,
            HentEnhetLøsning("1234"),
            hentPersoninfoLøsning(),
            HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning())
        )

        val speilSnapshotRef = session.findVedtakByFnr(fødselsnummer)!!.arbeidsgiverRef
        val snapshotFør = session.findSpeilSnapshot(speilSnapshotRef)

        spleisMockClient.enqueueResponses(SpleisMockClient.VEDTAKSPERIODE_UTBETALT)
        sendVedtaksperiodeForkastetEvent(aktørId, fødselsnummer, orgnummer)

        val snapshotEtter = session.findSpeilSnapshot(speilSnapshotRef)
        assertNotEquals(snapshotFør, snapshotEtter)
    }

    private fun sendVedtaksperiodeEndretEvent(aktørId: String, fødselsnummer: String, organisasjonsnummer: String) {
        testRapid.sendTestMessage(
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

    private fun sendVedtaksperiodeForkastetEvent(aktørId: String, fødselsnummer: String, organisasjonsnummer: String) {
        testRapid.sendTestMessage(
            """
            {
              "vedtaksperiodeId": "$vedtaksperiodeId",
              "tilstand": "TIL_INFOTRYGD",
              "@event_name": "vedtaksperiode_forkastet",
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
        warnings: String = "{\"aktiviteter\": []}",
        periodetype: Saksbehandleroppgavetype? = Saksbehandleroppgavetype.FØRSTEGANGSBEHANDLING
    ) {
        testRapid.sendTestMessage(
            """
            {
              "@behov": ["Godkjenning"],
              "@id": "$hendelseId",
              "fødselsnummer": "$fødselsnummer",
              "aktørId": "$aktørId",
              "organisasjonsnummer": "$organisasjonsnummer",
              "vedtaksperiodeId": "$vedtaksperiodeId",
              "periodeFom": "${LocalDate.of(2018, 1, 1)}",
              "periodeTom": "${LocalDate.of(2018, 1, 31)}",
              "warnings": $warnings,
              "periodetype": ${periodetype?.let { "\"${it.name}\"" }}
            }
        """
        )
    }

    private fun saksbehandlerLøsning() = SaksbehandlerLøsning(
        godkjent = true,
        saksbehandlerIdent = "abcd",
        godkjenttidspunkt = LocalDateTime.now(),
        oid = UUID.randomUUID(),
        epostadresse = "epost",
        årsak = null,
        begrunnelser = null,
        kommentar = null
    )

    private fun risikovurderingLøsning(hendelseId: UUID, vedtaksperiodeId: UUID, arbeidsuførhetvurdering: List<String> = emptyList()): RisikovurderingLøsning = RisikovurderingLøsning(
        hendelseId = hendelseId,
        vedtaksperiodeId = vedtaksperiodeId,
        opprettet = LocalDateTime.now(),
        samletScore = 10.0,
        begrunnelser = emptyList(),
        ufullstendig = false,
        faresignaler = emptyList(),
        arbeidsuførhetvurdering = arbeidsuførhetvurdering
    )
}

@ExperimentalContracts
fun customAssertNotNull(value: Any?) {
    contract { returns() implies (value is Any) }
    assertNotNull(value)
}

private fun hentPersoninfoLøsning() = HentPersoninfoLøsning("Test", null, "Testsen", LocalDate.now(), Kjønn.Mann)

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

private fun personinfoLøsningJson(
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
        } ],
        "HentEnhet" :"1119",
        "HentPersoninfo" : {
            "fornavn": "Person",
            "mellomnavn": "Ressurs",
            "etternavn": "Personsen",
            "fødselsdato": "2020-01-01",
            "kjønn": "Ukjent"
        }
    }
}""".trimIndent()

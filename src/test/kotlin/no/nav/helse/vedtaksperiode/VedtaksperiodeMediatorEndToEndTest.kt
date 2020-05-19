package no.nav.helse.vedtaksperiode

import com.fasterxml.jackson.databind.node.ArrayNode
import no.nav.helse.SpleisMockClient
import no.nav.helse.accessTokenClient
import no.nav.helse.mediator.kafka.SpleisbehovMediator
import no.nav.helse.mediator.kafka.meldinger.GodkjenningMessage
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.command.OppgaveDao
import no.nav.helse.modell.command.SpleisbehovDao
import no.nav.helse.modell.person.*
import no.nav.helse.modell.vedtak.NavnDto
import no.nav.helse.modell.vedtak.VedtakDao
import no.nav.helse.modell.vedtak.snapshot.SnapshotDao
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestDao
import no.nav.helse.objectMapper
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.setupDataSourceMedFlyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate
import java.util.*

class VedtaksperiodeMediatorEndToEndTest {

    private val fødselsnummer = "12345"
    private val aktørId = "23456"
    private val organisasjonsnummer = "34567"

    private val spleisMockClient =
        SpleisMockClient(fødselsnummer = fødselsnummer, aktørId = aktørId, organisasjonsnummer = organisasjonsnummer)
    private val accessTokenClient = accessTokenClient()
    private val speilSnapshotRestDao = SpeilSnapshotRestDao(
        spleisMockClient.client,
        accessTokenClient,
        "spleisClientId"
    )

    private val dataSource = setupDataSourceMedFlyway()
    private val vedtaksperiodeDao = VedtaksperiodeDao(dataSource)
    private val arbeidsgiverDao = ArbeidsgiverDao(dataSource)
    private val spleisbehovDao = SpleisbehovDao(dataSource)
    private val snapshotDao = SnapshotDao(dataSource)
    private val oppgaveDao = OppgaveDao(dataSource)
    private val vedtakDao = VedtakDao(dataSource)
    private val personDao = PersonDao(dataSource)

    private val spesialistOID: UUID = UUID.randomUUID()

    private lateinit var spleisbehovMediator: SpleisbehovMediator
    private lateinit var vedtaksperiodeId: UUID
    private lateinit var spleisbehovId: UUID
    private lateinit var rapid: TestRapid

    private lateinit var vedtaksperiodeMediator: VedtaksperiodeMediator

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
            spesialistOID = spesialistOID
        ).apply { init(rapid) }
        vedtaksperiodeMediator = VedtaksperiodeMediator(vedtaksperiodeDao, arbeidsgiverDao, snapshotDao, personDao)
    }

    @Test
    fun `bygger forventet spapspot`() {
        val navn = NavnDto("Spap", "Spappy", "Spapperson")

        GodkjenningMessage.Factory(rapid, spleisbehovMediator)

        sendGodkjenningsbehov(fødselsnummer = fødselsnummer)

        spleisbehovMediator.håndter(
            spleisbehovId,
            HentEnhetLøsning("1234"),
            HentPersoninfoLøsning(
                navn.fornavn,
                navn.mellomnavn,
                navn.etternavn,
                LocalDate.now(),
                Kjønn.Mann
            ),
            HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning())
        )
        val personDto = vedtaksperiodeMediator.byggSpeilSnapshotForFnr(fødselsnummer)
        assertDoesNotThrow {
            requireNotNull(personDto)
            requireNotNull(personDto.infotrygdutbetalinger)

            assertEquals(fødselsnummer, personDto.fødselsnummer)
            assertEquals(aktørId, personDto.aktørId)
            assertEquals(organisasjonsnummer, personDto.arbeidsgivere.first().organisasjonsnummer)
            assertEquals(navn, personDto.navn)

            val infotrygdutbetaling = (personDto.infotrygdutbetalinger as ArrayNode).first()
            assertEquals(LocalDate.of(2020, 1, 1), infotrygdutbetaling["fom"].asLocalDate())
            assertEquals(LocalDate.of(2020, 1, 31), infotrygdutbetaling["tom"].asLocalDate())
            assertEquals(1200.0, infotrygdutbetaling["dagsats"].asDouble())
            assertEquals(50, infotrygdutbetaling["grad"].asInt())
            assertEquals("ArbRef", infotrygdutbetaling["typetekst"].asText())
            assertEquals(organisasjonsnummer, infotrygdutbetaling["organisasjonsnummer"].asText())
        }

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

    private fun infotrygdutbetalingerLøsning(
        fom: LocalDate = LocalDate.of(2020, 1, 1),
        tom: LocalDate = LocalDate.of(2020, 1, 31),
        grad: Int = 50,
        dagsats: Double = 1200.0,
        typetekst: String = "ArbRef",
        orgnr: String = organisasjonsnummer
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

}

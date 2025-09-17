package no.nav.helse.spesialist.kafka.rivers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.modell.person.Person
import no.nav.helse.modell.person.vedtaksperiode.BehandlingDto
import no.nav.helse.modell.person.vedtaksperiode.TilstandDto
import no.nav.helse.modell.person.vedtaksperiode.Vedtaksperiode
import no.nav.helse.modell.person.vedtaksperiode.Vedtaksperiode.Companion.tilBehandling
import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.modell.vilkårsprøving.Avviksvurdering
import no.nav.helse.modell.vilkårsprøving.Beregningsgrunnlag
import no.nav.helse.modell.vilkårsprøving.InnrapportertInntekt
import no.nav.helse.modell.vilkårsprøving.Inntekt
import no.nav.helse.modell.vilkårsprøving.OmregnetÅrsinntekt
import no.nav.helse.modell.vilkårsprøving.Sammenligningsgrunnlag
import no.nav.helse.spesialist.domain.testfixtures.lagAktørId
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.kafka.IntegrationTestFixture
import no.nav.helse.spesialist.kafka.TestRapidHelpers.publiserteMeldingerUtenGenererteFelter
import no.nav.helse.spesialist.kafka.objectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.YearMonth
import java.util.UUID

internal class OppdaterPersondataRiverIntegrationTest {
    private val testRapid = TestRapid()
    private val integrationTestFixture = IntegrationTestFixture(testRapid)
    private val personRepository = integrationTestFixture.sessionFactory.sessionContext.personRepository
    private val vedtaksperiodeRepository = integrationTestFixture.sessionFactory.sessionContext.vedtaksperiodeRepository

    private val fødselsnummer = lagFødselsnummer()
    private val vedtaksperiodeId = UUID.randomUUID()
    private val spleisBehandlingId = UUID.randomUUID()

    @Test
    fun `oppdater persondata blir prosessert riktig`() {
        // Given:
        initPerson()

        // When:
        testRapid.sendTestMessage(oppdaterPersondataMelding())

        // Then:
        val meldinger = testRapid.publiserteMeldingerUtenGenererteFelter()
        assertEquals(2, meldinger.size)
        meldinger.forEach {
            assertEquals(fødselsnummer, it.key)
        }
        val behovMelding = meldinger.first { it.json["@event_name"].asText() == "behov" }
        val actualJsonNode = behovMelding.json

        @Language("JSON")
        val expectedJson = """
            {
              "@event_name": "behov",
              "@behov": [
                "HentInfotrygdutbetalinger"
              ],
              "HentInfotrygdutbetalinger": {
                "historikkFom": "2021-10-01",
                "historikkTom": "${LocalDate.now()}"
              },
              "fødselsnummer": "$fødselsnummer",
              "hendelseId": "bff52e44-d009-43c8-af43-a14a38b66cfb"
            }
        """.trimIndent()
        assertJsonEquals(expectedJson, (actualJsonNode as ObjectNode).apply {
            remove("@behovId")
            remove("contextId")
        })
    }

    @Test
    fun `oppdater persondata hopper over behov når det ikke finnes noen behandlinger`() {
        // Given:
        initPerson(behandlinger = emptyList())

        // When:
        testRapid.sendTestMessage(oppdaterPersondataMelding())

        // Then:
        val meldinger = testRapid.publiserteMeldingerUtenGenererteFelter()
        assertEquals(1, meldinger.size)
        meldinger.forEach {
            assertEquals(fødselsnummer, it.key)
        }
        assertEquals(null, meldinger.find { it.json["@event_name"].asText() == "behov" }?.json)
    }

    private fun assertJsonEquals(expectedJson: String, actualJsonNode: JsonNode) {
        val writer = objectMapper.writerWithDefaultPrettyPrinter()
        assertEquals(
            writer.writeValueAsString(objectMapper.readTree(expectedJson)),
            writer.writeValueAsString(actualJsonNode)
        )
    }

    private fun initPerson(
        behandlinger: List<BehandlingDto> = listOf(
            BehandlingDto(
                id = UUID.randomUUID(),
                vedtaksperiodeId = vedtaksperiodeId,
                utbetalingId = null,
                spleisBehandlingId = spleisBehandlingId,
                skjæringstidspunkt = LocalDate.parse("2024-10-01"),
                fom = LocalDate.parse("2024-10-01"),
                tom = LocalDate.parse("2024-10-31"),
                tilstand = TilstandDto.VidereBehandlingAvklares,
                tags = listOf("Behandling tag 1", "Behandling tag 2"),
                vedtakBegrunnelse = null,
                varsler = emptyList(),
                yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER
            )
        )
    ) {
        val vedtaksperioder = listOf(
            VedtaksperiodeDto(
                organisasjonsnummer = lagOrganisasjonsnummer(),
                vedtaksperiodeId = vedtaksperiodeId,
                forkastet = false,
                behandlinger = behandlinger
            )
        )
        vedtaksperiodeRepository.vedtaksperioderPerFødselsnummer[fødselsnummer] = vedtaksperioder.toMutableList()
        personRepository.leggTilPerson(
            Person(
                aktørId = lagAktørId(),
                fødselsnummer = fødselsnummer,
                vedtaksperioder = vedtaksperioder.map { vedtaksperiodeDto ->
                    Vedtaksperiode(
                        organisasjonsnummer = vedtaksperiodeDto.organisasjonsnummer,
                        vedtaksperiodeId = vedtaksperiodeDto.vedtaksperiodeId,
                        forkastet = vedtaksperiodeDto.forkastet,
                        behandlinger = vedtaksperiodeDto.behandlinger.map { it.tilBehandling() },
                    )
                },
                skjønnsfastsatteSykepengegrunnlag = emptyList(),
                avviksvurderinger = listOf(
                    Avviksvurdering(
                        unikId = UUID.randomUUID(),
                        vilkårsgrunnlagId = UUID.randomUUID(),
                        fødselsnummer = fødselsnummer,
                        skjæringstidspunkt = LocalDate.parse("2024-10-01"),
                        opprettet = LocalDateTime.now(),
                        avviksprosent = 10.0,
                        sammenligningsgrunnlag = Sammenligningsgrunnlag(
                            totalbeløp = 660000.0,
                            innrapporterteInntekter = listOf(
                                InnrapportertInntekt(
                                    arbeidsgiverreferanse = lagOrganisasjonsnummer(),
                                    inntekter = (1..12).map {
                                        Inntekt(
                                            årMåned = YearMonth.of(2023, Month.of(it)),
                                            beløp = 55000.0
                                        )
                                    }
                                )
                            )
                        ),
                        beregningsgrunnlag = Beregningsgrunnlag(
                            totalbeløp = 600000.0,
                            omregnedeÅrsinntekter = listOf(
                                OmregnetÅrsinntekt(
                                    arbeidsgiverreferanse = lagOrganisasjonsnummer(),
                                    beløp = 600000.0
                                )
                            )
                        )
                    ))
            )
        )
    }

    @Language("JSON")
    private fun oppdaterPersondataMelding() =
        """
    {
      "@event_name": "oppdater_persondata",
      "fødselsnummer": "$fødselsnummer",
      "@id": "bff52e44-d009-43c8-af43-a14a38b66cfb",
      "@opprettet": "2025-08-22T10:12:25.424748984",
      "system_read_count": 1,
      "system_participating_services": [
        {
          "id": "bff52e44-d009-43c8-af43-a14a38b66cfb",
          "time": "2025-08-22T10:12:25.424748984",
          "service": "spesialist",
          "instance": "spesialist-8944b8c68-zlhkj",
          "image": "europe-north1-docker.pkg.dev/nais-management-233d/tbd/helse-spesialist:2025.08.19-14.16-da17e2f"
        },
        {
          "id": "bff52e44-d009-43c8-af43-a14a38b66cfb",
          "time": "2025-08-22T10:12:25.433034589",
          "service": "spesialist",
          "instance": "spesialist-8944b8c68-zlhkj",
          "image": "europe-north1-docker.pkg.dev/nais-management-233d/tbd/helse-spesialist:2025.08.19-14.16-da17e2f"
        }
      ]
    }
    """.trimIndent()
}

package no.nav.helse.spesialist.kafka.rivers

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.TestRapidHelpers.publiserteMeldingerUtenGenererteFelter
import no.nav.helse.modell.person.Person
import no.nav.helse.modell.person.vedtaksperiode.BehandlingDto
import no.nav.helse.modell.person.vedtaksperiode.TilstandDto
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
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.YearMonth
import java.util.UUID

internal class AvsluttetUtenVedtakRiverIntegrationTest {
    private val testRapid = TestRapid()
    private val integrationTestFixture = IntegrationTestFixture(testRapid)
    private val personRepository = integrationTestFixture.sessionFactory.sessionContext.personRepository

    private val fødselsnummer = lagFødselsnummer()
    private val vedtaksperiodeId = UUID.randomUUID()
    private val spleisBehandlingId = UUID.randomUUID()

    @Test
    fun `avsluttet med vedtak blir prosessert riktig`() {
        // Given:
        initPerson()
        assertBehandlingTilstand("VidereBehandlingAvklares")

        // When:
        testRapid.sendTestMessage(avsluttetUtenVedtakMelding())

        // Then:
        assertEquals(0, testRapid.publiserteMeldingerUtenGenererteFelter().size)
        assertBehandlingTilstand("AvsluttetUtenVedtak")
    }

    private fun initPerson() {
        personRepository.leggTilPerson(
            Person.gjenopprett(
                aktørId = lagAktørId(),
                fødselsnummer = fødselsnummer,
                vedtaksperioder = listOf(
                    VedtaksperiodeDto(
                        organisasjonsnummer = lagOrganisasjonsnummer(),
                        vedtaksperiodeId = vedtaksperiodeId,
                        forkastet = false,
                        behandlinger = listOf(
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
                    )
                ),
                skjønnsfastsattSykepengegrunnlag = emptyList(),
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

    private fun assertBehandlingTilstand(expected: String) {
        personRepository.brukPersonHvisFinnes(fødselsnummer) {
            assertEquals(expected, vedtaksperiode(vedtaksperiodeId).finnBehandling(spleisBehandlingId).tilstand.navn())
        }
    }

    @Language("JSON")
    private fun avsluttetUtenVedtakMelding() =
        """
    {
      "@event_name": "avsluttet_uten_vedtak",
      "organisasjonsnummer": "896929119",
      "yrkesaktivitetstype": "ARBEIDSTAKER",
      "vedtaksperiodeId": "a0c7ad24-3277-4632-9e83-7e40cec12766",
      "behandlingId": "$spleisBehandlingId",
      "fom": "2024-05-01",
      "tom": "2024-05-15",
      "skjæringstidspunkt": "2024-05-01",
      "hendelser": [
        "d59986f7-24c1-4b7b-858b-3a9421eb0d04",
        "46cd1183-3c13-435e-a4a1-4321797159f7"
      ],
      "avsluttetTidspunkt": "2025-08-05T12:35:30.405991344",
      "@id": "e1a3a479-b485-48fb-86ed-8a73714c23ed",
      "@opprettet": "2025-08-05T12:35:30.406397224",
      "system_read_count": 1,
      "system_participating_services": [
        {
          "id": "e1a3a479-b485-48fb-86ed-8a73714c23ed",
          "time": "2025-08-05T12:35:30.406397224",
          "service": "helse-spleis",
          "instance": "helse-spleis-78ccf7d6c9-q67dw",
          "image": "europe-north1-docker.pkg.dev/nais-management-233d/tbd/helse-spleis-spleis:2025.08.05-10.27-ce63981"
        },
        {
          "id": "e1a3a479-b485-48fb-86ed-8a73714c23ed",
          "time": "2025-08-05T12:35:30.964091493",
          "service": "spesialist",
          "instance": "spesialist-58c64b76cd-hbl8z",
          "image": "europe-north1-docker.pkg.dev/nais-management-233d/tbd/helse-spesialist:2025.08.05-08.17-593599a"
        }
      ],
      "fødselsnummer": "$fødselsnummer",
      "@forårsaket_av": {
        "id": "46cd1183-3c13-435e-a4a1-4321797159f7",
        "opprettet": "2024-05-01T00:00",
        "event_name": "inntektsmelding"
      }
    }
""".trimIndent()
}

package no.nav.helse.mediator.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.kafka.AvsluttetMedVedtakRiver
import no.nav.helse.medRivers
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.meldinger.hendelser.AvsluttetMedVedtakMessage
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.util.UUID

internal class AvsluttetMedVedtakRiverTest {
    private val mediator = mockk<MeldingMediator>(relaxed = true)
    private val testRapid =
        TestRapid().medRivers(AvsluttetMedVedtakRiver(mediator))

    @Test
    fun `Leser inn utkast_til_vedtak-event`() {
        testRapid.sendTestMessage(utkastTilVedtak("EtterSkjønn"))
        verify(exactly = 1) { mediator.mottaMelding(any<AvsluttetMedVedtakMessage>(), any()) }
        testRapid.sendTestMessage(utkastTilVedtak("EtterHovedregel"))
        verify(exactly = 2) { mediator.mottaMelding(any<AvsluttetMedVedtakMessage>(), any()) }
        testRapid.sendTestMessage(utkastTilVedtak("IInfotrygd"))
        verify(exactly = 3) { mediator.mottaMelding(any<AvsluttetMedVedtakMessage>(), any()) }
    }

    @Language("JSON")
    private fun utkastTilVedtak(faktaType: String): String {
        val fakta =
            when (faktaType) {
                "EtterSkjønn" -> faktaEtterSkjønn
                "EtterHovedregel" -> faktaEtterHovedregel
                "IInfotrygd" -> faktaIInfotrygd
                else -> throw IllegalArgumentException()
            }

        return """
            {
              "@event_name": "avsluttet_med_vedtak",
              "organisasjonsnummer": "987654321",
              "vedtaksperiodeId": "${UUID.randomUUID()}",
              "behandlingId": "${UUID.randomUUID()}",
              "fom": "2018-01-01",
              "tom": "2018-01-31",
              "hendelser": [
                "${UUID.randomUUID()}"
              ],
              "skjæringstidspunkt": "2018-01-01",
              "sykepengegrunnlag": 700000.0,
              "sykepengegrunnlagsfakta": $fakta,
              "vedtakFattetTidspunkt": "2018-02-01T00:00:00.000",
              "utbetalingId": "${UUID.randomUUID()}",
              "@id": "${UUID.randomUUID()}",
              "@opprettet": "2018-02-01T00:00:00.000",
              "aktørId": "1234567891011",
              "fødselsnummer": "12345678910",
              "tags": ["IngenNyArbeidsgiverperiode"]
            }
            """.trimIndent()
    }

    @Language("JSON")
    private val faktaEtterSkjønn =
        """
        {
            "fastsatt": "EtterSkjønn",
            "omregnetÅrsinntekt": 800000.00,
            "innrapportertÅrsinntekt": 755555.00,
            "skjønnsfastsatt": 777000.00,
            "avviksprosent": 26.38,
            "6G": 711720,
            "tags": [
              "6GBegrenset"
            ],
            "arbeidsgivere": [
              {
                "arbeidsgiver": "987654321",
                "omregnetÅrsinntekt": 800000.00,
                "skjønnsfastsatt": 777000.00
              }
            ]
          }
        """.trimIndent()

    @Language("JSON")
    private val faktaEtterHovedregel =
        """
        {
            "fastsatt": "EtterHovedregel",
            "omregnetÅrsinntekt": 800000.00,
            "innrapportertÅrsinntekt": 755555.00,
            "avviksprosent": 26.38,
            "6G": 711720,
            "tags": [
              "6GBegrenset"
            ],
            "arbeidsgivere": [
              {
                "arbeidsgiver": "987654321",
                "omregnetÅrsinntekt": 800000.00
              }
            ]
          }
        """.trimIndent()

    @Language("JSON")
    private val faktaIInfotrygd =
        """
        {
            "fastsatt": "IInfotrygd",
            "omregnetÅrsinntekt": 800000.00
          }
        """.trimIndent()
}

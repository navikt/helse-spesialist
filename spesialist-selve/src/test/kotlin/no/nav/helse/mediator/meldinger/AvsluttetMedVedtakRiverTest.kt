package no.nav.helse.mediator.meldinger

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.db.AvviksvurderingDao
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.meldinger.hendelser.AvsluttetMedVedtakMessage
import no.nav.helse.db.AvslagDao
import no.nav.helse.modell.avviksvurdering.Avviksvurdering
import no.nav.helse.modell.avviksvurdering.BeregningsgrunnlagDto
import no.nav.helse.modell.avviksvurdering.InnrapportertInntektDto
import no.nav.helse.modell.avviksvurdering.InntektDto
import no.nav.helse.modell.avviksvurdering.OmregnetÅrsinntektDto
import no.nav.helse.modell.avviksvurdering.SammenligningsgrunnlagDto
import no.nav.helse.modell.vedtaksperiode.GenerasjonDao
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class AvsluttetMedVedtakRiverTest {
    private val mediator = mockk<MeldingMediator>(relaxed = true)
    private val avviksvurderingDao = mockk<AvviksvurderingDao>(relaxed = true)
    private val generasjonDao = mockk<GenerasjonDao>(relaxed = true)
    private val avslagDao = mockk<AvslagDao>(relaxed = true)
    private val testRapid = TestRapid()

    init {
        AvsluttetMedVedtakRiver(testRapid, mediator, avviksvurderingDao, generasjonDao, avslagDao)
    }

    @Test
    fun `Leser inn utkast_til_vedtak-event`() {
        every { avviksvurderingDao.finnAvviksvurderinger(any()) } returns listOf(avviksvurdering())
        testRapid.sendTestMessage(utkastTilVedtak("EtterSkjønn"))
        verify(exactly = 1) { mediator.håndter(any<AvsluttetMedVedtakMessage>(), any()) }
        testRapid.sendTestMessage(utkastTilVedtak("EtterHovedregel"))
        verify(exactly = 2) { mediator.håndter(any<AvsluttetMedVedtakMessage>(), any()) }
        testRapid.sendTestMessage(utkastTilVedtak("IInfotrygd"))
        verify(exactly = 3) { mediator.håndter(any<AvsluttetMedVedtakMessage>(), any()) }
    }

    private fun avviksvurdering(): Avviksvurdering = Avviksvurdering(
        unikId = UUID.randomUUID(),
        vilkårsgrunnlagId = UUID.randomUUID(),
        fødselsnummer = "12345678910",
        skjæringstidspunkt = LocalDate.of(2018,1,1),
        opprettet = LocalDateTime.now(),
        avviksprosent = 10.0,
        sammenligningsgrunnlag = SammenligningsgrunnlagDto(
            totalbeløp = 10000.0,
            innrapporterteInntekter = listOf(InnrapportertInntektDto(
                arbeidsgiverreferanse = "123456789",
                inntekter = listOf(InntektDto(
                    årMåned = YearMonth.now(),
                    beløp = 10000.0,
                ))
            ))
        ),
        beregningsgrunnlag = BeregningsgrunnlagDto(
            totalbeløp = 10000.0,
            omregnedeÅrsinntekter = listOf(OmregnetÅrsinntektDto(
                arbeidsgiverreferanse = "123456789",
                beløp = 10000.0
            ))
        )
    )

    @Language("JSON")
    private fun utkastTilVedtak(faktaType: String): String {
        val fakta = when(faktaType) {
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
          "grunnlagForSykepengegrunnlag": 700000.0,
          "grunnlagForSykepengegrunnlagPerArbeidsgiver": {
            "987654321": 700000.0
          },
          "sykepengegrunnlagsfakta": $fakta,
          "begrensning": "ER_6G_BEGRENSET",
          "inntekt": 60000.0,
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
    private val faktaEtterSkjønn = """
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
    private val faktaEtterHovedregel = """
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
    private val faktaIInfotrygd = """
        {
            "fastsatt": "IInfotrygd",
            "omregnetÅrsinntekt": 800000.00
          }
    """.trimIndent()
}


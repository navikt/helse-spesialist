package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.medRivers
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.vilkårsprøving.AvviksvurderingBehovLøsning
import no.nav.helse.modell.vilkårsprøving.Beregningsgrunnlag
import no.nav.helse.modell.vilkårsprøving.InnrapportertInntekt
import no.nav.helse.modell.vilkårsprøving.Inntekt
import no.nav.helse.modell.vilkårsprøving.OmregnetÅrsinntekt
import no.nav.helse.modell.vilkårsprøving.Sammenligningsgrunnlag
import no.nav.helse.spesialist.testhjelp.lagOrganisasjonsnummer
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

class AvviksvurderingLøsningRiverTest {
    private val mediator = mockk<MeldingMediator>(relaxed = true)
    private val testRapid = TestRapid().medRivers(AvviksvurderingLøsningRiver(mediator))

    private val hendelseId = UUID.randomUUID()
    private val contextId = UUID.randomUUID()
    private val id = UUID.randomUUID()
    private val organisasjonsnummer = lagOrganisasjonsnummer()
    private val avviksvurderingId = UUID.randomUUID()
    private val maksimaltTillattAvvik = 25.0
    private val avviksprosent = 50.0
    private val harAkseptabeltAvvik = false
    private val opprettet = LocalDateTime.now()
    private val beregningsgrunnlagTotalbeløp = 900000.0
    private val sammenligningsgrunnlagTotalbeløp = 600000.0
    private val beregningsgrunnlag = Beregningsgrunnlag(
        totalbeløp = beregningsgrunnlagTotalbeløp,
        omregnedeÅrsinntekter = listOf(OmregnetÅrsinntekt(organisasjonsnummer, beregningsgrunnlagTotalbeløp))
    )

    private val sammenligningsgrunnlag = Sammenligningsgrunnlag(
        totalbeløp = sammenligningsgrunnlagTotalbeløp,
        innrapporterteInntekter = listOf(
            InnrapportertInntekt(
                arbeidsgiverreferanse = organisasjonsnummer,
                inntekter = listOf(Inntekt(YearMonth.of(2018, 1), sammenligningsgrunnlagTotalbeløp))
            )
        )
    )

    @Test
    fun `kan lese løsning på avviksvurdering-behov med ny vurdering`() {
        testRapid.sendTestMessage(løsningMedNyVurdering())
        val forventetLøsning = AvviksvurderingBehovLøsning(
            avviksvurderingId,
            maksimaltTillattAvvik,
            avviksprosent,
            harAkseptabeltAvvik,
            opprettet,
            beregningsgrunnlag,
            sammenligningsgrunnlag
        )
        verify(exactly = 1) {
            mediator.løsning(hendelseId, contextId, id, forventetLøsning, any())
        }
    }

    @Test
    fun `kan lese løsning på avviksvurdering-behov uten ny vurdering`() {
        testRapid.sendTestMessage(løsningUtenNyVurdering())
        val forventetLøsning = AvviksvurderingBehovLøsning(
            avviksvurderingId = avviksvurderingId,
            maksimaltTillattAvvik = maksimaltTillattAvvik,
            avviksprosent = avviksprosent,
            harAkseptabeltAvvik = harAkseptabeltAvvik,
            opprettet = opprettet,
            beregningsgrunnlag = beregningsgrunnlag,
            sammenligningsgrunnlag = sammenligningsgrunnlag
        )
        verify(exactly = 1) {
            mediator.løsning(hendelseId, contextId, id, forventetLøsning, any())
        }
    }

    @Language("JSON")
    private fun løsningMedNyVurdering(): String = """
        {
          "@event_name": "behov",
          "@behovId": "b4c1ebd1-61af-46f0-8706-31f31ea61b8b",
          "@behov": [
            "Avviksvurdering"
          ],
          "@final": true,
          "fødselsnummer": "12345678910",
          "organisasjonsnummer": "$organisasjonsnummer",
          "hendelseId": "$hendelseId",
          "contextId": "$contextId",
          "vedtaksperiodeId": "4dde0ba9-5bbf-4c4d-800b-246cf19dfbc6",
          "Avviksvurdering": {
            "skjæringstidspunkt": "2018-01-01",
            "vilkårsgrunnlagId": "87b9339d-a67d-49b0-af36-c93d6f9249ae",
            "omregnedeÅrsinntekter": [
              {
                "organisasjonsnummer": "$organisasjonsnummer",
                "beløp": 900000.0
              }
            ]
          },
          "@id": "$id",
          "@opprettet": "2018-01-01T00:00:00.000",
          "@løsning": {
            "Avviksvurdering": {
              "utfall": "NyVurderingForetatt",
              "avviksvurderingId": "$avviksvurderingId",
              "avviksprosent": $avviksprosent,
              "harAkseptabeltAvvik": $harAkseptabeltAvvik,
              "maksimaltTillattAvvik": $maksimaltTillattAvvik,
              "opprettet": "$opprettet",
              "beregningsgrunnlag": {
                "totalbeløp": 900000.0,
                "omregnedeÅrsinntekter": [
                  {
                    "arbeidsgiverreferanse": "$organisasjonsnummer",
                    "beløp": 900000.0
                  }
                ]
              },
              "sammenligningsgrunnlag": {
                "totalbeløp": 600000.0,
                "innrapporterteInntekter": [
                  {
                    "arbeidsgiverreferanse": "$organisasjonsnummer",
                    "inntekter": [
                      {
                        "beløp": 600000.0,
                        "årMåned": "2018-01"
                      }
                    ]
                  }
                ]
              }
            }
          },
          "skjæringstidspunkt": "2018-01-01"
        }

    """.trimIndent()

    @Language("JSON")
    private fun løsningUtenNyVurdering(): String = """
        {
          "@event_name": "behov",
          "@behovId": "b4c1ebd1-61af-46f0-8706-31f31ea61b8b",
          "@behov": [
            "Avviksvurdering"
          ],
          "@final": true,
          "fødselsnummer": "12345678910",
          "organisasjonsnummer": "$organisasjonsnummer",
          "hendelseId": "$hendelseId",
          "contextId": "$contextId",
          "vedtaksperiodeId": "4dde0ba9-5bbf-4c4d-800b-246cf19dfbc6",
          "Avviksvurdering": {
            "skjæringstidspunkt": "2018-01-01",
            "vilkårsgrunnlagId": "87b9339d-a67d-49b0-af36-c93d6f9249ae",
            "omregnedeÅrsinntekter": [
              {
                "organisasjonsnummer": "$organisasjonsnummer",
                "beløp": 900000.0
              }
            ]
          },
          "@id": "$id",
          "@opprettet": "2018-01-01T00:00:00.000",
          "@løsning": {
            "Avviksvurdering": {
              "utfall": "TrengerIkkeNyVurdering",
              "avviksvurderingId": "$avviksvurderingId",
              "avviksprosent": $avviksprosent,
              "harAkseptabeltAvvik": $harAkseptabeltAvvik,
              "maksimaltTillattAvvik": $maksimaltTillattAvvik,
              "opprettet": "$opprettet",
              "beregningsgrunnlag": {
                "totalbeløp": 900000.0,
                "omregnedeÅrsinntekter": [
                  {
                    "arbeidsgiverreferanse": "$organisasjonsnummer",
                    "beløp": 900000.0
                  }
                ]
              },
              "sammenligningsgrunnlag": {
                "totalbeløp": 600000.0,
                "innrapporterteInntekter": [
                  {
                    "arbeidsgiverreferanse": "$organisasjonsnummer",
                    "inntekter": [
                      {
                        "beløp": 600000.0,
                        "årMåned": "2018-01"
                      }
                    ]
                  }
                ]
              }
            }
          },
          "skjæringstidspunkt": "2018-01-01"
        }
    """.trimIndent()
}

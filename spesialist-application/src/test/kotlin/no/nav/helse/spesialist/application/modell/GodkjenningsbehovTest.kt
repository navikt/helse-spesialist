package no.nav.helse.spesialist.application.modell

import no.nav.helse.modell.person.vedtaksperiode.SpleisVedtaksperiode
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import kotlin.test.assertEquals

class GodkjenningsbehovTest {
    @Test
    fun `skal kunne parse godkjenningsbehov med perioderMedSammeSkjæringstidspunkt uten yrkesaktivitet`() {
        val behov = Godkjenningsbehov.fraJson(godkjenningMedSpleisperioderUtenYrkesaktivitet)
        behov.data().spleisVedtaksperioder.first().let { periode ->
            assertNull(periode.yrkesaktivitet)
        }
    }

    @Test
    fun `skal kunne parse godkjenningsbehov med perioderMedSammeSkjæringstidspunkt med yrkesaktivitet`() {
        val behov = Godkjenningsbehov.fraJson(godkjenningMedSpleisperioderMedYrkesaktivitet)
        behov.data().spleisVedtaksperioder.first().let { periode ->
            assertEquals(
                SpleisVedtaksperiode.Yrkesaktivitet(
                    yrkesaktivitetstype = "ARBEIDSTAKER",
                    organisasjonsnummer = "987654321",
                ),
                periode.yrkesaktivitet,
            )
        }
    }

    @Test
    fun `skal kunne parse godkjenningsbehov med perioderMedSammeSkjæringstidspunkt med yrkesaktivitet uten orgnr`() {
        val behov = Godkjenningsbehov.fraJson(godkjenningMedSpleisperioderMedYrkesaktivitetMenUtenOrgnr)
        behov.data().spleisVedtaksperioder.first().let { periode ->
            assertEquals(
                SpleisVedtaksperiode.Yrkesaktivitet(
                    yrkesaktivitetstype = "SJØLVSTENDIG",
                ),
                periode.yrkesaktivitet,
            )
        }
    }
}

private val godkjenningMedSpleisperioderUtenYrkesaktivitet =
    """
{
  "@event_name" : "behov",
  "@id" : "9d9c194f-7307-4ab4-9121-3b2a1398d45a",
  "@opprettet" : "2026-07-17T10:28:43.056696",
  "@behov" : [ "Godkjenning" ],
  "aktørId" : "1000044929781",
  "fødselsnummer" : "15870062211",
  "organisasjonsnummer" : "989393204",
  "yrkesaktivitetstype" : "ARBEIDSTAKER",
  "vedtaksperiodeId" : "b504667d-6183-456c-b9fd-bc2d8fe9de06",
  "utbetalingId" : "8be1d73f-1fd1-4893-934e-a82cb6d57a5e",
  "Godkjenning" : {
    "periodeFom" : "2018-01-01",
    "periodeTom" : "2018-01-31",
    "skjæringstidspunkt" : "2018-01-01",
    "periodetype" : "FØRSTEGANGSBEHANDLING",
    "førstegangsbehandling" : true,
    "utbetalingtype" : "UTBETALING",
    "inntektskilde" : "EN_ARBEIDSGIVER",
    "orgnummereMedRelevanteArbeidsforhold" : [ ],
    "kanAvvises" : true,
    "vilkårsgrunnlagId" : "7c406eca-7c8b-4a2c-9765-6d1750be983c",
    "forsikringsvurderingId" : null,
    "behandlingId" : "bd7a7b3b-f0eb-4df9-a898-18550a339eb4",
    "tags" : [ "Innvilget" ],
    "perioderMedSammeSkjæringstidspunkt" : [ {
      "fom" : "2018-01-01",
      "tom" : "2018-01-31",
      "vedtaksperiodeId" : "b504667d-6183-456c-b9fd-bc2d8fe9de06",
      "behandlingId" : "bd7a7b3b-f0eb-4df9-a898-18550a339eb4"
    } ],
    "sykepengegrunnlagsfakta" : {
      "fastsatt" : "EtterHovedregel",
      "sykepengegrunnlag" : 123456.7,
      "6G" : 666666.66,
      "arbeidsgivere" : [ {
        "arbeidsgiver" : "989393204",
        "omregnetÅrsinntekt" : 123456.7,
        "inntektskilde" : "Arbeidsgiver"
      } ]
    },
    "foreløpigBeregnetSluttPåSykepenger" : "2018-12-01",
    "relevanteSøknader" : [ "bff705d5-9e16-4f1f-8c30-93e90514d013" ],
    "arbeidssituasjon" : null,
    "utbetalingsdager" : [ {
      "utbetaling" : "medFriStruktur"
    } ]
  },
  "system_read_count" : 0,
  "system_participating_services" : [ {
    "id" : "9d9c194f-7307-4ab4-9121-3b2a1398d45a",
    "time" : "2026-07-17T10:28:43.102315"
  } ]
}    
    """.trimIndent()

private val godkjenningMedSpleisperioderMedYrkesaktivitet =
    """
{
  "@id" : "019f6f0a-c085-708b-820e-80c822e68d1b",
  "@behov" : [ "Godkjenning" ],
  "@behovId" : "bf255373-15c8-4093-b054-452c90d61f31",
  "@opprettet" : "2026-07-17T09:46:43.461492",
  "@event_name" : "behov",
  "Godkjenning" : {
    "tags" : [ "Førstegangsbehandling", "Arbeidsgiverutbetaling", "ArbeidsgiverØnskerRefusjon", "Innvilget", "EnArbeidsgiver" ],
    "kanAvvises" : true,
    "periodeFom" : "2018-01-03",
    "periodeTom" : "2018-01-26",
    "periodetype" : "FØRSTEGANGSBEHANDLING",
    "behandlingId" : "dd92a8fa-d517-489a-b9d4-1c183ad580d3",
    "inntektskilde" : "EN_ARBEIDSGIVER",
    "utbetalingtype" : "UTBETALING",
    "arbeidssituasjon" : "ARBEIDSTAKER",
    "utbetalingsdager" : [ {
      "utbetaling" : "medFriStruktur"
    } ],
    "forbrukteSykedager" : 6,
    "relevanteSøknader" : [ "df4d39f1-b338-45dd-bebe-8904a7be373a" ],
    "vilkårsgrunnlagId" : "441f6e0d-f512-4a28-98a2-f1bc62138bfb",
    "skjæringstidspunkt" : "2018-01-03",
    "gjenståendeSykedager" : 242,
    "førstegangsbehandling" : true,
    "sykepengegrunnlagsfakta" : {
      "6G" : 561804.0,
      "fastsatt" : "EtterHovedregel",
      "selvstendig" : null,
      "arbeidsgivere" : [ {
        "arbeidsgiver" : "987654321",
        "inntektskilde" : "Arbeidsgiver",
        "omregnetÅrsinntekt" : 372000.0
      } ],
      "sykepengegrunnlag" : 372000.0
    },
    "perioderMedSammeSkjæringstidspunkt" : [ {
      "fom" : "2018-01-03",
      "tom" : "2018-01-26",
      "behandlingId" : "dd92a8fa-d517-489a-b9d4-1c183ad580d3",
      "yrkesaktivitet" : {
        "organisasjonsnummer" : "987654321",
        "yrkesaktivitetstype" : "ARBEIDSTAKER"
      },
      "vedtaksperiodeId" : "8bacd138-3061-46d0-a37c-1171695ec9ae"
    } ],
    "foreløpigBeregnetSluttPåSykepenger" : "2019-01-01",
    "orgnummereMedRelevanteArbeidsforhold" : [ "987654321" ]
  },
  "behandlingId" : "dd92a8fa-d517-489a-b9d4-1c183ad580d3",
  "utbetalingId" : "3f8d8a4a-ba48-48d3-ab7b-051781bb5f25",
  "@opprettetUTC" : "2026-07-17T07:46:43.461492Z",
  "fødselsnummer" : "12029240045",
  "@forårsaket_av" : {
    "id" : "e0b95cb9-f101-400c-90af-7d0c7ca088c8",
    "behov" : [ "Simulering" ],
    "opprettet" : "2026-07-17T09:46:43.428938",
    "event_name" : "behov"
  },
  "vedtaksperiodeId" : "8bacd138-3061-46d0-a37c-1171695ec9ae",
  "system_read_count" : 0,
  "meldingsreferanseId" : "e0b95cb9-f101-400c-90af-7d0c7ca088c8",
  "organisasjonsnummer" : "987654321",
  "yrkesaktivitetstype" : "ARBEIDSTAKER",
  "system_participating_services" : [ {
    "id" : "019f6f0a-c085-708b-820e-80c822e68d1b",
    "time" : "2026-07-17T09:46:43.461886"
  } ],
  "@sendt" : "2026-07-17T07:46:43.488530Z"
}    
    """.trimIndent()

private val godkjenningMedSpleisperioderMedYrkesaktivitetMenUtenOrgnr =
    """
{
  "@id" : "019f6f0a-c085-708b-820e-80c822e68d1b",
  "@behov" : [ "Godkjenning" ],
  "@behovId" : "bf255373-15c8-4093-b054-452c90d61f31",
  "@opprettet" : "2026-07-17T09:46:43.461492",
  "@event_name" : "behov",
  "Godkjenning" : {
    "tags" : [ "Førstegangsbehandling", "Arbeidsgiverutbetaling", "ArbeidsgiverØnskerRefusjon", "Innvilget", "EnArbeidsgiver" ],
    "kanAvvises" : true,
    "periodeFom" : "2018-01-03",
    "periodeTom" : "2018-01-26",
    "periodetype" : "FØRSTEGANGSBEHANDLING",
    "behandlingId" : "dd92a8fa-d517-489a-b9d4-1c183ad580d3",
    "inntektskilde" : "EN_ARBEIDSGIVER",
    "utbetalingtype" : "UTBETALING",
    "arbeidssituasjon" : "ARBEIDSTAKER",
    "utbetalingsdager" : [ {
      "utbetaling" : "medFriStruktur"
    } ],
    "forbrukteSykedager" : 6,
    "relevanteSøknader" : [ "df4d39f1-b338-45dd-bebe-8904a7be373a" ],
    "vilkårsgrunnlagId" : "441f6e0d-f512-4a28-98a2-f1bc62138bfb",
    "skjæringstidspunkt" : "2018-01-03",
    "gjenståendeSykedager" : 242,
    "førstegangsbehandling" : true,
    "sykepengegrunnlagsfakta" : {
      "6G" : 561804.0,
      "fastsatt" : "EtterHovedregel",
      "selvstendig" : null,
      "arbeidsgivere" : [ {
        "arbeidsgiver" : "987654321",
        "inntektskilde" : "Arbeidsgiver",
        "omregnetÅrsinntekt" : 372000.0
      } ],
      "sykepengegrunnlag" : 372000.0
    },
    "perioderMedSammeSkjæringstidspunkt" : [ {
      "fom" : "2018-01-03",
      "tom" : "2018-01-26",
      "behandlingId" : "dd92a8fa-d517-489a-b9d4-1c183ad580d3",
      "yrkesaktivitet" : {
        "yrkesaktivitetstype" : "SJØLVSTENDIG"
      },
      "vedtaksperiodeId" : "8bacd138-3061-46d0-a37c-1171695ec9ae"
    } ],
    "foreløpigBeregnetSluttPåSykepenger" : "2019-01-01",
    "orgnummereMedRelevanteArbeidsforhold" : [ "987654321" ]
  },
  "behandlingId" : "dd92a8fa-d517-489a-b9d4-1c183ad580d3",
  "utbetalingId" : "3f8d8a4a-ba48-48d3-ab7b-051781bb5f25",
  "@opprettetUTC" : "2026-07-17T07:46:43.461492Z",
  "fødselsnummer" : "12029240045",
  "@forårsaket_av" : {
    "id" : "e0b95cb9-f101-400c-90af-7d0c7ca088c8",
    "behov" : [ "Simulering" ],
    "opprettet" : "2026-07-17T09:46:43.428938",
    "event_name" : "behov"
  },
  "vedtaksperiodeId" : "8bacd138-3061-46d0-a37c-1171695ec9ae",
  "system_read_count" : 0,
  "meldingsreferanseId" : "e0b95cb9-f101-400c-90af-7d0c7ca088c8",
  "organisasjonsnummer" : "987654321",
  "yrkesaktivitetstype" : "ARBEIDSTAKER",
  "system_participating_services" : [ {
    "id" : "019f6f0a-c085-708b-820e-80c822e68d1b",
    "time" : "2026-07-17T09:46:43.461886"
  } ],
  "@sendt" : "2026-07-17T07:46:43.488530Z"
}    
    """.trimIndent()

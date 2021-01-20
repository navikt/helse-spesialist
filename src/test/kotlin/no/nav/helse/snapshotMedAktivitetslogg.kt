package no.nav.helse

import org.intellij.lang.annotations.Language
import java.util.*

@Language("json")
fun snapshotMedWarning(vedtaksperiodeId: UUID) = """
            {
  "aktørId": "aktørId",
  "fødselsnummer": "fnr",
  "arbeidsgivere": [
    {
      "organisasjonsnummer": "orgnr",
      "id": "71ebda8a-4f65-7754-b3ca-bf243b85315d",
      "vedtaksperioder": [
        {
          "id": "${vedtaksperiodeId}",
          "aktivitetslogg": [
            {
              "vedtaksperiodeId": "${vedtaksperiodeId}",
              "alvorlighetsgrad": "W",
              "melding": "Brukeren har flere inntekter de siste tre måneder.",
              "tidsstempel": "2020-07-25 23:51:26.031"
            },
            {
              "vedtaksperiodeId": "${vedtaksperiodeId}",
              "alvorlighetsgrad": "I",
              "melding": "Infomelding.",
              "tidsstempel": "2020-07-25 23:51:26.032"
            }
          ]
        },
        {
          "id": "81ebda8a-4f65-7754-b3ca-bf243b85513c",
          "gruppeId": "gruppeId",
          "fom": "2020-03-24",
          "tom": "2020-03-30",
          "tilstand": "KunFerie",
          "fullstendig": false
        },
        {
          "id": "32f43569-26d2-4a45-a8da-fe6e1d9643e3",
          "aktivitetslogg": [
            {
              "vedtaksperiodeId": "32f43569-26d2-4a45-a8da-fe6e1d9643e3",
              "alvorlighetsgrad": "W",
              "melding": "Brukeren har flere inntekter de siste tre måneder.",
              "tidsstempel": "2020-06-12 13:21:24.072"
            }
          ],
          "forlengelseFraInfotrygd": "NEI",
          "periodetype": "FØRSTEGANGSBEHANDLING"
        }
      ]
    }
  ]
}
"""

@Language("json")
fun snapshotUtenWarnings(vedtaksperiodeId: UUID) = """
            {
  "aktørId": "aktørId",
  "fødselsnummer": "fnr",
  "arbeidsgivere": [
    {
      "organisasjonsnummer": "orgnr",
      "id": "71ebda8a-4f65-7754-b3ca-bf243b85315d",
      "vedtaksperioder": [
        {
          "id": "${vedtaksperiodeId}",
          "aktivitetslogg": [
            {
              "vedtaksperiodeId": "${vedtaksperiodeId}",
              "alvorlighetsgrad": "I",
              "melding": "Infomelding.",
              "tidsstempel": "2020-07-25 23:51:26.032"
            }
          ]
        },
        {
          "id": "81ebda8a-4f65-7754-b3ca-bf243b85513c",
          "gruppeId": "gruppeId",
          "fom": "2020-03-24",
          "tom": "2020-03-30",
          "tilstand": "KunFerie",
          "fullstendig": false
        },
        {
          "id": "32f43569-26d2-4a45-a8da-fe6e1d9643e3",
          "aktivitetslogg": [
            {
              "vedtaksperiodeId": "32f43569-26d2-4a45-a8da-fe6e1d9643e3",
              "alvorlighetsgrad": "W",
              "melding": "Brukeren har flere inntekter de siste tre måneder.",
              "tidsstempel": "2020-06-12 13:21:24.072"
            }
          ],
          "forlengelseFraInfotrygd": "NEI",
          "periodetype": "FØRSTEGANGSBEHANDLING"
        }
      ]
    }
  ]
}
"""

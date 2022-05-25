package no.nav.helse.serde

import DatabaseIntegrationTest
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.util.*
import org.junit.jupiter.api.Assertions.assertEquals

internal class FjernCarriageReturnsFraHendelseJsonTest : DatabaseIntegrationTest() {
    @Test
    fun `Endrer ingenting ved fagsystemIDer uten carriage returns`() {
        opprettHendelse(utbetaling)
        kjørMigrering()
        val actual = hentHendelse(HENDELSE_ID)
        assertJsonEquals(utbetalingMedAltExpected, actual)
    }

    @Test
    fun `fagsystemID med carriage returns i alle tre tilfeller`() {
        opprettHendelse(utbetalingMedAlt)
        kjørMigrering()
        val actual = hentHendelse(HENDELSE_ID)
        assertJsonEquals(utbetalingMedAltExpected, actual)
    }

    @Test
    fun `fagsystemID med carriage returns i alle tre tilfeller og flere linjer med CR`() {
        opprettHendelse(utbetalingMedAltFlereLinjerMedCarriageReturn)
        kjørMigrering()
        val actual = hentHendelse(HENDELSE_ID)
        assertJsonEquals(utbetalingMedAltFlereLinjerMedCarriageReturnExpected, actual)
    }

    @Test
    fun `fagsystemID med carriage returns i alle tre tilfeller og flere linjer hvor en har CR`() {
        opprettHendelse(utbetalingMedAltFlereLinjerMedCarriageReturnPåEnLinje)
        kjørMigrering()
        val actual = hentHendelse(HENDELSE_ID)
        assertJsonEquals(utbetalingMedAltFlereLinjerMedCarriageReturnPåEnLinjeExpected, actual)
    }

    @Test
    fun `arbeidsgiveroppdrag-FagsystemID er null`() {
        opprettHendelse(utbetalingArbeidsgiverOppdragFagsystemIdLikNull)
        kjørMigrering()
        val actual = hentHendelse(HENDELSE_ID)
        assertJsonEquals(utbetalingArbeidsgiverOppdragFagsystemIdLikNullExpected, actual)
    }

    @Test
    fun `personoppdrag-FagsystemID er null`() {
        opprettHendelse(utbetalingPersonOppdragFagsystemIdLikNull)
        kjørMigrering()
        val actual = hentHendelse(HENDELSE_ID)
        assertJsonEquals(utbetalingPersonOppdragFagsystemIdLikNullExpected, actual)
    }

    @Test
    fun `linjer-FagsystemIDRef er null - én linje`() {
        opprettHendelse(utbetalingRefFagsystemIdErNullEnLinje)
        kjørMigrering()
        val actual = hentHendelse(HENDELSE_ID)
        assertJsonEquals(utbetalingRefFagsystemIdErNullEnLinjeExpected, actual)
    }

    @Test
    fun `linjer-FagsystemIDRef er null - flere linjer`() {
        opprettHendelse(utbetalingRefFagsystemIdErNullFlereLinjer)
        kjørMigrering()
        val actual = hentHendelse(HENDELSE_ID)
        assertJsonEquals(utbetalingRefFagsystemIdErNullFlereLinjerExpected, actual)
    }

    @Test
    fun `ignorerer hendelser før timestamp`() {
        val hendelseIdNy = UUID.randomUUID()
        val hendelseIdGammel = UUID.randomUUID()

        opprettHendelse(utbetalingMedAlt, hendelseIdNy)
        opprettHendelse(utbetalingMedAltGammel, hendelseIdGammel)

        kjørMigrering()
        val actualNy = hentHendelse(hendelseIdNy)
        val actualGammel = hentHendelse(hendelseIdGammel)
        assertJsonEquals(utbetalingMedAltExpected, actualNy)
        assertJsonEquals(utbetalingMedAltGammel, actualGammel)
    }

    @Test
    fun `ignorerer hendelser med annen type`() {
        val hendelseId = UUID.randomUUID()
        val hendelseIdFeilType = UUID.randomUUID()

        opprettHendelse(utbetalingMedAlt, hendelseId)
        opprettHendelse(utbetalingMedAlt, hendelseIdFeilType, "GODKJENNING")

        kjørMigrering()
        val actual = hentHendelse(hendelseId)
        val actualFeilType = hentHendelse(hendelseIdFeilType)
        assertJsonEquals(utbetalingMedAltExpected, actual)
        assertJsonEquals(utbetalingMedAlt, actualFeilType)
    }

    private fun opprettHendelse(data: String, hendelseId: UUID = HENDELSE_ID, type: String = "UTBETALING_ENDRET") =
        testhendelse(hendelseId = hendelseId, type = type, json = data)

    private fun assertJsonEquals(expected: String, actual: String?) =
        assertEquals(objectMapper.readTree(expected), objectMapper.readTree(actual))

    private fun kjørMigrering() {
        @Language("PostgreSQL")
        val arbeidsgiverOppdragStatement = """
            UPDATE hendelse
            SET data = jsonb_set(data::jsonb, '{arbeidsgiverOppdrag,fagsystemId}', to_jsonb(regexp_replace(data->'arbeidsgiverOppdrag'->>'fagsystemId',E'[\\r\\n]+', '', 'g')), false)
            WHERE type = 'UTBETALING_ENDRET' and data -> 'arbeidsgiverOppdrag' ->> 'fagsystemId' LIKE E'%\r\n' and (data->>'@opprettet')::timestamp > '2021-08-05';
        """

        @Language("PostgreSQL")
        val personOppdragStatement = """
            UPDATE hendelse
            SET data = jsonb_set(data::jsonb, '{personOppdrag,fagsystemId}', to_jsonb(regexp_replace(data->'personOppdrag'->>'fagsystemId',E'[\\r\\n]+', '', 'g')), false)
            WHERE type = 'UTBETALING_ENDRET' and data -> 'personOppdrag' ->> 'fagsystemId' LIKE E'%\r\n' and (data->>'@opprettet')::timestamp > '2021-08-05';
        """

        @Language("PostgreSQL")
        val linjerStatement = """
            with
                linjer as
                    (select id, jsonb_array_elements((data->'arbeidsgiverOppdrag'->'linjer')::jsonb) as original_linje
                    from hendelse
                    where (data->>'@opprettet')::timestamp > '2021-08-05' AND type = 'UTBETALING_ENDRET'),
                modifiedLinjer as
                    (select
                         id,
                         CASE
                             WHEN original_linje->>'refFagsystemId' IS NOT NULL THEN
                                 jsonb_set(original_linje, '{refFagsystemId}', to_jsonb(regexp_replace(original_linje->>'refFagsystemId',E'[\\r\\n]+', '', 'g')), false)
                             ELSE
                                 original_linje
                             END as modifisert_linje
                     from linjer)
            update hendelse
            set data = jsonb_set(data::jsonb, '{arbeidsgiverOppdrag,linjer}', (select jsonb_agg(modifisert_linje) from modifiedLinjer WHERE modifiedLinjer.id = hendelse.id))
            where type = 'UTBETALING_ENDRET' and hendelse.id in (select id from linjer where original_linje ->>'refFagsystemId' LIKE E'%\r\n');
        """

        sessionOf(dataSource).use {
            it.run(queryOf(arbeidsgiverOppdragStatement).asExecute)
            it.run(queryOf(personOppdragStatement).asExecute)
            it.run(queryOf(linjerStatement).asExecute)
        }
    }

    @Language("json")
    val utbetaling = """
    {
      "arbeidsgiverOppdrag": {
        "linjer": [
          {
            "fom": "2021-06-17",
            "tom": "2021-06-30",
            "satstype": "DAG",
            "sats": 669,
            "dagsats": 669,
            "lønn": 669,
            "grad": 100.0,
            "stønadsdager": 10,
            "totalbeløp": 6690,
            "endringskode": "NY",
            "delytelseId": 1,
            "refDelytelseId": null,
            "refFagsystemId": "QUS2RY3IFNFITMWGE6B2ICYZF4",
            "statuskode": null,
            "datoStatusFom": null,
            "klassekode": "SPREFAG-IOP"
          }
        ],
        "fagsystemId": "QUS2RY3IFNFITMWGE6B2ICYZF4"
      },
      "personOppdrag": {
        "linjer": [],
        "fagsystemId": "Y46RB4ZG6VCS5NW3IGPFLZ65QY"
      },
      "@opprettet": "2021-08-20T14:40:12.295678239"
    }
    """.trimIndent()

    @Language("json")
    val utbetalingExpected = """
    {
      "arbeidsgiverOppdrag": {
        "linjer": [
          {
            "fom": "2021-06-17",
            "tom": "2021-06-30",
            "satstype": "DAG",
            "sats": 669,
            "dagsats": 669,
            "lønn": 669,
            "grad": 100.0,
            "stønadsdager": 10,
            "totalbeløp": 6690,
            "endringskode": "NY",
            "delytelseId": 1,
            "refDelytelseId": null,
            "refFagsystemId": "QUS2RY3IFNFITMWGE6B2ICYZF4",
            "statuskode": null,
            "datoStatusFom": null,
            "klassekode": "SPREFAG-IOP"
          }
        ],
        "fagsystemId": "QUS2RY3IFNFITMWGE6B2ICYZF4"
      },
      "personOppdrag": {
        "linjer": [],
        "fagsystemId": "Y46RB4ZG6VCS5NW3IGPFLZ65QY"
      },
      "@opprettet": "2021-08-20T14:40:12.295678239"
    }
    """.trimIndent()

    @Language("json")
    val utbetalingMedAlt = """
    {
      "arbeidsgiverOppdrag": {
        "linjer": [
          {
            "fom": "2021-06-17",
            "tom": "2021-06-30",
            "satstype": "DAG",
            "sats": 669,
            "dagsats": 669,
            "lønn": 669,
            "grad": 100.0,
            "stønadsdager": 10,
            "totalbeløp": 6690,
            "endringskode": "NY",
            "delytelseId": 1,
            "refDelytelseId": null,
            "refFagsystemId": "QUS2RY3IFNFITMWGE6B2ICYZF4\r\n",
            "statuskode": null,
            "datoStatusFom": null,
            "klassekode": "SPREFAG-IOP"
          }
        ],
        "fagsystemId": "QUS2RY3IFNFITMWGE6B2ICYZF4\r\n"
      },
      "personOppdrag": {
        "linjer": [],
        "fagsystemId": "Y46RB4ZG6VCS5NW3IGPFLZ65QY\r\n"
      },
      "@opprettet": "2021-08-20T14:40:12.295678239"
    }
    """.trimIndent()

    @Language("json")
    val utbetalingMedAltGammel = """
    {
      "arbeidsgiverOppdrag": {
        "linjer": [
          {
            "fom": "2021-06-17",
            "tom": "2021-06-30",
            "satstype": "DAG",
            "sats": 669,
            "dagsats": 669,
            "lønn": 669,
            "grad": 100.0,
            "stønadsdager": 10,
            "totalbeløp": 6690,
            "endringskode": "NY",
            "delytelseId": 1,
            "refDelytelseId": null,
            "refFagsystemId": "QUS2RY3IFNFITMWGE6B2ICYZF4\r\n",
            "statuskode": null,
            "datoStatusFom": null,
            "klassekode": "SPREFAG-IOP"
          }
        ],
        "fagsystemId": "QUS2RY3IFNFITMWGE6B2ICYZF4\r\n"
      },
      "personOppdrag": {
        "linjer": [],
        "fagsystemId": "Y46RB4ZG6VCS5NW3IGPFLZ65QY\r\n"
      },
      "@opprettet": "2021-07-01T14:40:12.295678239"
    }
    """.trimIndent()

    @Language("json")
    val utbetalingMedAltExpected = """
    {
      "arbeidsgiverOppdrag": {
        "linjer": [
          {
            "fom": "2021-06-17",
            "tom": "2021-06-30",
            "satstype": "DAG",
            "sats": 669,
            "dagsats": 669,
            "lønn": 669,
            "grad": 100.0,
            "stønadsdager": 10,
            "totalbeløp": 6690,
            "endringskode": "NY",
            "delytelseId": 1,
            "refDelytelseId": null,
            "refFagsystemId": "QUS2RY3IFNFITMWGE6B2ICYZF4",
            "statuskode": null,
            "datoStatusFom": null,
            "klassekode": "SPREFAG-IOP"
          }
        ],
        "fagsystemId": "QUS2RY3IFNFITMWGE6B2ICYZF4"
      },
      "personOppdrag": {
        "linjer": [],
        "fagsystemId": "Y46RB4ZG6VCS5NW3IGPFLZ65QY"
      },
      "@opprettet": "2021-08-20T14:40:12.295678239"
    }
    """.trimIndent()

    @Language("json")
    val utbetalingMedAltFlereLinjerMedCarriageReturn = """
    {
      "arbeidsgiverOppdrag": {
        "linjer": [
          {
            "fom": "2021-06-17",
            "tom": "2021-06-30",
            "satstype": "DAG",
            "sats": 669,
            "dagsats": 669,
            "lønn": 669,
            "grad": 100.0,
            "stønadsdager": 10,
            "totalbeløp": 6690,
            "endringskode": "NY",
            "delytelseId": 1,
            "refDelytelseId": null,
            "refFagsystemId": "QUS2RY3IFNFITMWGE6B2ICYZF4\r\n",
            "statuskode": null,
            "datoStatusFom": null,
            "klassekode": "SPREFAG-IOP"
          },
          {
            "fom": "2021-06-17",
            "tom": "2021-06-30",
            "satstype": "DAG",
            "sats": 669,
            "dagsats": 669,
            "lønn": 669,
            "grad": 100.0,
            "stønadsdager": 10,
            "totalbeløp": 6690,
            "endringskode": "NY",
            "delytelseId": 1,
            "refDelytelseId": null,
            "refFagsystemId": "QUS2RY3IFNFITMWGE6B2ICYZF4\r\n",
            "statuskode": null,
            "datoStatusFom": null,
            "klassekode": "SPREFAG-IOP"
          }
        ],
        "fagsystemId": "QUS2RY3IFNFITMWGE6B2ICYZF4\r\n"
      },
      "personOppdrag": {
        "linjer": [],
        "fagsystemId": "Y46RB4ZG6VCS5NW3IGPFLZ65QY\r\n"
      },
      "@opprettet": "2021-08-20T14:40:12.295678239"
    }
    """.trimIndent()

    @Language("json")
    val utbetalingMedAltFlereLinjerMedCarriageReturnExpected = """
    {
      "arbeidsgiverOppdrag": {
        "linjer": [
          {
            "fom": "2021-06-17",
            "tom": "2021-06-30",
            "satstype": "DAG",
            "sats": 669,
            "dagsats": 669,
            "lønn": 669,
            "grad": 100.0,
            "stønadsdager": 10,
            "totalbeløp": 6690,
            "endringskode": "NY",
            "delytelseId": 1,
            "refDelytelseId": null,
            "refFagsystemId": "QUS2RY3IFNFITMWGE6B2ICYZF4",
            "statuskode": null,
            "datoStatusFom": null,
            "klassekode": "SPREFAG-IOP"
          },
          {
            "fom": "2021-06-17",
            "tom": "2021-06-30",
            "satstype": "DAG",
            "sats": 669,
            "dagsats": 669,
            "lønn": 669,
            "grad": 100.0,
            "stønadsdager": 10,
            "totalbeløp": 6690,
            "endringskode": "NY",
            "delytelseId": 1,
            "refDelytelseId": null,
            "refFagsystemId": "QUS2RY3IFNFITMWGE6B2ICYZF4",
            "statuskode": null,
            "datoStatusFom": null,
            "klassekode": "SPREFAG-IOP"
          }
        ],
        "fagsystemId": "QUS2RY3IFNFITMWGE6B2ICYZF4"
      },
      "personOppdrag": {
        "linjer": [],
        "fagsystemId": "Y46RB4ZG6VCS5NW3IGPFLZ65QY"
      },
      "@opprettet": "2021-08-20T14:40:12.295678239"
    }
    """.trimIndent()

    @Language("json")
    val utbetalingMedAltFlereLinjerMedCarriageReturnPåEnLinje = """
    {
      "arbeidsgiverOppdrag": {
        "linjer": [
          {
            "fom": "2021-06-17",
            "tom": "2021-06-30",
            "satstype": "DAG",
            "sats": 669,
            "dagsats": 669,
            "lønn": 669,
            "grad": 100.0,
            "stønadsdager": 10,
            "totalbeløp": 6690,
            "endringskode": "NY",
            "delytelseId": 1,
            "refDelytelseId": null,
            "refFagsystemId": "QUS2RY3IFNFITMWGE6B2ICYZF4",
            "statuskode": null,
            "datoStatusFom": null,
            "klassekode": "SPREFAG-IOP"
          },
          {
            "fom": "2021-06-17",
            "tom": "2021-06-30",
            "satstype": "DAG",
            "sats": 669,
            "dagsats": 669,
            "lønn": 669,
            "grad": 100.0,
            "stønadsdager": 10,
            "totalbeløp": 6690,
            "endringskode": "NY",
            "delytelseId": 1,
            "refDelytelseId": null,
            "refFagsystemId": "QUS2RY3IFNFITMWGE6B2ICYZF4\r\n",
            "statuskode": null,
            "datoStatusFom": null,
            "klassekode": "SPREFAG-IOP"
          }
        ],
        "fagsystemId": "QUS2RY3IFNFITMWGE6B2ICYZF4\r\n"
      },
      "personOppdrag": {
        "linjer": [],
        "fagsystemId": "Y46RB4ZG6VCS5NW3IGPFLZ65QY\r\n"
      },
      "@opprettet": "2021-08-20T14:40:12.295678239"
    }
    """.trimIndent()

    @Language("json")
    val utbetalingMedAltFlereLinjerMedCarriageReturnPåEnLinjeExpected = """
    {
      "arbeidsgiverOppdrag": {
        "linjer": [
          {
            "fom": "2021-06-17",
            "tom": "2021-06-30",
            "satstype": "DAG",
            "sats": 669,
            "dagsats": 669,
            "lønn": 669,
            "grad": 100.0,
            "stønadsdager": 10,
            "totalbeløp": 6690,
            "endringskode": "NY",
            "delytelseId": 1,
            "refDelytelseId": null,
            "refFagsystemId": "QUS2RY3IFNFITMWGE6B2ICYZF4",
            "statuskode": null,
            "datoStatusFom": null,
            "klassekode": "SPREFAG-IOP"
          },
          {
            "fom": "2021-06-17",
            "tom": "2021-06-30",
            "satstype": "DAG",
            "sats": 669,
            "dagsats": 669,
            "lønn": 669,
            "grad": 100.0,
            "stønadsdager": 10,
            "totalbeløp": 6690,
            "endringskode": "NY",
            "delytelseId": 1,
            "refDelytelseId": null,
            "refFagsystemId": "QUS2RY3IFNFITMWGE6B2ICYZF4",
            "statuskode": null,
            "datoStatusFom": null,
            "klassekode": "SPREFAG-IOP"
          }
        ],
        "fagsystemId": "QUS2RY3IFNFITMWGE6B2ICYZF4"
      },
      "personOppdrag": {
        "linjer": [],
        "fagsystemId": "Y46RB4ZG6VCS5NW3IGPFLZ65QY"
      },
      "@opprettet": "2021-08-20T14:40:12.295678239"
    }
    """.trimIndent()

    @Language("json")
    val utbetalingArbeidsgiverOppdragFagsystemIdLikNull = """
    {
      "arbeidsgiverOppdrag": {
        "linjer": [
          {
            "fom": "2021-06-17",
            "tom": "2021-06-30",
            "satstype": "DAG",
            "sats": 669,
            "dagsats": 669,
            "lønn": 669,
            "grad": 100.0,
            "stønadsdager": 10,
            "totalbeløp": 6690,
            "endringskode": "NY",
            "delytelseId": 1,
            "refDelytelseId": null,
            "refFagsystemId": "QUS2RY3IFNFITMWGE6B2ICYZF4\r\n",
            "statuskode": null,
            "datoStatusFom": null,
            "klassekode": "SPREFAG-IOP"
          }
        ],
        "fagsystemId": null
      },
      "personOppdrag": {
        "linjer": [],
        "fagsystemId": "Y46RB4ZG6VCS5NW3IGPFLZ65QY\r\n"
      },
      "@opprettet": "2021-08-20T14:40:12.295678239"
    }
    """.trimIndent()

    @Language("json")
    val utbetalingArbeidsgiverOppdragFagsystemIdLikNullExpected = """
    {
      "arbeidsgiverOppdrag": {
        "linjer": [
          {
            "fom": "2021-06-17",
            "tom": "2021-06-30",
            "satstype": "DAG",
            "sats": 669,
            "dagsats": 669,
            "lønn": 669,
            "grad": 100.0,
            "stønadsdager": 10,
            "totalbeløp": 6690,
            "endringskode": "NY",
            "delytelseId": 1,
            "refDelytelseId": null,
            "refFagsystemId": "QUS2RY3IFNFITMWGE6B2ICYZF4",
            "statuskode": null,
            "datoStatusFom": null,
            "klassekode": "SPREFAG-IOP"
          }
        ],
        "fagsystemId": null
      },
      "personOppdrag": {
        "linjer": [],
        "fagsystemId": "Y46RB4ZG6VCS5NW3IGPFLZ65QY"
      },
      "@opprettet": "2021-08-20T14:40:12.295678239"
    }
    """.trimIndent()

    @Language("json")
    val utbetalingPersonOppdragFagsystemIdLikNull = """
    {
      "arbeidsgiverOppdrag": {
        "linjer": [
          {
            "fom": "2021-06-17",
            "tom": "2021-06-30",
            "satstype": "DAG",
            "sats": 669,
            "dagsats": 669,
            "lønn": 669,
            "grad": 100.0,
            "stønadsdager": 10,
            "totalbeløp": 6690,
            "endringskode": "NY",
            "delytelseId": 1,
            "refDelytelseId": null,
            "refFagsystemId": "QUS2RY3IFNFITMWGE6B2ICYZF4\r\n",
            "statuskode": null,
            "datoStatusFom": null,
            "klassekode": "SPREFAG-IOP"
          }
        ],
        "fagsystemId": "Y46RB4ZG6VCS5NW3IGPFLZ65QY\r\n"
      },
      "personOppdrag": {
        "linjer": [],
        "fagsystemId": null
      },
      "@opprettet": "2021-08-20T14:40:12.295678239"
    }
    """.trimIndent()

    @Language("json")
    val utbetalingPersonOppdragFagsystemIdLikNullExpected = """
    {
      "arbeidsgiverOppdrag": {
        "linjer": [
          {
            "fom": "2021-06-17",
            "tom": "2021-06-30",
            "satstype": "DAG",
            "sats": 669,
            "dagsats": 669,
            "lønn": 669,
            "grad": 100.0,
            "stønadsdager": 10,
            "totalbeløp": 6690,
            "endringskode": "NY",
            "delytelseId": 1,
            "refDelytelseId": null,
            "refFagsystemId": "QUS2RY3IFNFITMWGE6B2ICYZF4",
            "statuskode": null,
            "datoStatusFom": null,
            "klassekode": "SPREFAG-IOP"
          }
        ],
        "fagsystemId": "Y46RB4ZG6VCS5NW3IGPFLZ65QY"
      },
      "personOppdrag": {
        "linjer": [],
        "fagsystemId": null
      },
      "@opprettet": "2021-08-20T14:40:12.295678239"
    }
    """.trimIndent()

    @Language("json")
    val utbetalingRefFagsystemIdErNullEnLinje = """
    {
      "arbeidsgiverOppdrag": {
        "linjer": [
          {
            "fom": "2021-06-17",
            "tom": "2021-06-30",
            "satstype": "DAG",
            "sats": 669,
            "dagsats": 669,
            "lønn": 669,
            "grad": 100.0,
            "stønadsdager": 10,
            "totalbeløp": 6690,
            "endringskode": "NY",
            "delytelseId": 1,
            "refDelytelseId": null,
            "refFagsystemId": null,
            "statuskode": null,
            "datoStatusFom": null,
            "klassekode": "SPREFAG-IOP"
          }
        ],
        "fagsystemId": "QUS2RY3IFNFITMWGE6B2ICYZF4\r\n"
      },
      "personOppdrag": {
        "linjer": [],
        "fagsystemId": "Y46RB4ZG6VCS5NW3IGPFLZ65QY\r\n"
      },
      "@opprettet": "2021-08-20T14:40:12.295678239"
    }
    """.trimIndent()

    @Language("json")
    val utbetalingRefFagsystemIdErNullEnLinjeExpected = """
    {
      "arbeidsgiverOppdrag": {
        "linjer": [
          {
            "fom": "2021-06-17",
            "tom": "2021-06-30",
            "satstype": "DAG",
            "sats": 669,
            "dagsats": 669,
            "lønn": 669,
            "grad": 100.0,
            "stønadsdager": 10,
            "totalbeløp": 6690,
            "endringskode": "NY",
            "delytelseId": 1,
            "refDelytelseId": null,
            "refFagsystemId": null,
            "statuskode": null,
            "datoStatusFom": null,
            "klassekode": "SPREFAG-IOP"
          }
        ],
        "fagsystemId": "QUS2RY3IFNFITMWGE6B2ICYZF4"
      },
      "personOppdrag": {
        "linjer": [],
        "fagsystemId": "Y46RB4ZG6VCS5NW3IGPFLZ65QY"
      },
      "@opprettet": "2021-08-20T14:40:12.295678239"
    }
    """.trimIndent()

    @Language("json")
    val utbetalingRefFagsystemIdErNullFlereLinjer = """
    {
      "arbeidsgiverOppdrag": {
        "linjer": [
          {
            "fom": "2021-06-17",
            "tom": "2021-06-30",
            "satstype": "DAG",
            "sats": 669,
            "dagsats": 669,
            "lønn": 669,
            "grad": 100.0,
            "stønadsdager": 10,
            "totalbeløp": 6690,
            "endringskode": "NY",
            "delytelseId": 1,
            "refDelytelseId": null,
            "refFagsystemId": "QUS2RY3IFNFITMWGE6B2ICYZF4\r\n",
            "statuskode": null,
            "datoStatusFom": null,
            "klassekode": "SPREFAG-IOP"
          },
          {
            "fom": "2021-06-17",
            "tom": "2021-06-30",
            "satstype": "DAG",
            "sats": 669,
            "dagsats": 669,
            "lønn": 669,
            "grad": 100.0,
            "stønadsdager": 10,
            "totalbeløp": 6690,
            "endringskode": "NY",
            "delytelseId": 1,
            "refDelytelseId": null,
            "refFagsystemId": null,
            "statuskode": null,
            "datoStatusFom": null,
            "klassekode": "SPREFAG-IOP"
          }
        ],
        "fagsystemId": "QUS2RY3IFNFITMWGE6B2ICYZF4\r\n"
      },
      "personOppdrag": {
        "linjer": [],
        "fagsystemId": "Y46RB4ZG6VCS5NW3IGPFLZ65QY\r\n"
      },
      "@opprettet": "2021-08-20T14:40:12.295678239"
    }
    """.trimIndent()

    @Language("json")
    val utbetalingRefFagsystemIdErNullFlereLinjerExpected = """
    {
      "arbeidsgiverOppdrag": {
        "linjer": [
          {
            "fom": "2021-06-17",
            "tom": "2021-06-30",
            "satstype": "DAG",
            "sats": 669,
            "dagsats": 669,
            "lønn": 669,
            "grad": 100.0,
            "stønadsdager": 10,
            "totalbeløp": 6690,
            "endringskode": "NY",
            "delytelseId": 1,
            "refDelytelseId": null,
            "refFagsystemId": "QUS2RY3IFNFITMWGE6B2ICYZF4",
            "statuskode": null,
            "datoStatusFom": null,
            "klassekode": "SPREFAG-IOP"
          },
          {
            "fom": "2021-06-17",
            "tom": "2021-06-30",
            "satstype": "DAG",
            "sats": 669,
            "dagsats": 669,
            "lønn": 669,
            "grad": 100.0,
            "stønadsdager": 10,
            "totalbeløp": 6690,
            "endringskode": "NY",
            "delytelseId": 1,
            "refDelytelseId": null,
            "refFagsystemId": null,
            "statuskode": null,
            "datoStatusFom": null,
            "klassekode": "SPREFAG-IOP"
          }
        ],
        "fagsystemId": "QUS2RY3IFNFITMWGE6B2ICYZF4"
      },
      "personOppdrag": {
        "linjer": [],
        "fagsystemId": "Y46RB4ZG6VCS5NW3IGPFLZ65QY"
      },
      "@opprettet": "2021-08-20T14:40:12.295678239"
    }
    """.trimIndent()
}

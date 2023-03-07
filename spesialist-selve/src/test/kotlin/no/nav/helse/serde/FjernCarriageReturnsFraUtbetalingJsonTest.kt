package no.nav.helse.serde

import DatabaseIntegrationTest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.OVERFØRT
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class FjernCarriageReturnsFraUtbetalingJsonTest : DatabaseIntegrationTest() {
    @BeforeEach
    fun setup() {
        nyPerson()
    }

    @Test
    fun `Endrer ingenting ved fagsystemIDer uten carriage returns`() {
        val utbetalingId = opprettUtbetaling(utbetaling)
        kjørMigrering()
        val actual = hentUtbetalingMedUtbetalingId(utbetalingId)
        assertJsonEquals(utbetalingMedAltExpected, actual)
    }

    @Test
    fun `fagsystemID med carriage returns i alle tre tilfeller`() {
        val utbetalingId = opprettUtbetaling(utbetalingMedAlt)
        kjørMigrering()
        val actual = hentUtbetalingMedUtbetalingId(utbetalingId)
        assertJsonEquals(utbetalingMedAltExpected, actual)
    }

    @Test
    fun `fagsystemID med carriage returns i alle tre tilfeller og flere linjer med CR`() {
        val utbetalingId = opprettUtbetaling(utbetalingMedAltFlereLinjerMedCarriageReturn)
        kjørMigrering()
        val actual = hentUtbetalingMedUtbetalingId(utbetalingId)
        assertJsonEquals(utbetalingMedAltFlereLinjerMedCarriageReturnExpected, actual)
    }

    @Test
    fun `fagsystemID med carriage returns i alle tre tilfeller og flere linjer hvor en har CR`() {
        val utbetalingId = opprettUtbetaling(utbetalingMedAltFlereLinjerMedCarriageReturnPåEnLinje)
        kjørMigrering()
        val actual = hentUtbetalingMedUtbetalingId(utbetalingId)
        assertJsonEquals(utbetalingMedAltFlereLinjerMedCarriageReturnPåEnLinjeExpected, actual)
    }

    @Test
    fun `arbeidsgiveroppdrag-FagsystemID er null`() {
        val utbetalingId = opprettUtbetaling(utbetalingArbeidsgiverOppdragFagsystemIdLikNull)
        kjørMigrering()
        val actual = hentUtbetalingMedUtbetalingId(utbetalingId)
        assertJsonEquals(utbetalingArbeidsgiverOppdragFagsystemIdLikNullExpected, actual)
    }

    @Test
    fun `personoppdrag-FagsystemID er null`() {
        val utbetalingId = opprettUtbetaling(utbetalingPersonOppdragFagsystemIdLikNull)
        kjørMigrering()
        val actual = hentUtbetalingMedUtbetalingId(utbetalingId)
        assertJsonEquals(utbetalingPersonOppdragFagsystemIdLikNullExpected, actual)
    }

    @Test
    fun `linjer-FagsystemIDRef er null - én linje`() {
        val utbetalingId = opprettUtbetaling(utbetalingRefFagsystemIdErNullEnLinje)
        kjørMigrering()
        val actual = hentUtbetalingMedUtbetalingId(utbetalingId)
        assertJsonEquals(utbetalingRefFagsystemIdErNullEnLinjeExpected, actual)
    }

    @Test
    fun `linjer-FagsystemIDRef er null - flere linjer`() {
        val utbetalingId = opprettUtbetaling(utbetalingRefFagsystemIdErNullFlereLinjer)
        kjørMigrering()
        val actual = hentUtbetalingMedUtbetalingId(utbetalingId)
        assertJsonEquals(utbetalingRefFagsystemIdErNullFlereLinjerExpected, actual)
    }

    @Test
    fun `ignorerer utbetalinger før timestamp`() {
        val utbetalingIdNy = opprettUtbetaling(utbetalingMedAlt)
        val utbetalingIdGammel = opprettUtbetaling(utbetalingMedAlt, LocalDateTime.of(2021, 7, 1, 0, 0))
        kjørMigrering()
        val actualNy = hentUtbetalingMedUtbetalingId(utbetalingIdNy)
        val actualGammel = hentUtbetalingMedUtbetalingId(utbetalingIdGammel)
        assertJsonEquals(utbetalingMedAltExpected, actualNy)
        assertJsonEquals(utbetalingMedAlt, actualGammel)
    }

    private fun opprettUtbetaling(data: String, opprettet: LocalDateTime = now()): Long {
        val fagsystemId = fagsystemId()
        val arbeidsgiverOppdragId = lagArbeidsgiveroppdrag(fagsystemId)
        val personOppdragId = lagPersonoppdrag()
        lagLinje(arbeidsgiverOppdragId, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31))
        val utbetalingId = lagUtbetalingId(arbeidsgiverOppdragId, personOppdragId)
        utbetalingDao.nyUtbetalingStatus(utbetalingId, OVERFØRT, opprettet, data)
        return utbetalingId
    }

    private fun assertJsonEquals(expected: String, actual: String?) =
        assertEquals(objectMapper.readTree(expected), objectMapper.readTree(actual))

    private fun kjørMigrering() {
        @Language("PostgreSQL")
        val arbeidsgiverOppdragStatement = """
            UPDATE utbetaling
            SET data = jsonb_set(data::jsonb, '{arbeidsgiverOppdrag,fagsystemId}', to_jsonb(regexp_replace(data->'arbeidsgiverOppdrag'->>'fagsystemId',E'[\\r\\n]+', '', 'g')), false)
            WHERE data -> 'arbeidsgiverOppdrag' ->> 'fagsystemId' LIKE E'%\r\n' AND opprettet > '2021-08-05';
        """

        @Language("PostgreSQL")
        val personOppdragStatement = """
            UPDATE utbetaling
            SET data = jsonb_set(data::jsonb, '{personOppdrag,fagsystemId}', to_jsonb(regexp_replace(data->'personOppdrag'->>'fagsystemId',E'[\\r\\n]+', '', 'g')), false)
            WHERE data -> 'personOppdrag' ->> 'fagsystemId' LIKE E'%\r\n' AND opprettet > '2021-08-05';
        """

        @Language("PostgreSQL")
        val linjerStatement = """
            with
                linjer as
                    (select id, jsonb_array_elements((data->'arbeidsgiverOppdrag'->'linjer')::jsonb) as original_linje
                    from utbetaling
                    where opprettet > '2021-08-05'),
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
            update utbetaling
            set data = jsonb_set(data::jsonb, '{arbeidsgiverOppdrag,linjer}', (select jsonb_agg(modifisert_linje) from modifiedLinjer WHERE modifiedLinjer.id = utbetaling.id))
            where utbetaling.id in (select id from linjer where original_linje ->>'refFagsystemId' LIKE E'%\r\n');
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
      }
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
      }
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
      }
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
      }
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
      }
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
      }
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
      }
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
      }
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
      }
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
      }
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
      }
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
      }
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
      }
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
      }
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
      }
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
      }
    }
    """.trimIndent()
}

package no.nav.helse.mediator.meldinger

import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.AbstractDatabaseTest
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PersonavstemmingRiverTest: AbstractDatabaseTest() {

    private val testRapid = TestRapid().apply {
        Personavstemming.River(this, dataSource)
    }

    @BeforeEach
    fun setUp() {
        testRapid.reset()
    }

    @Test
    fun `leser person_avstemt`() {
        testRapid.sendTestMessage(event)
        assertGenerasjoner("9a925d22-1ad3-47ee-89e8-f544e6177292", 0, 1)
        assertGenerasjoner("b23a964a-8a4e-459c-9e32-7c00388a9f1a", 1, 0)
        assertGenerasjoner("a061b3bb-88b3-4a3d-a4e8-dacd2a3e7317", 1, 2)
        assertGenerasjoner("d09c7414-2645-49bf-9002-fd7e601a42e0", 1, 0)
        assertGenerasjoner("5dd38c1b-24bf-43fc-b1ac-658ecd34404f", 0, 1)
    }

    private fun assertGenerasjoner(vedtaksperiodeId: String, forventetAntallUlåste: Int, forventetAntallLåste: Int) {
        val antallUlåste = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT COUNT(1) FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ? AND låst = false"
            session.run(queryOf(query, UUID.fromString(vedtaksperiodeId)).map { it.int(1) }.asSingle)
        }
        val antallLåste = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT COUNT(1) FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ? AND låst = true"
            session.run(queryOf(query, UUID.fromString(vedtaksperiodeId)).map { it.int(1) }.asSingle)
        }
        assertEquals(forventetAntallUlåste, antallUlåste)
        assertEquals(forventetAntallLåste, antallLåste)
    }

    @Language("JSON")
    private val event = """{
      "@event_name": "person_avstemt",
      "arbeidsgivere": [
        {
          "organisasjonsnummer": "987654321",
          "vedtaksperioder": [
            {
              "id": "9a925d22-1ad3-47ee-89e8-f544e6177292",
              "tilstand": "AVSLUTTET",
              "opprettet": "2022-11-23T12:52:41.074236",
              "oppdatert": "2022-11-23T12:52:41.624746",
              "utbetalinger": [
                "ab0dbe6b-1f19-4dbd-beab-ad3550464c92"
              ]
            },
            {
              "id": "b23a964a-8a4e-459c-9e32-7c00388a9f1a",
              "tilstand": "AVVENTER_SIMULERING",
              "opprettet": "2022-11-23T12:52:41.787149",
              "oppdatert": "2022-11-23T12:52:41.957408",
              "utbetalinger": [
                "0ac13895-d324-429b-8213-1cf67d233cf7"
              ]
            },
            {
              "id": "a061b3bb-88b3-4a3d-a4e8-dacd2a3e7317",
              "tilstand": "AVVENTER_SIMULERING",
              "opprettet": "2022-11-20T12:52:41.787149",
              "oppdatert": "2022-11-20T12:52:41.957408",
              "utbetalinger": [
                "fe43ed13-280b-4676-991e-02afd65faeaf",
                "4cf65a62-1312-4673-91b7-7ac64cc9880b",
                "4c1d8e06-68b0-48eb-9c85-f064016dd8ed"
              ]
            },
            {
              "id": "d09c7414-2645-49bf-9002-fd7e601a42e0",
              "tilstand": "AVVENTER_HISTORIKK",
              "opprettet": "2022-11-23T12:52:41.787149",
              "oppdatert": "2022-11-23T12:52:41.957408",
              "utbetalinger": []
            }
            ,
            {
              "id": "5dd38c1b-24bf-43fc-b1ac-658ecd34404f",
              "tilstand": "AVSLUTTET_UTEN_UTBETALING",
              "opprettet": "2022-11-23T12:52:41.787149",
              "oppdatert": "2022-11-23T12:52:41.957408",
              "utbetalinger": []
            }
          ],
          "forkastedeVedtaksperioder": [
            {
              "id": "09c2b8c3-c25a-4582-a34d-2296f3df5c36",
              "tilstand": "TIL_INFOTRYGD",
              "opprettet": "2022-11-23T12:52:41.707557",
              "oppdatert": "2022-11-23T12:52:41.760996",
              "utbetalinger": []
            },
            {
              "id": "2da2f98f-8ab7-4456-9b08-49db1332486c",
              "tilstand": "TIL_INFOTRYGD",
              "opprettet": "2022-11-23T12:52:41.762828",
              "oppdatert": "2022-11-23T12:52:41.76302",
              "utbetalinger": []
            }
          ],
          "utbetalinger": [
            {
              "id": "ab0dbe6b-1f19-4dbd-beab-ad3550464c92",
              "type": "UTBETALING",
              "status": "UTBETALT",
              "opprettet": "2022-11-23T12:52:41.446479",
              "oppdatert": "2022-11-23T12:52:41.61707"
            },
            {
              "id": "0ac13895-d324-429b-8213-1cf67d233cf7",
              "type": "UTBETALING",
              "status": "IKKE_UTBETALT",
              "opprettet": "2022-11-23T12:52:41.956677",
              "oppdatert": "2022-11-23T12:52:41.95671"
            },
            {
              "id": "fe43ed13-280b-4676-991e-02afd65faeaf",
              "type": "UTBETALING",
              "status": "UTBETALT",
              "opprettet": "2022-11-22T12:52:41.956677",
              "oppdatert": "2022-11-22T12:52:41.95671"
            },
            {
              "id": "4cf65a62-1312-4673-91b7-7ac64cc9880b",
              "type": "UTBETALING",
              "status": "IKKE_UTBETALT",
              "opprettet": "2022-11-24T12:52:41.956677",
              "oppdatert": "2022-11-24T12:52:41.95671"
            },
            {
              "id": "4c1d8e06-68b0-48eb-9c85-f064016dd8ed",
              "type": "UTBETALING",
              "status": "GODKJENT_UTEN_UTBETALING",
              "opprettet": "2022-11-23T12:52:41.956677",
              "oppdatert": "2022-11-23T12:52:41.95671"
            }
          ]
        }
      ],
      "@id": "c405a203-264e-4496-99dc-785e76ede254",
      "@opprettet": "2022-11-23T12:52:42.017867",
      "system_read_count": 0,
      "system_participating_services": [
        {
          "id": "c405a203-264e-4496-99dc-785e76ede254",
          "time": "2022-11-23T12:52:42.017867"
        }
      ],
      "fødselsnummer": "12029240045",
      "aktørId": "42",
      "@forårsaket_av": {
        "id": "4703284f-3f84-4f04-83a0-fb561c65c2fc",
        "opprettet": "2022-11-23T12:52:41.985026",
        "event_name": "person_avstemming"
      }
    }"""
}
package no.nav.helse.migrering

import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PersonavstemmingRiverTest : AbstractDatabaseTest() {

    private val sparsomDao: SparsomDao = mockk(relaxed = true)
    private val spesialistDao: SpesialistDao = SpesialistDao(dataSource)
    private val v1 = UUID.fromString("9a925d22-1ad3-47ee-89e8-f544e6177292")
    private val v2 = UUID.fromString("b23a964a-8a4e-459c-9e32-7c00388a9f1a")
    private val v3 = UUID.fromString("a061b3bb-88b3-4a3d-a4e8-dacd2a3e7317")
    private val v4 = UUID.fromString("d09c7414-2645-49bf-9002-fd7e601a42e0")
    private val v5 = UUID.fromString("5dd38c1b-24bf-43fc-b1ac-658ecd34404f")

    private val testRapid = TestRapid().apply {
        Personavstemming.River(this, sparsomDao, spesialistDao)
    }

    @BeforeEach
    fun setUp() {
        testRapid.reset()
    }

    @Test
    fun `leser person_avstemt`() {
        testRapid.sendTestMessage(event)
        assertGenerasjoner(v1, 0, 1)
        assertGenerasjoner(v2, 1, 0)
        assertGenerasjoner(v3, 1, 2)
        assertGenerasjoner(v4, 1, 0)
        assertGenerasjoner(v5, 0, 1)
    }

    @Test
    fun `lagrer varsler sammen med generasjoner`() {
        val v3Opprettet = LocalDateTime.parse("2022-11-20T12:52:41.787149")
        val v3g1Utbetalt = LocalDateTime.parse("2022-11-22T12:52:41.95671")
        val v3g2Utbetalt = LocalDateTime.parse("2022-11-23T12:52:41.95671")
        val v3g3Utbetalt = LocalDateTime.parse("2022-11-24T12:52:41.95671")
        every { sparsomDao.finnVarslerFor(any()) } returns listOf(
            lagVarsel(v3, "Arbeidsgiver er ikke registrert i Aa-registeret.", UUID.randomUUID(), v3Opprettet),
            lagVarsel(v3, "Arbeidsgiver er ikke registrert i Aa-registeret.", UUID.randomUUID(), v3g1Utbetalt.minusHours(1)),
            lagVarsel(v3, "Arbeidsgiver er ikke registrert i Aa-registeret.", UUID.randomUUID(), v3g2Utbetalt.minusHours(1)),
            lagVarsel(v3, "Arbeidsgiver er ikke registrert i Aa-registeret.", UUID.randomUUID(), v3g3Utbetalt.minusHours(1)),
        )
        testRapid.sendTestMessage(event)
        assertVarselPåGenerasjon(v3, 0, "RV_VV_1", "RV_VV_1")
        assertVarselPåGenerasjon(v3, 1, "RV_VV_1")
        assertVarselPåGenerasjon(v3, 2, "RV_VV_1")
    }

    private fun assertVarselPåGenerasjon(vedtaksperiodeId: UUID, generasjonIndex: Int, vararg varselkode: String) {
        @Language("PostgreSQL")
        val query1 = "SELECT id, opprettet_tidspunkt FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ? ORDER BY opprettet_tidspunkt;"

        val (generasjonId, opprettet) = sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query1,
                    vedtaksperiodeId
                ).map {
                    it.long("id") to it.localDateTime("opprettet_tidspunkt")
                }.asList)[generasjonIndex]
        }

        @Language("PostgreSQL")
        val query2 = "SELECT kode FROM selve_varsel WHERE generasjon_ref = ?;"

        val varselkoder = sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query2,
                    generasjonId
                ).map {
                    it.string("kode")
                }.asList)
        }

        assertEquals(varselkode.toList(), varselkoder)
    }

    private fun assertGenerasjoner(vedtaksperiodeId: UUID, forventetAntallUlåste: Int, forventetAntallLåste: Int) {
        val antallUlåste = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query =
                "SELECT COUNT(1) FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ? AND låst = false"
            session.run(queryOf(query, vedtaksperiodeId).map { it.int(1) }.asSingle)
        }
        val antallLåste = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query =
                "SELECT COUNT(1) FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ? AND låst = true"
            session.run(queryOf(query, vedtaksperiodeId).map { it.int(1) }.asSingle)
        }
        assertEquals(forventetAntallUlåste, antallUlåste)
        assertEquals(forventetAntallLåste, antallLåste)
    }

    private fun lagVarsel(vedtaksperiodeId: UUID, melding: String, id: UUID, opprettet: LocalDateTime): Varsel {
        return Varsel(
            vedtaksperiodeId,
            melding,
            opprettet,
            id,
        )
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
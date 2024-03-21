package no.nav.helse.migrering

import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.migrering.db.SpesialistDao
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PersonavstemmingTest : AbstractDatabaseTest() {

    private val spesialistDao = SpesialistDao(dataSource)
    private val testRapid = TestRapid().apply {
        Personavstemming.River(this, spesialistDao)
    }

    @BeforeEach
    fun setUp() {
        testRapid.reset()
    }

    @Test
    fun `markerer periode som forkastet`() {
        val vedtaksperiodeId = UUID.randomUUID()
        opprettPersonFor("12345678910", "1234567891011")
        opprettArbeidsgiverFor("987654321")
        opprettVedtaksperiodeFor("12345678910", "987654321", vedtaksperiodeId)
        assertVedtaksperiode(vedtaksperiodeId, 1)
        testRapid.sendTestMessage(testevent(vedtaksperiodeId))
        assertForkastetVedtaksperiode(vedtaksperiodeId, 1)
    }

    private fun assertVedtaksperiode(vedtaksperiodeId: UUID, forventetAntall: Int) {
        @Language("PostgreSQL") val query = "SELECT count(1) FROM vedtak WHERE vedtaksperiode_id = ?"
        val antall = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, vedtaksperiodeId).map { it.int(1) }.asSingle)
        }
        assertEquals(forventetAntall, antall)
    }

    private fun assertForkastetVedtaksperiode(vedtaksperiodeId: UUID, forventetAntall: Int) {
        @Language("PostgreSQL") val query = "SELECT count(1) FROM vedtak WHERE vedtaksperiode_id = ? AND forkastet = true"
        val antall = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, vedtaksperiodeId).map { it.int(1) }.asSingle)
        }
        assertEquals(forventetAntall, antall)
    }

    private fun opprettPersonFor(fødselsnummer: String, aktørId: String) {
        @Language("PostgreSQL")
        val query =
            """
                INSERT INTO person(fodselsnummer, aktor_id, info_ref, enhet_ref, enhet_ref_oppdatert, personinfo_oppdatert, infotrygdutbetalinger_ref, infotrygdutbetalinger_oppdatert) 
                VALUES (?, ?, null, null, null, null, null, null)
            """
        sessionOf(dataSource).use { session ->
            session.run(queryOf(query, fødselsnummer.toLong(), aktørId.toLong()).asExecute)
        }
    }

    private fun opprettArbeidsgiverFor(organisasjonsnummer: String) {
        @Language("PostgreSQL")
        val query =
            """
                INSERT INTO arbeidsgiver(orgnummer, navn_ref, bransjer_ref) 
                VALUES (?, null, null)
            """
        sessionOf(dataSource).use { session ->
            session.run(queryOf(query, organisasjonsnummer.toLong()).asExecute)
        }
    }

    private fun opprettVedtaksperiodeFor(fødselsnummer: String, organisasjonsnummer: String, vedtaksperiodeId: UUID) {
        @Language("PostgreSQL")
        val query =
            """
                INSERT INTO vedtak (vedtaksperiode_id, fom, tom, arbeidsgiver_ref, person_ref, forkastet, forkastet_tidspunkt, forkastet_av_hendelse) 
                VALUES (
                    ?, 
                    '2018-01-01', 
                    '2018-01-31',
                    (SELECT id FROM arbeidsgiver WHERE orgnummer = ?),
                    (SELECT id FROM person WHERE fodselsnummer = ?),
                    false,
                    null,
                    null
                )
            """
        sessionOf(dataSource).use { session ->
            session.run(queryOf(query, vedtaksperiodeId, organisasjonsnummer.toLong(), fødselsnummer.toLong()).asExecute)
        }
    }

    @Language("JSON")
    private fun testevent(forkastetVedtaksperiodeId: UUID = UUID.randomUUID()): String {
        return """{
  "@event_name": "person_avstemt",
  "arbeidsgivere": [
    {
      "organisasjonsnummer": "987654321",
      "forkastedeVedtaksperioder": [
        {
          "id": "$forkastetVedtaksperiodeId",
          "fom": "2018-01-01",
          "tom": "2018-01-31",
          "skjæringstidspunkt": "2018-01-01",
          "opprettet": "2022-11-23T12:52:42.017867",
          "oppdatert": "2022-11-23T12:52:42.017867"
        }
      ]
    }
  ],
  "@id": "c405a203-264e-4496-99dc-785e76ede254",
  "@opprettet": "2022-11-23T12:52:42.017867",
  "fødselsnummer": "12345678910",
  "aktørId": "42"
}
            
        """
    }
}
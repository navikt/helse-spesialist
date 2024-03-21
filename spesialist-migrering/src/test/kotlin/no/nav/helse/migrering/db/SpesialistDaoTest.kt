package no.nav.helse.migrering.db

import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.migrering.AbstractDatabaseTest
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SpesialistDaoTest : AbstractDatabaseTest() {
    private val dao = SpesialistDao(dataSource)

    @Test
    fun `Oppdaterer forkastet på ny vedtaksperiode`() {
        val vedtaksperiodeId = UUID.randomUUID()
        opprettPersonFor("12345678910", "1234567891011")
        opprettArbeidsgiverFor("987654321")
        opprettVedtaksperiodeFor("12345678910", "987654321", vedtaksperiodeId)
        assertVedtaksperiode(vedtaksperiodeId, 1)
        dao.forkast(vedtaksperiodeId)
        assertForkastetVedtaksperiode(vedtaksperiodeId, 1)
    }

    private fun assertVedtaksperiode(vedtaksperiodeId: UUID, forventetAntall: Int) {
        @Language("PostgreSQL")
        val query =
            "SELECT count(1) FROM vedtak WHERE vedtaksperiode_id = ? AND forkastet = false AND forkastet_av_hendelse IS NULL "
        val antallFunnet = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, vedtaksperiodeId).map { it.int(1) }.asSingle)
        }
        assertEquals(forventetAntall, antallFunnet)
    }

    private fun assertForkastetVedtaksperiode(vedtaksperiodeId: UUID, forventetAntall: Int) {
        @Language("PostgreSQL")
        val query =
            "SELECT count(1) FROM vedtak WHERE vedtaksperiode_id = ? AND forkastet = true AND forkastet_av_hendelse = ?"
        val antallFunnet = sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query,
                    vedtaksperiodeId,
                    UUID.fromString("00000000-0000-0000-0000-000000000000")
                ).map { it.int(1) }.asSingle
            )
        }
        assertEquals(forventetAntall, antallFunnet)
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
}
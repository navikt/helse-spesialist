package no.nav.helse.migrering.db

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.migrering.AbstractDatabaseTest
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class SpesialistDaoTest: AbstractDatabaseTest() {
    private val dao = SpesialistDao(dataSource)

    @Test
    fun `Kan opprette person`() {
        dao.personOpprettet("1234", "134")
        assertPerson("1234", "134", 1)
    }

    @Test
    fun `Opretter ikke person flere ganger`() {
        dao.personOpprettet("1234", "134")
        dao.personOpprettet("1234", "134")

        assertPerson("1234", "134", 1)
    }
    @Test
    fun `Kan opprette arbeidsgiver`() {
        dao.arbeidsgiverOpprettet("1234")
        assertArbeidsgiver("1234",  1)
    }
    @Test
    fun `Opretter ikke arbeidsgiver flere ganger`() {
        dao.arbeidsgiverOpprettet("1234")
        dao.arbeidsgiverOpprettet("1234")
        assertArbeidsgiver("1234",  1)
    }

    private fun assertPerson(aktørId: String, fødselsnummer: String, forventetAntall: Int) {
        @Language("PostgreSQL")
        val query = "SELECT count(1) FROM person WHERE aktor_id = ? AND fodselsnummer = ?"
        val antallFunnet = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, aktørId.toLong(), fødselsnummer.toLong()).map { it.int(1) }.asSingle)
        }
        assertEquals(forventetAntall, antallFunnet)
    }

    private fun assertArbeidsgiver(organisasjonsnummer: String, forventetAntall: Int) {
        @Language("PostgreSQL")
        val query = "SELECT count(1) FROM arbeidsgiver WHERE orgnummer = ?"
        val antallFunnet = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, organisasjonsnummer.toLong()).map { it.int(1) }.asSingle)
        }
        assertEquals(forventetAntall, antallFunnet)
    }
}
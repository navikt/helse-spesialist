package no.nav.helse.migrering.db

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.migrering.AbstractDatabaseTest
import no.nav.helse.migrering.februar
import no.nav.helse.migrering.januar
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
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
    fun `Oppretter ikke arbeidsgiver flere ganger`() {
        dao.arbeidsgiverOpprettet("1234")
        dao.arbeidsgiverOpprettet("1234")
        assertArbeidsgiver("1234",  1)
    }
    @Test
    fun `Kan opprette vedtaksperiode`() {
        dao.personOpprettet("1234", "123")
        dao.arbeidsgiverOpprettet("1234")
        val vedtaksperiodeId = UUID.randomUUID()
        dao.vedtaksperiodeOpprettet(vedtaksperiodeId, LocalDateTime.now(), 1.januar, 15.januar, 1.januar, "123", "1234", false)
        assertVedtaksperiode(vedtaksperiodeId,  1)
    }
    @Test
    fun `Oppretter ikke vedtaksperiode hvis den finnes fra før av`() {
        dao.personOpprettet("1234", "123")
        dao.arbeidsgiverOpprettet("1234")
        val vedtaksperiodeId = UUID.randomUUID()
        dao.vedtaksperiodeOpprettet(vedtaksperiodeId, LocalDateTime.now(), 1.januar, 15.januar, 1.januar, "123", "1234", false)
        dao.vedtaksperiodeOpprettet(vedtaksperiodeId, LocalDateTime.now(), 1.januar, 15.januar, 1.januar, "123", "1234", false)
        assertVedtaksperiode(vedtaksperiodeId,  1)
    }
    @Test
    fun `Oppdaterer generasjoner for ekstisterende vedtaksperiode`() {
        dao.personOpprettet("1234", "123")
        dao.arbeidsgiverOpprettet("1234")
        val vedtaksperiodeId = UUID.randomUUID()
        opprettGenerasjonFor(vedtaksperiodeId)
        assertGenerasjonMed(vedtaksperiodeId, 1)

        dao.vedtaksperiodeOpprettet(vedtaksperiodeId, LocalDateTime.now(), 1.januar, 15.januar, 1.januar, "123", "1234", false)
        assertVedtaksperiode(vedtaksperiodeId,  1)
        assertGenerasjonMed(vedtaksperiodeId, 1.januar, 15.januar, 1.januar, 1)
    }
    @Test
    fun `Oppdaterer generasjoner for ekstisterende vedtaksperiode kun for generasjoner som ikke har satt fom, tom eller skjæringstidspunkt`() {
        dao.personOpprettet("1234", "123")
        dao.arbeidsgiverOpprettet("1234")
        val vedtaksperiodeId = UUID.randomUUID()
        opprettGenerasjonFor(vedtaksperiodeId)
        dao.vedtaksperiodeOpprettet(vedtaksperiodeId, LocalDateTime.now(), 1.januar, 31.januar, 1.januar, "123", "1234", false)
        opprettGenerasjonFor(vedtaksperiodeId)
        opprettGenerasjonFor(vedtaksperiodeId)
        assertGenerasjonMed(vedtaksperiodeId, 2)
        assertGenerasjonMed(vedtaksperiodeId, 1.januar, 31.januar, 1.januar, 1)

        dao.vedtaksperiodeOpprettet(vedtaksperiodeId, LocalDateTime.now(), 15.januar, 15.februar, 15.januar, "123", "1234", false)
        assertVedtaksperiode(vedtaksperiodeId,  1)
        assertGenerasjonMed(vedtaksperiodeId, 1.januar, 31.januar, 1.januar, 1)
        assertGenerasjonMed(vedtaksperiodeId, 15.januar, 15.februar, 15.januar, 2)
    }

    @Test
    fun `Oppretter ikke vedtaksperiode hvis den er forkastet`() {
        dao.personOpprettet("1234", "123")
        dao.arbeidsgiverOpprettet("1234")
        val vedtaksperiodeId = UUID.randomUUID()
        dao.vedtaksperiodeOpprettet(vedtaksperiodeId, LocalDateTime.now(), 1.januar, 15.januar, 1.januar, "123", "1234", true)
        assertVedtaksperiode(vedtaksperiodeId, 0)
        assertForkastetVedtaksperiode(vedtaksperiodeId, 0)
    }

    @Test
    fun `Oppdaterer forkastet på ny vedtaksperiode`() {
        dao.personOpprettet("1234", "123")
        dao.arbeidsgiverOpprettet("1234")
        val vedtaksperiodeId = UUID.randomUUID()
        dao.vedtaksperiodeOpprettet(vedtaksperiodeId, LocalDateTime.now(), 1.januar, 15.januar, 1.januar, "123", "1234", false)
        assertVedtaksperiode(vedtaksperiodeId, 1)
        dao.vedtaksperiodeOpprettet(vedtaksperiodeId, LocalDateTime.now(), 1.januar, 15.januar, 1.januar, "123", "1234", true)

        assertForkastetVedtaksperiode(vedtaksperiodeId, 1)
    }

    @Test
    fun `Oppdaterer forkastet på eksisterende vedtaksperiode`() {
        dao.personOpprettet("1234", "123")
        dao.arbeidsgiverOpprettet("1234")
        val vedtaksperiodeId = UUID.randomUUID()
        dao.vedtaksperiodeOpprettet(vedtaksperiodeId, LocalDateTime.now(), 1.januar, 15.januar, 1.januar, "123", "1234", true)
        dao.vedtaksperiodeOpprettet(vedtaksperiodeId, LocalDateTime.now(), 1.januar, 15.januar, 1.januar, "123", "1234", false)

        assertVedtaksperiode(vedtaksperiodeId, 1)
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
    private fun assertVedtaksperiode(vedtaksperiodeId: UUID, forventetAntall: Int) {
        @Language("PostgreSQL")
        val query = "SELECT count(1) FROM vedtak WHERE vedtaksperiode_id = ? AND forkastet = false AND forkastet_av_hendelse IS NULL "
        val antallFunnet = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, vedtaksperiodeId).map { it.int(1) }.asSingle)
        }
        assertEquals(forventetAntall, antallFunnet)
    }

    private fun assertForkastetVedtaksperiode(vedtaksperiodeId: UUID, forventetAntall: Int) {
        @Language("PostgreSQL")
        val query = "SELECT count(1) FROM vedtak WHERE vedtaksperiode_id = ? AND forkastet = true AND forkastet_av_hendelse = ?"
        val antallFunnet = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, vedtaksperiodeId, UUID.fromString("00000000-0000-0000-0000-000000000000")).map { it.int(1) }.asSingle)
        }
        assertEquals(forventetAntall, antallFunnet)
    }

    private fun opprettGenerasjonFor(vedtaksperiodeId: UUID) {
        @Language("PostgreSQL")
        val query = "INSERT INTO selve_vedtaksperiode_generasjon (vedtaksperiode_id, opprettet_av_hendelse) VALUES (?, gen_random_uuid())"
        sessionOf(dataSource).use { session ->
            session.run(queryOf(query, vedtaksperiodeId).asExecute)
        }
    }

    private fun assertGenerasjonMed(vedtaksperiodeId: UUID, forventetAntall: Int) {
        @Language("PostgreSQL")
        val query = "SELECT count(1) FROM selve_vedtaksperiode_generasjon svg WHERE vedtaksperiode_id = ? AND fom IS NULL AND tom IS NULL AND skjæringstidspunkt is null "
        val antallFunnet = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, vedtaksperiodeId).map { it.int(1) }.asSingle)
        }
        assertEquals(forventetAntall, antallFunnet)
    }
    private fun assertGenerasjonMed(vedtaksperiodeId: UUID, fom: LocalDate, tom: LocalDate, skjæringstidspunkt: LocalDate, forventetAntall: Int) {
        @Language("PostgreSQL")
        val query = "SELECT count(1) FROM selve_vedtaksperiode_generasjon svg WHERE vedtaksperiode_id = ? AND fom = ? AND tom = ? AND skjæringstidspunkt = ? "
        val antallFunnet = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, vedtaksperiodeId, fom, tom, skjæringstidspunkt).map { it.int(1) }.asSingle)
        }
        assertEquals(forventetAntall, antallFunnet)
    }
}
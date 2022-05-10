package no.nav.helse.modell

import DatabaseIntegrationTest
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.LocalDate
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLPerson
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.postgresql.util.PSQLException

internal class VedtakDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `lagre vedtak`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettSnapshot()
        opprettSnapshot()
        vedtakDao.opprett(VEDTAKSPERIODE, FOM, TOM, personId, arbeidsgiverId, snapshotId)
        assertEquals(1, vedtak().size)
        vedtak().first().assertEquals(VEDTAKSPERIODE, FOM, TOM, personId, arbeidsgiverId, snapshotId)
    }

    @Test
    fun `opprette duplikat vedtak`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettSnapshot()
        opprettSnapshot()
        vedtakDao.opprett(VEDTAKSPERIODE, FOM, TOM, personId, arbeidsgiverId, snapshotId)
        val nyFom = LocalDate.now().minusMonths(1)
        val nyTom = LocalDate.now()
        val nySnapshotRef = snapshotDao.lagre(FNR, snapshot().data!!.person!!)
        assertThrows<PSQLException> {
            vedtakDao.opprett(
                VEDTAKSPERIODE,
                nyFom,
                nyTom,
                personId,
                arbeidsgiverId,
                nySnapshotRef
            )
        }
    }

    @Test
    fun `oppdatere vedtak`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettSnapshot()
        opprettVedtaksperiode()
        val nyFom = LocalDate.now().minusMonths(1)
        val nyTom = LocalDate.now()
        val nySnapshotRef = snapshotDao.lagre(FNR, snapshot().data!!.person!!)
        vedtakDao.oppdaterSnaphot(
            vedtakRef = vedtakId,
            fom = nyFom,
            tom = nyTom,
            snapshotRef = nySnapshotRef
        )
        assertEquals(1, vedtak().size)
        vedtak().first().assertEquals(
            forventetVedtaksperiodeId = VEDTAKSPERIODE,
            forventetFom = nyFom,
            forventetTom = nyTom,
            forventetPersonRef = personId,
            forventetArbeidsgiverRef = arbeidsgiverId,
            forventetSnapshotRef = nySnapshotRef
        )
    }

    @Test
    fun `lagrer og leser vedtaksperiodetype hvis den er satt`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        val vedtaksperiodetype = Periodetype.FØRSTEGANGSBEHANDLING
        val inntektskilde = Inntektskilde.EN_ARBEIDSGIVER
        vedtakDao.leggTilVedtaksperiodetype(VEDTAKSPERIODE, vedtaksperiodetype, inntektskilde)
        assertEquals(Periodetype.FØRSTEGANGSBEHANDLING, vedtakDao.finnVedtaksperiodetype(VEDTAKSPERIODE))
        assertEquals(inntektskilde, vedtakDao.finnInntektskilde(VEDTAKSPERIODE))
    }

    @Test
    fun `oppretter innslag i koblingstabellen`() {
        godkjenningsbehov(HENDELSE_ID)
        nyPerson()
        vedtakDao.opprettKobling(VEDTAKSPERIODE, HENDELSE_ID)
        assertEquals(VEDTAKSPERIODE, finnKobling(HENDELSE_ID))
    }

    @Test
    fun `fjerner innslag i koblingstabellen`() {
        godkjenningsbehov(HENDELSE_ID)
        nyPerson()
        vedtakDao.opprettKobling(VEDTAKSPERIODE, HENDELSE_ID)
        assertEquals(VEDTAKSPERIODE, finnKobling(HENDELSE_ID))

        vedtakDao.fjernKobling(VEDTAKSPERIODE, HENDELSE_ID)

        assertNull(finnKobling(HENDELSE_ID))
    }

    @Test
    fun `insert speil snapshot`() {
        val nyPerson = snapshot().data!!.person!!
        val nyVedtaksperiode = UUID.randomUUID()
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettVedtaksperiode(nyVedtaksperiode)
        snapshotDao.lagre(FNR, nyPerson)
        val vedtak = vedtak()
        assertTrue(
            vedtak.all { finnSnapshot(it.snapshotRef) == nyPerson },
            "Alle snapshots på person skal matche den nye jsonen"
        )
    }

    @Test
    fun `ikke automatisk godkjent dersom det ikke finnes innslag i db`() {
        nyPerson()
        assertFalse(vedtakDao.erAutomatiskGodkjent(UTBETALING_ID))
    }

    @Test
    fun `ikke automatisk godkjent dersom innslag i db sier false`() {
        godkjenningsbehov(HENDELSE_ID)
        nyPerson()
        nyttAutomatiseringsinnslag(false)
        assertFalse(vedtakDao.erAutomatiskGodkjent(UTBETALING_ID))
    }

    @Test
    fun `automatisk godkjent dersom innslag i db sier true`() {
        godkjenningsbehov(HENDELSE_ID)
        nyPerson()
        nyttAutomatiseringsinnslag(true)
        assertTrue(vedtakDao.erAutomatiskGodkjent(UTBETALING_ID))
    }

    private fun finnKobling(hendelseId: UUID) = sessionOf(dataSource).use {
        it.run(
            queryOf("SELECT vedtaksperiode_id FROM vedtaksperiode_hendelse WHERE hendelse_ref = ?", hendelseId)
                .map { row -> UUID.fromString(row.string(1)) }.asSingle
        )
    }

    private fun vedtak() = sessionOf(dataSource).use {
        it.run(queryOf("SELECT vedtaksperiode_id, fom, tom, person_ref, arbeidsgiver_ref, snapshot_ref FROM vedtak").map { row ->
            Vedtak(
                vedtaksperiodeId = UUID.fromString(row.string("vedtaksperiode_id")),
                fom = row.localDate("fom"),
                tom = row.localDate("tom"),
                personRef = row.long("person_ref"),
                arbeidsgiverRef = row.long("arbeidsgiver_ref"),
                snapshotRef = row.int("snapshot_ref")
            )
        }.asList)
    }

    private class Vedtak(
        private val vedtaksperiodeId: UUID,
        private val fom: LocalDate,
        private val tom: LocalDate,
        private val personRef: Long,
        private val arbeidsgiverRef: Long,
        val snapshotRef: Int
    ) {
        fun assertEquals(
            forventetVedtaksperiodeId: UUID,
            forventetFom: LocalDate,
            forventetTom: LocalDate,
            forventetPersonRef: Long,
            forventetArbeidsgiverRef: Long,
            forventetSnapshotRef: Int
        ) {
            assertEquals(forventetVedtaksperiodeId, vedtaksperiodeId)
            assertEquals(forventetFom, fom)
            assertEquals(forventetTom, tom)
            assertEquals(forventetPersonRef, personRef)
            assertEquals(forventetArbeidsgiverRef, arbeidsgiverRef)
            assertEquals(forventetSnapshotRef, snapshotRef)
        }
    }

    private fun finnSnapshot(snapshotRef: Int): GraphQLPerson? = sessionOf(dataSource).use { session ->
        session.run(
            queryOf(
                "SELECT data FROM snapshot WHERE id = ?",
                snapshotRef
            ).map { objectMapper.readValue<GraphQLPerson>(it.string("data")) }.asSingle
        )
    }
}

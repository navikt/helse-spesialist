package no.nav.helse.modell

import DatabaseIntegrationTest
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.postgresql.util.PSQLException
import java.time.LocalDate
import java.util.*

internal class VedtakDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `lagre vedtak`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettSnapshot()
        opprettGraphQLSnapshot()
        vedtakDao.opprett(VEDTAKSPERIODE, FOM, TOM, personId, arbeidsgiverId, snapshotId, graphQLSnapshotId)
        assertEquals(1, vedtak().size)
        vedtak().first().assertEquals(
            forventetVedtaksperiodeId = VEDTAKSPERIODE,
            forventetFom = FOM,
            forventetTom = TOM,
            forventetPersonRef = personId,
            forventetArbeidsgiverRef = arbeidsgiverId,
            forventetSnapshotRef = snapshotId,
            forventetGraphQLSnapshotRef = graphQLSnapshotId
        )
    }

    @Test
    fun `opprette duplikat vedtak`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettSnapshot()
        opprettGraphQLSnapshot()
        vedtakDao.opprett(VEDTAKSPERIODE, FOM, TOM, personId, arbeidsgiverId, snapshotId, graphQLSnapshotId)
        val nyFom = LocalDate.now().minusMonths(1)
        val nyTom = LocalDate.now()
        val nySnapshotRef = speilSnapshotDao.lagre(FNR, snapshot())
        val nyGraphQLSnapshotRef = snapshotDao.lagre(FNR, graphQLSnapshot())
        assertThrows<PSQLException> {
            vedtakDao.opprett(
                vedtaksperiodeId = VEDTAKSPERIODE,
                fom = nyFom,
                tom = nyTom,
                personRef = personId,
                arbeidsgiverRef = arbeidsgiverId,
                speilSnapshotRef = nySnapshotRef,
                graphQLSnapshotRef = nyGraphQLSnapshotRef
            )
        }
    }

    @Test
    fun `oppdatere vedtak`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettSnapshot()
        opprettGraphQLSnapshot()
        opprettVedtaksperiode()
        val nyFom = LocalDate.now().minusMonths(1)
        val nyTom = LocalDate.now()
        val nySnapshotRef = speilSnapshotDao.lagre(FNR, snapshot())
        val nyGraphQLSnapshotRef = snapshotDao.lagre(FNR, graphQLSnapshot())
        vedtakDao.oppdater(vedtakId, nyFom, nyTom, nySnapshotRef)
        assertEquals(1, vedtak().size)
        vedtak().first()
            .assertEquals(
                forventetVedtaksperiodeId = VEDTAKSPERIODE,
                forventetFom = nyFom,
                forventetTom = nyTom,
                forventetPersonRef = personId,
                forventetArbeidsgiverRef = arbeidsgiverId,
                forventetSnapshotRef = nySnapshotRef,
                forventetGraphQLSnapshotRef = nyGraphQLSnapshotRef
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
        val newJson = snapshot()
        val nyVedtaksperiode = UUID.randomUUID()
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettVedtaksperiode(nyVedtaksperiode)
        speilSnapshotDao.lagre(FNR, newJson)
        val vedtak = vedtak()
        assertTrue(
            vedtak.all { finnSnapshot(it.snapshotRef) == newJson },
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
        it.run(queryOf("SELECT vedtaksperiode_id, fom, tom, person_ref, arbeidsgiver_ref, speil_snapshot_ref, snapshot_ref FROM vedtak").map { row ->
            Vedtak(
                UUID.fromString(row.string("vedtaksperiode_id")),
                row.localDate("fom"),
                row.localDate("tom"),
                row.long("person_ref"),
                row.long("arbeidsgiver_ref"),
                row.int("speil_snapshot_ref"),
                row.int("snapshot_ref")
            )
        }.asList)
    }

    private class Vedtak(
        private val vedtaksperiodeId: UUID,
        private val fom: LocalDate,
        private val tom: LocalDate,
        private val personRef: Long,
        private val arbeidsgiverRef: Long,
        val snapshotRef: Int,
        val graphQLSnapshotRef: Int,
    ) {
        fun assertEquals(
            forventetVedtaksperiodeId: UUID,
            forventetFom: LocalDate,
            forventetTom: LocalDate,
            forventetPersonRef: Long,
            forventetArbeidsgiverRef: Long,
            forventetSnapshotRef: Int,
            forventetGraphQLSnapshotRef: Int
        ) {
            assertEquals(forventetVedtaksperiodeId, vedtaksperiodeId)
            assertEquals(forventetFom, fom)
            assertEquals(forventetTom, tom)
            assertEquals(forventetPersonRef, personRef)
            assertEquals(forventetArbeidsgiverRef, arbeidsgiverRef)
            assertEquals(forventetSnapshotRef, snapshotRef)
            assertEquals(forventetGraphQLSnapshotRef, graphQLSnapshotRef)
        }
    }

    private fun finnSnapshot(snapshotRef: Int) = sessionOf(dataSource).use { session ->
        session.run(
            queryOf(
                "SELECT data FROM speil_snapshot WHERE id = ?",
                snapshotRef
            ).map { it.string("data") }.asSingle
        )
    }
}

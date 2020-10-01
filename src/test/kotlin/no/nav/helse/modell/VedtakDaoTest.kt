package no.nav.helse.modell

import DatabaseIntegrationTest
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class VedtakDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `lagre vedtak`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettSnapshot()
        vedtakDao.upsertVedtak(VEDTAKSPERIODE, FOM, TOM, personId, arbeidsgiverId, snapshotId)
        assertNotNull(vedtakDao.findVedtak(VEDTAKSPERIODE))
        assertEquals(1, vedtak().size)
        vedtak().first().assertEquals(VEDTAKSPERIODE, FOM, TOM, personId, arbeidsgiverId, snapshotId)
    }

    @Test
    fun `oppdatere vedtak`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettSnapshot()
        vedtakDao.upsertVedtak(VEDTAKSPERIODE, FOM, TOM, personId, arbeidsgiverId, snapshotId)
        val nyFom = LocalDate.now().minusMonths(1)
        val nyTom = LocalDate.now()
        val nySnapshotRef = snapshotDao.insertSpeilSnapshot("{}")
        vedtakDao.upsertVedtak(VEDTAKSPERIODE, nyFom, nyTom, personId, arbeidsgiverId, nySnapshotRef)
        assertEquals(1, vedtak().size)
        vedtak().first().assertEquals(VEDTAKSPERIODE, nyFom, nyTom, personId, arbeidsgiverId, nySnapshotRef)
    }

    @Test
    fun `lagrer warnings`() {
        godkjenningsbehov(HENDELSE_ID)
        val testwarnings= listOf("Warning A", "Warning B")
        vedtakDao.leggTilWarnings(HENDELSE_ID, testwarnings)
        assertWarnings(HENDELSE_ID, testwarnings)
    }

    @Test
    fun `lagrer vedtaksperiodetype hvis den er satt`() {
        godkjenningsbehov(HENDELSE_ID)
        val vedtaksperiodetype = Saksbehandleroppgavetype.FØRSTEGANGSBEHANDLING
        vedtakDao.leggTilVedtaksperiodetype(HENDELSE_ID, vedtaksperiodetype)
        assertVedtaksperiodetype(HENDELSE_ID, vedtaksperiodetype)
    }

    @Test
    fun `oppretter innslag i koblingstabellen`() {
        godkjenningsbehov(HENDELSE_ID)
        nyPerson()
        vedtakDao.opprettKobling(VEDTAKSPERIODE, HENDELSE_ID)
        assertEquals(vedtakId, finnKobling(HENDELSE_ID))
    }

    @Test
    fun `fjerner innslag i koblingstabellen`() {
        godkjenningsbehov(HENDELSE_ID)
        nyPerson()
        vedtakDao.opprettKobling(VEDTAKSPERIODE, HENDELSE_ID)
        assertEquals(vedtakId, finnKobling(HENDELSE_ID))

        vedtakDao.fjernKobling(VEDTAKSPERIODE, HENDELSE_ID)

        assertNull(finnKobling(HENDELSE_ID))
    }

    private fun assertVedtaksperiodetype(hendelseId: UUID, type: Saksbehandleroppgavetype) {
        assertEquals(type, vedtaksperiodetype(hendelseId))
    }

    @Test
    fun `fjerner vedtak`() {
        val NY_VEDTAKSPERIODE = UUID.randomUUID()
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettVedtaksperiode(NY_VEDTAKSPERIODE)
        vedtakDao.fjernVedtaksperioder(listOf(VEDTAKSPERIODE, NY_VEDTAKSPERIODE))
        assertEquals(0, vedtak().size)
    }

    private fun assertWarnings(hendelseId: UUID, warnings: List<String>) {
        assertEquals(warnings, finnWarnings(hendelseId))
    }

    private fun vedtaksperiodetype(hendelseId: UUID): Saksbehandleroppgavetype? {
        return using(sessionOf(dataSource)) {
            it.run(queryOf("SELECT type FROM saksbehandleroppgavetype WHERE hendelse_id = ?", hendelseId).map { row ->
                Saksbehandleroppgavetype.valueOf(row.string("type"))
            }.asSingle)
        }
    }

    private fun finnWarnings(hendelseId: UUID) = using(sessionOf(dataSource)) {
        it.run(
            queryOf("SELECT melding FROM warning WHERE hendelse_id = ?", hendelseId).map { row ->
                row.string("melding")
            }.asList
        )
    }

    private fun finnKobling(hendelseId: UUID) = using(sessionOf(dataSource)) {
        it.run(
            queryOf("SELECT vedtaksperiode_ref FROM vedtaksperiode_hendelse WHERE hendelse_ref = ?", hendelseId)
                .map { it.long(1)
            }.asSingle
        )
    }

    private fun vedtak() = using(sessionOf(dataSource)) {
        it.run(queryOf("SELECT vedtaksperiode_id, fom, tom, person_ref, arbeidsgiver_ref, speil_snapshot_ref FROM vedtak").map {
            Vedtak(
                UUID.fromString(it.string("vedtaksperiode_id")),
                it.localDate("fom"),
                it.localDate("tom"),
                it.int("person_ref"),
                it.int("arbeidsgiver_ref"),
                it.int("speil_snapshot_ref")
            )
        }.asList)
    }

    private class Vedtak(
        private val vedtaksperiodeId: UUID,
        private val fom: LocalDate,
        private val tom: LocalDate,
        private val personRef: Int,
        private val arbeidsgiverRef: Int,
        private val snapshotRef: Int
    ) {
        fun assertEquals(forventetVedtaksperiodeId: UUID, forventetFom: LocalDate, forventetTom: LocalDate, forventetPersonRef: Int, forventetArbeidsgiverRef: Int, forventetSnapshotRef: Int) {
            assertEquals(forventetVedtaksperiodeId, vedtaksperiodeId)
            assertEquals(forventetFom, fom)
            assertEquals(forventetTom, tom)
            assertEquals(forventetPersonRef, personRef)
            assertEquals(forventetArbeidsgiverRef, arbeidsgiverRef)
            assertEquals(forventetSnapshotRef, snapshotRef)
        }
    }
}

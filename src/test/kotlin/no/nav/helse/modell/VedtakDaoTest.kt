package no.nav.helse.modell

import AbstractEndToEndTest
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.time.LocalDate
import java.util.*

internal class VedtakDaoTest : AbstractEndToEndTest() {

    @Test
    fun `lagre vedtak`() {
        val (personRef, arbeidsgiverRef, snapshotRef) = opprettPerson()
        vedtakDao.upsertVedtak(VEDTAKSPERIODE, FOM, TOM, personRef, arbeidsgiverRef, snapshotRef.toInt())
        assertNotNull(vedtakDao.findVedtak(VEDTAKSPERIODE))
        assertEquals(1, vedtak().size)
        vedtak().first().assertEquals(VEDTAKSPERIODE, FOM, TOM, personRef, arbeidsgiverRef, snapshotRef)
    }

    @Test
    fun `oppdatere vedtak`() {
        val (personRef, arbeidsgiverRef, snapshotRef) = opprettPerson()
        vedtakDao.upsertVedtak(VEDTAKSPERIODE, FOM, TOM, personRef, arbeidsgiverRef, snapshotRef.toInt())
        val nyFom = LocalDate.now().minusMonths(1)
        val nyTom = LocalDate.now()
        val nySnapshotRef = snapshotDao.insertSpeilSnapshot("{}").toLong()
        vedtakDao.upsertVedtak(VEDTAKSPERIODE, nyFom, nyTom, personRef, arbeidsgiverRef, nySnapshotRef.toInt())
        assertEquals(1, vedtak().size)
        vedtak().first().assertEquals(VEDTAKSPERIODE, nyFom, nyTom, personRef, arbeidsgiverRef, nySnapshotRef)
    }

    @Test
    fun `lagrer warnings`() {
        testbehov(HENDELSE_ID)
        val testwarnings= listOf("Warning A", "Warning B")
        vedtakDao.leggTilWarnings(HENDELSE_ID, testwarnings)
        assertWarnings(HENDELSE_ID, testwarnings)
    }

    @Test
    fun `lagrer vedtaksperiodetype hvis den er satt`() {
        testbehov(HENDELSE_ID)
        val vedtaksperiodetype = Saksbehandleroppgavetype.FØRSTEGANGSBEHANDLING
        vedtakDao.leggTilVedtaksperiodetype(HENDELSE_ID, vedtaksperiodetype)
        assertVedtaksperiodetype(HENDELSE_ID, vedtaksperiodetype)
    }

    private fun assertVedtaksperiodetype(hendelseId: UUID, type: Saksbehandleroppgavetype) {
        assertEquals(type, vedtaksperiodetype(hendelseId))
    }

    private fun assertWarnings(hendelseId: UUID, warnings: List<String>) {
        assertEquals(warnings, finnWarnings(hendelseId))
    }

    private fun opprettPerson(): Triple<Int, Int, Long> {
        val personinfoRef = personDao.insertPersoninfo(FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN)
        val utbetalingerRef = personDao.insertInfotrygdutbetalinger(objectMapper.createObjectNode())
        val personRef = personDao.insertPerson(FNR, AKTØR, personinfoRef, ENHET.toInt(), utbetalingerRef) ?: fail { "Kunne ikke opprette person" }
        val arbeidsgiverRef = arbeidsgiverDao.insertArbeidsgiver(ORGNUMMER, ORGNAVN) ?: fail { "Kunne ikke opprette arbeidsgiver" }
        val snapshotRef = snapshotDao.insertSpeilSnapshot("{}")
        return Triple(personRef, arbeidsgiverRef, snapshotRef.toLong())
    }

    private fun vedtaksperiodetype(hendelseId: UUID): Saksbehandleroppgavetype? {
        return using(sessionOf(dataSource)) {
            it.run(queryOf("SELECT type FROM saksbehandleroppgavetype WHERE spleisbehov_ref = ?", hendelseId).map { row ->
                Saksbehandleroppgavetype.valueOf(row.string("type"))
            }.asSingle)
        }
    }

    private fun finnWarnings(hendelseId: UUID) = using(sessionOf(dataSource)) {
        it.run(
            queryOf("SELECT melding FROM warning WHERE spleisbehov_ref = ?", hendelseId).map { row ->
                row.string("melding")
            }.asList
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
                it.long("speil_snapshot_ref")
            )
        }.asList)
    }

    private class Vedtak(
        private val vedtaksperiodeId: UUID,
        private val fom: LocalDate,
        private val tom: LocalDate,
        private val personRef: Int,
        private val arbeidsgiverRef: Int,
        private val snapshotRef: Long
    ) {
        fun assertEquals(forventetVedtaksperiodeId: UUID, forventetFom: LocalDate, forventetTom: LocalDate, forventetPersonRef: Int, forventetArbeidsgiverRef: Int, forventetSnapshotRef: Long) {
            assertEquals(forventetVedtaksperiodeId, vedtaksperiodeId)
            assertEquals(forventetFom, fom)
            assertEquals(forventetTom, tom)
            assertEquals(forventetPersonRef, personRef)
            assertEquals(forventetArbeidsgiverRef, arbeidsgiverRef)
            assertEquals(forventetSnapshotRef, snapshotRef)
        }
    }
}

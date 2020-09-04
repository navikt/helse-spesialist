package no.nav.helse.modell

import AbstractEndToEndTest
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.person.Kjønn
import no.nav.helse.modell.person.PersonDao
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.time.LocalDate
import java.util.*

internal class VedtakDaoTest : AbstractEndToEndTest() {
    private companion object {
        private const val FNR = "12345678911"
        private const val AKTØR = "4321098765432"
        private const val ORGNR = "123456789"
        private const val ORGNAVN = "Bedrift AS"
        private const val FORNAVN = "KARI"
        private const val MELLOMNAVN = "Mellomnavn"
        private const val ETTERNAVN = "Nordmann"
        private const val ENHET_OSLO = "0301"
        private val FØDSELSDATO = LocalDate.EPOCH
        private val KJØNN = Kjønn.Kvinne

        private val objectMapper = jacksonObjectMapper()

        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private val FOM = LocalDate.of(2020, 1, 1)
        private val TOM = LocalDate.of(2020, 1, 31)
    }

    private lateinit var personDao: PersonDao
    private lateinit var arbeidsgiverDao: ArbeidsgiverDao
    private lateinit var snapshotDao: SnapshotDao
    private lateinit var vedtakDao: VedtakDao

    @BeforeEach
    fun setup() {
        personDao = PersonDao(dataSource)
        arbeidsgiverDao = ArbeidsgiverDao(dataSource)
        snapshotDao = SnapshotDao(dataSource)
        vedtakDao = VedtakDao(dataSource)
    }

    @Test
    fun `lagre vedtak`() {
        val (personRef, arbeidsgiverRef, snapshotRef) = opprettPerson()
        vedtakDao.upsertVedtak(VEDTAKSPERIODE_ID, FOM, TOM, personRef.toInt(), arbeidsgiverRef.toInt(), snapshotRef.toInt())
        assertNotNull(vedtakDao.findVedtak(VEDTAKSPERIODE_ID))
        assertEquals(1, vedtak().size)
        vedtak().first().assertEquals(VEDTAKSPERIODE_ID, FOM, TOM, personRef, arbeidsgiverRef, snapshotRef)
    }

    @Test
    fun `oppdatere vedtak`() {
        val (personRef, arbeidsgiverRef, snapshotRef) = opprettPerson()
        vedtakDao.upsertVedtak(VEDTAKSPERIODE_ID, FOM, TOM, personRef.toInt(), arbeidsgiverRef.toInt(), snapshotRef.toInt())
        val nyFom = LocalDate.now().minusMonths(1)
        val nyTom = LocalDate.now()
        val nySnapshotRef = snapshotDao.insertSpeilSnapshot("{}").toLong()
        vedtakDao.upsertVedtak(VEDTAKSPERIODE_ID, nyFom, nyTom, personRef.toInt(), arbeidsgiverRef.toInt(), nySnapshotRef.toInt())
        assertEquals(1, vedtak().size)
        vedtak().first().assertEquals(VEDTAKSPERIODE_ID, nyFom, nyTom, personRef, arbeidsgiverRef, nySnapshotRef)
    }

    private fun opprettPerson(): Triple<Long, Long, Long> {
        val personinfoRef = personDao.insertPersoninfo(FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN)
        val utbetalingerRef = personDao.insertInfotrygdutbetalinger(objectMapper.createObjectNode())
        val personRef = personDao.insertPerson(FNR, AKTØR, personinfoRef, ENHET_OSLO.toInt(), utbetalingerRef) ?: fail { "Kunne ikke opprette person" }
        val arbeidsgiverRef = arbeidsgiverDao.insertArbeidsgiver(ORGNR, ORGNAVN) ?: fail { "Kunne ikke opprette arbeidsgiver" }
        val snapshotRef = snapshotDao.insertSpeilSnapshot("{}")
        return Triple(personRef, arbeidsgiverRef, snapshotRef.toLong())
    }

    private fun vedtak() = using(sessionOf(dataSource)) {
        it.run(queryOf("SELECT vedtaksperiode_id, fom, tom, person_ref, arbeidsgiver_ref, speil_snapshot_ref FROM vedtak").map {
            Vedtak(
                UUID.fromString(it.string("vedtaksperiode_id")),
                it.localDate("fom"),
                it.localDate("tom"),
                it.long("person_ref"),
                it.long("arbeidsgiver_ref"),
                it.long("speil_snapshot_ref")
            )
        }.asList)
    }

    private class Vedtak(
        private val vedtaksperiodeId: UUID,
        private val fom: LocalDate,
        private val tom: LocalDate,
        private val personRef: Long,
        private val arbeidsgiverRef: Long,
        private val snapshotRef: Long
    ) {
        fun assertEquals(forventetVedtaksperiodeId: UUID, forventetFom: LocalDate, forventetTom: LocalDate, forventetPersonRef: Long, forventetArbeidsgiverRef: Long, forventetSnapshotRef: Long) {
            assertEquals(forventetVedtaksperiodeId, vedtaksperiodeId)
            assertEquals(forventetFom, fom)
            assertEquals(forventetTom, tom)
            assertEquals(forventetPersonRef, personRef)
            assertEquals(forventetArbeidsgiverRef, arbeidsgiverRef)
            assertEquals(forventetSnapshotRef, snapshotRef)
        }
    }
}

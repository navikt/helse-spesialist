package no.nav.helse.modell.kommando

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.snapshotUtenWarnings
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class OpprettVedtakCommandTest {
    private companion object {
        private const val FNR = "12345678911"
        private const val ORGNR = "123456789"
        private const val VEDTAK_REF = 1L
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private val FOM = LocalDate.of(2020, 1, 1)
        private val TOM = LocalDate.of(2020, 1, 31)
    }

    private lateinit var context: CommandContext
    private val personDao = mockk<PersonDao>(relaxed = true)
    private val arbeidsgiverDao = mockk<ArbeidsgiverDao>(relaxed = true)
    private val snapshotDao = mockk<SnapshotDao>(relaxed = true)
    private val vedtakDao = mockk<VedtakDao>(relaxed = true)
    private val warningDao = mockk<WarningDao>(relaxed = true)
    private val restClient = mockk<SpeilSnapshotRestClient>(relaxed = true)
    private val command = OpprettVedtakCommand(restClient, FNR, ORGNR, VEDTAKSPERIODE_ID, FOM, TOM, personDao, arbeidsgiverDao, snapshotDao, vedtakDao, warningDao)

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(personDao, arbeidsgiverDao, snapshotDao, vedtakDao)
    }

    @Test
    fun `opprette vedtak`() {
        every { restClient.hentSpeilSpapshot(FNR) } returns snapshotUtenWarnings(VEDTAKSPERIODE_ID)
        val (personRef, arbeidsgiverRef, snapshotRef) = personFinnes()
        every { vedtakDao.finnVedtakId(VEDTAKSPERIODE_ID) } returns null
        assertTrue(command.execute(context))
        verify(exactly = 1) { vedtakDao.opprett(VEDTAKSPERIODE_ID, FOM, TOM, personRef, arbeidsgiverRef, snapshotRef) }
    }

    @Test
    fun `oppdatere vedtak`() {
        every { restClient.hentSpeilSpapshot(FNR) } returns snapshotUtenWarnings(VEDTAKSPERIODE_ID)
        val (_, _, snapshotRef) = personFinnes()
        every { vedtakDao.finnVedtakId(VEDTAKSPERIODE_ID) } returns VEDTAK_REF
        assertTrue(command.execute(context))
        verify(exactly = 1) { vedtakDao.oppdater(VEDTAK_REF, FOM, TOM, snapshotRef) }
    }

    private fun personFinnes(): Triple<Long, Long, Int> {
        val personRef = 1L
        val arbeidsgiverRef = 2L
        val snapshotRef = 3
        every { personDao.findPersonByFødselsnummer(FNR) } returns personRef
        every { arbeidsgiverDao.findArbeidsgiverByOrgnummer(ORGNR) } returns arbeidsgiverRef
        every { snapshotDao.lagre(any(), any()) } returns snapshotRef
        return Triple(personRef, arbeidsgiverRef, snapshotRef)
    }
}

package no.nav.helse.modell.kommando

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.util.*
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.snapshot
import no.nav.helse.spesialist.api.snapshot.SnapshotClient
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
    private val snapshotClient = mockk<SnapshotClient>(relaxed = true)
    private val command = OpprettVedtakCommand(
        snapshotClient = snapshotClient,
        fødselsnummer = FNR,
        orgnummer = ORGNR,
        vedtaksperiodeId = VEDTAKSPERIODE_ID,
        periodeFom = FOM,
        periodeTom = TOM,
        personDao = personDao,
        arbeidsgiverDao = arbeidsgiverDao,
        snapshotDao = snapshotDao,
        vedtakDao = vedtakDao,
        warningDao = warningDao
    )

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(personDao, arbeidsgiverDao, snapshotDao, vedtakDao)
    }

    @Test
    fun `opprette vedtak`() {
        every { snapshotClient.hentSnapshot(FNR) } returns snapshot(
            fnr = FNR,
            aktørId = "Aktørid",
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        val (personRef, arbeidsgiverRef, snapshotRef) = personFinnes()
        every { vedtakDao.finnVedtakId(VEDTAKSPERIODE_ID) } returns null
        assertTrue(command.execute(context))
        verify(exactly = 1) {
            vedtakDao.opprett(
                vedtaksperiodeId = VEDTAKSPERIODE_ID,
                fom = FOM,
                tom = TOM,
                personRef = personRef,
                arbeidsgiverRef = arbeidsgiverRef,
                snapshotRef = snapshotRef,
            )
        }
    }

    @Test
    fun `oppdatere vedtak`() {
        every { snapshotClient.hentSnapshot(FNR) } returns snapshot(
            fnr = FNR,
            aktørId = "Aktørid",
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        val (_, _, snapshotRef) = personFinnes()
        every { vedtakDao.finnVedtakId(VEDTAKSPERIODE_ID) } returns VEDTAK_REF
        assertTrue(command.execute(context))
        verify(exactly = 1) { vedtakDao.oppdaterSnaphot(
            vedtakRef = VEDTAK_REF,
            fom = FOM,
            tom = TOM,
            snapshotRef = snapshotRef
        ) }
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

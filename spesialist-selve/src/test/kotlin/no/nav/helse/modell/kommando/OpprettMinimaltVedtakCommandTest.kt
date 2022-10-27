package no.nav.helse.modell.kommando

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.person.PersonDao
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class OpprettMinimaltVedtakCommandTest {
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
    private val command = OpprettMinimaltVedtakCommand(
        fødselsnummer = FNR,
        orgnummer = ORGNR,
        vedtaksperiodeId = VEDTAKSPERIODE_ID,
        periodeFom = FOM,
        periodeTom = TOM,
        personDao = personDao,
        arbeidsgiverDao = arbeidsgiverDao,
        vedtakDao = vedtakDao,
    )

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(personDao, arbeidsgiverDao, snapshotDao, vedtakDao)
    }

    @Test
    fun `opprette vedtak`() {
        val (personRef, arbeidsgiverRef) = personFinnes()
        every { vedtakDao.finnVedtakId(VEDTAKSPERIODE_ID) } returns null
        assertTrue(command.execute(context))
        verify(exactly = 1) {
            vedtakDao.opprett(
                vedtaksperiodeId = VEDTAKSPERIODE_ID,
                fom = FOM,
                tom = TOM,
                personRef = personRef,
                arbeidsgiverRef = arbeidsgiverRef,
                snapshotRef = null
            )
        }
    }

    private fun personFinnes(): Pair<Long, Long> {
        val personRef = 1L
        val arbeidsgiverRef = 2L
        every { personDao.findPersonByFødselsnummer(FNR) } returns personRef
        every { arbeidsgiverDao.findArbeidsgiverByOrgnummer(ORGNR) } returns arbeidsgiverRef
        return Pair(personRef, arbeidsgiverRef)
    }
}

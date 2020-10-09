package no.nav.helse.modell.dkif

import io.mockk.CapturingSlot
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.meldinger.DigitalKontaktinformasjonløsning
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.vedtak.WarningDto
import no.nav.helse.modell.vedtak.WarningKilde
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class DigitalKontaktinformasjonCommandTest {

    private companion object {
        private const val FNR = "12345678911"
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
    }

    private val dao = mockk<DigitalKontaktinformasjonDao>(relaxed = true)
    private val vedtakDao = mockk<VedtakDao>(relaxed = true)

    private val command = DigitalKontaktinformasjonCommand(dao, vedtakDao, VEDTAKSPERIODE_ID)
    private lateinit var context: CommandContext
    private var captureWarningDto = CapturingSlot<WarningDto>()


    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(dao)
    }

    @Test
    fun `Ber om digital kontaktinformasjon`() {
        assertFalse(command.execute(context))
        assertEquals(listOf("DigitalKontaktinformasjon"), context.behov().keys.toList())
    }

    @Test
    fun `Mangler løsning ved resume`() {
        assertFalse(command.resume(context))
        verify(exactly = 0) { dao.persisterDigitalKontaktinformasjon(any()) }
    }

    @Test
    fun `Lagrer løsning ved resume`() {
        context.add(DigitalKontaktinformasjonløsning(LocalDateTime.now(), FNR, true))
        assertTrue(command.resume(context))
        verify(exactly = 1) { dao.persisterDigitalKontaktinformasjon(any()) }
    }

    @Test
    fun `Lagrer ikke warning ved digital person`() {
        context.add(DigitalKontaktinformasjonløsning(LocalDateTime.now(), FNR, true))
        assertTrue(command.resume(context))
        verify(exactly = 1) { dao.persisterDigitalKontaktinformasjon(any()) }
        verify(exactly = 0) { vedtakDao.leggTilWarning(VEDTAKSPERIODE_ID, any()) }
    }

    @Test
    fun `Lagrer warning ved analog person`() {
        context.add(DigitalKontaktinformasjonløsning(LocalDateTime.now(), FNR, false))
        assertTrue(command.resume(context))
        verify(exactly = 1) { dao.persisterDigitalKontaktinformasjon(any()) }
        verify(exactly = 1) { vedtakDao.leggTilWarning(VEDTAKSPERIODE_ID, capture(captureWarningDto)) }
        assertEquals(
            "Ikke registrert eller mangler samtykke i Kontakt- og reservasjonsregisteret, eventuell kommunikasjon må skje i brevform",
            captureWarningDto.captured.melding
        )
        assertEquals(WarningKilde.Spesialist, captureWarningDto.captured.kilde)
    }
}

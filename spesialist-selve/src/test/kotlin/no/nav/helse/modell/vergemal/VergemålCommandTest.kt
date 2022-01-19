package no.nav.helse.modell.vergemal

import io.mockk.*
import no.nav.helse.mediator.meldinger.Vergemålløsning
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.behov
import no.nav.helse.modell.vedtak.Warning
import no.nav.helse.modell.vedtak.WarningKilde
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class VergemålCommandTest {

    private companion object {
        private const val FNR = "12345678911"
        private val VEDTAKSPERIODE_ID = UUID.fromString("1cd0d9cb-62e8-4f16-b634-f2b9dab550b6")
    }

    private val vergemålDao = mockk<VergemålDao>(relaxed = true)
    private val warningMock = WarningMock()
    private val forventetFullmaktWarnings = listOf(Warning("Registert fullmakt på personen.", WarningKilde.Spesialist))

    private val command = VergemålCommand(vergemålDao = vergemålDao, warningDao = warningMock.warningDao, vedtaksperiodeId = VEDTAKSPERIODE_ID)
    private lateinit var context: CommandContext

    private val ingenVergemål = Vergemål(harVergemål = false, harFremtidsfullmakter = false, harFullmakter = false)
    private val harVergemål = Vergemål(harVergemål = true, harFremtidsfullmakter = false, harFullmakter = false)
    private val harFullmakt = Vergemål(harVergemål = false, harFremtidsfullmakter = true, harFullmakter = false)
    private val harFremtidsfullmakt = Vergemål(harVergemål = false, harFremtidsfullmakter = false, harFullmakter = true)
    private val harAlt = Vergemål(harVergemål = true, harFremtidsfullmakter = true, harFullmakter = true)
    private val harBeggeFullmatkstyper = Vergemål(harVergemål = false, harFremtidsfullmakter = true, harFullmakter = true)

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(vergemålDao)
        warningMock.clear()
    }

    @Test
    fun `Toggle på - ber om informasjon om vergemål`() {
        assertFalse(command.execute(context))
        assertEquals(listOf("Vergemål"), context.behov().keys.toList())
    }

    @Test
    fun `gjør ingen behandling om vi mangler løsning ved resume`() {
        assertFalse(command.resume(context))
        verify(exactly = 0) { vergemålDao.lagre(any(), any()) }
    }

    @Test
    fun `lagrer svar på vergemål ved løsning ingen vergemål`() {
        context.add(Vergemålløsning(FNR, ingenVergemål))
        assertTrue(command.resume(context))
        verify(exactly = 1) { vergemålDao.lagre(FNR, ingenVergemål) }
        assertEquals(0, context.meldinger().size)
        assertEquals(emptyList<Warning>(), warningMock.warnings(VEDTAKSPERIODE_ID))
    }

    @Test
    fun `lagrer svar på vergemål ved løsning har vergemål`() {
        context.add(Vergemålløsning(FNR, harVergemål))
        assertTrue(command.resume(context))
        verify(exactly = 1) { vergemålDao.lagre(FNR, harVergemål) }
        assertEquals(0, context.meldinger().size)
        assertEquals(emptyList<Warning>(), warningMock.warnings(VEDTAKSPERIODE_ID))
    }

    @Test
    fun `lagrer svar på vergemål ved løsning har fullmakt`() {
        context.add(Vergemålløsning(FNR, harFullmakt))
        assertTrue(command.resume(context))
        verify(exactly = 1) { vergemålDao.lagre(FNR, harFullmakt) }
        assertEquals(0, context.meldinger().size)
        assertEquals(forventetFullmaktWarnings, warningMock.warnings(VEDTAKSPERIODE_ID))
    }

    @Test
    fun `lagrer svar på vergemål ved løsning har fremtidsfullmakt`() {
        context.add(Vergemålløsning(FNR, harFremtidsfullmakt))
        assertTrue(command.resume(context))
        verify(exactly = 1) { vergemålDao.lagre(FNR, harFremtidsfullmakt) }
        assertEquals(0, context.meldinger().size)
        assertEquals(forventetFullmaktWarnings, warningMock.warnings(VEDTAKSPERIODE_ID))
    }

    @Test
    fun `legger ikke til warning ved vergemål kombinert med fullmakt`() {
        context.add(Vergemålløsning(FNR, harAlt))
        assertTrue(command.resume(context))
        verify(exactly = 1) { vergemålDao.lagre(FNR, harAlt) }
        assertEquals(0, context.meldinger().size)
        assertEquals(emptyList<Warning>(), warningMock.warnings(VEDTAKSPERIODE_ID))
    }

    @Test
    fun `legger kun til en warning ved både fullmakt og fremtidsfullmakt`() {
        context.add(Vergemålløsning(FNR, harBeggeFullmatkstyper))
        assertTrue(command.resume(context))
        verify(exactly = 1) { vergemålDao.lagre(FNR, harBeggeFullmatkstyper) }
        assertEquals(0, context.meldinger().size)
        assertEquals(forventetFullmaktWarnings, warningMock.warnings(VEDTAKSPERIODE_ID))
    }

    private class WarningMock {
        private val warnings = mutableMapOf<UUID, MutableList<Warning>>()
        private val vedtaksperiodeIdSlot = slot<UUID>()
        private val warningSlot = slot<Warning>()
        val warningDao = mockk<WarningDao>().also {
            every { it.leggTilWarning(capture(vedtaksperiodeIdSlot), capture(warningSlot)) }.answers {
                val vedtaksperiodeId = vedtaksperiodeIdSlot.captured
                val warning = warningSlot.captured
                val warningsForVedtaksperiode = warnings[vedtaksperiodeId] ?: mutableListOf()
                warningsForVedtaksperiode.add(warning)
                warnings[vedtaksperiodeId] = warningsForVedtaksperiode
            }
        }
        fun warnings(vedtaksperiode: UUID) = warnings[vedtaksperiode]?.toList() ?: emptyList()
        fun clear() = warnings.clear()
    }
}

package no.nav.helse.modell.gosysoppgaver

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.mediator.meldinger.løsninger.ÅpneGosysOppgaverløsning
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.modell.varsel.Varselkode.SB_EX_1
import no.nav.helse.modell.varsel.Varselkode.SB_EX_3
import no.nav.helse.modell.vedtak.Warning
import no.nav.helse.modell.vedtak.WarningKilde
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.modell.vedtaksperiode.GenerasjonRepository
import no.nav.helse.modell.vedtaksperiode.IVedtaksperiodeObserver
import no.nav.helse.modell.vedtaksperiode.Vedtaksperiode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


internal class ÅpneGosysOppgaverCommandTest {

    private companion object {
        private const val FNR = "12345678911"
        private const val AKTØR_ID = "1234567891112"
        private val VEDTAKPERIODE_ID = UUID.randomUUID()
    }
    private val observer = object : IVedtaksperiodeObserver {

        val opprettedeVarsler = mutableListOf<String>()

        override fun varselOpprettet(
            vedtaksperiodeId: UUID,
            generasjonId: UUID,
            varselId: UUID,
            varselkode: String,
            opprettet: LocalDateTime
        ) {
            opprettedeVarsler.add(varselkode)
        }
    }

    private val vedtaksperiode = Vedtaksperiode(VEDTAKPERIODE_ID, generasjon(VEDTAKPERIODE_ID)).also { it.registrer(observer) }
    private val sykefraværstilfelle = Sykefraværstilfelle(FNR, 1.januar, listOf(vedtaksperiode))
    private val dao = mockk<ÅpneGosysOppgaverDao>(relaxed = true)
    private val warningDao = mockk<WarningDao>(relaxed = true)
    private val varselRepository = mockk<VarselRepository>(relaxed = true)
    private val generasjonRepository = mockk<GenerasjonRepository>(relaxed = true)
    private val command = ÅpneGosysOppgaverCommand(AKTØR_ID, dao, warningDao, varselRepository, generasjonRepository, VEDTAKPERIODE_ID, sykefraværstilfelle)
    private lateinit var context: CommandContext

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(dao)
    }

    @Test
    fun `Ber om åpne oppgaver i gosys`() {
        assertFalse(command.execute(context))
        assertEquals(listOf("ÅpneOppgaver"), context.behov().keys.toList())
    }

    @Test
    fun `Mangler løsning ved resume`() {
        assertFalse(command.resume(context))
        verify(exactly = 0) { dao.persisterÅpneGosysOppgaver(any()) }
    }

    @Test
    fun `Lagrer løsning ved resume`() {
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, 0, false))
        assertTrue(command.resume(context))
        verify(exactly = 1) { dao.persisterÅpneGosysOppgaver(any()) }
    }

    @Test
    fun `Lagrer ikke warning ved ingen åpne oppgaver`() {
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, 0, false))
        assertTrue(command.resume(context))
        verify(exactly = 1) { dao.persisterÅpneGosysOppgaver(any()) }
        verify(exactly = 0) { warningDao.leggTilWarning(VEDTAKPERIODE_ID, any()) }
        assertEquals(0, observer.opprettedeVarsler.size)
    }

    @Test
    fun `Lagrer warning ved åpne oppgaver`() {
        every { generasjonRepository.sisteFor(VEDTAKPERIODE_ID) } returns (generasjon(VEDTAKPERIODE_ID))
        val forventetWarning = Warning(
            melding = "Det finnes åpne oppgaver på sykepenger i Gosys",
            kilde = WarningKilde.Spesialist,
            opprettet = LocalDateTime.now()
        )
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, 1, false))
        assertTrue(command.resume(context))
        verify(exactly = 1) { dao.persisterÅpneGosysOppgaver(any()) }
        verify(exactly = 1) { warningDao.leggTilWarning(VEDTAKPERIODE_ID, forventetWarning) }
        assertEquals(1, observer.opprettedeVarsler.size)
        assertEquals(SB_EX_1.name, observer.opprettedeVarsler[0])
    }

    @Test
    fun `Lagrer warning ved oppslag feilet`() {
        every { generasjonRepository.sisteFor(VEDTAKPERIODE_ID) } returns (generasjon(VEDTAKPERIODE_ID))
        val forventetWarning = Warning(
            melding = "Kunne ikke sjekke åpne oppgaver på sykepenger i Gosys",
            kilde = WarningKilde.Spesialist,
            opprettet = LocalDateTime.now()
        )
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, null, true))
        assertTrue(command.resume(context))
        verify(exactly = 1) { dao.persisterÅpneGosysOppgaver(any()) }
        verify(exactly = 1) { warningDao.leggTilWarning(VEDTAKPERIODE_ID, forventetWarning) }
        assertEquals(1, observer.opprettedeVarsler.size)
        assertEquals(SB_EX_3.name, observer.opprettedeVarsler[0])
    }

    private fun generasjon(vedtaksperiodeId: UUID = UUID.randomUUID()) = Generasjon(
        id = UUID.randomUUID(),
        vedtaksperiodeId = vedtaksperiodeId,
        fom = 1.januar,
        tom = 31.januar,
        skjæringstidspunkt = 1.januar
    )
}

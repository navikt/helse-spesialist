package no.nav.helse.modell.gosysoppgaver

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.mediator.meldinger.løsninger.ÅpneGosysOppgaverløsning
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.varsel.Varselkode.SB_EX_1
import no.nav.helse.modell.varsel.Varselkode.SB_EX_3
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.modell.vedtaksperiode.IVedtaksperiodeObserver
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
            varselId: UUID,
            vedtaksperiodeId: UUID,
            generasjonId: UUID,
            varselkode: String,
            opprettet: LocalDateTime
        ) {
            opprettedeVarsler.add(varselkode)
        }
    }

    private val generasjon = generasjon(VEDTAKPERIODE_ID).also { it.registrer(observer) }
    private val sykefraværstilfelle = Sykefraværstilfelle(FNR, 1.januar, listOf(generasjon))
    private val dao = mockk<ÅpneGosysOppgaverDao>(relaxed = true)
    private val command = ÅpneGosysOppgaverCommand(
        UUID.randomUUID(),
        AKTØR_ID,
        dao,
        VEDTAKPERIODE_ID,
        sykefraværstilfelle
    )
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
        assertEquals(0, observer.opprettedeVarsler.size)
    }

    @Test
    fun `Lagrer warning ved åpne oppgaver`() {
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, 1, false))
        assertTrue(command.resume(context))
        verify(exactly = 1) { dao.persisterÅpneGosysOppgaver(any()) }
        assertEquals(1, observer.opprettedeVarsler.size)
        assertEquals(SB_EX_1.name, observer.opprettedeVarsler[0])
    }

    @Test
    fun `Lagrer warning ved oppslag feilet`() {
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, null, true))
        assertTrue(command.resume(context))
        verify(exactly = 1) { dao.persisterÅpneGosysOppgaver(any()) }
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

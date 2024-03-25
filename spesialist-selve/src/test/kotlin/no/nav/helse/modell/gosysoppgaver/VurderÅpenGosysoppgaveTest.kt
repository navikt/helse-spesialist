package no.nav.helse.modell.gosysoppgaver

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.mediator.meldinger.løsninger.ÅpneGosysOppgaverløsning
import no.nav.helse.mediator.oppgave.OppgaveMediator
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.varsel.Varselkode
import no.nav.helse.modell.varsel.Varselkode.SB_EX_1
import no.nav.helse.modell.varsel.Varselkode.SB_EX_3
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.modell.vedtaksperiode.IVedtaksperiodeObserver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


internal class VurderÅpenGosysoppgaveTest {

    private companion object {
        private const val FNR = "12345678911"
        private const val AKTØR_ID = "1234567891112"
        private val VEDTAKPERIODE_ID = UUID.randomUUID()
    }
    private val vedtaksperiodeObserver = object : IVedtaksperiodeObserver {
        val deaktiverteVarsler = mutableListOf<Varselkode>()
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

        override fun varselDeaktivert(varselId: UUID, varselkode: String, generasjonId: UUID, vedtaksperiodeId: UUID) {
            deaktiverteVarsler.add(Varselkode.valueOf(varselkode))
        }

        fun reset() {
            deaktiverteVarsler.clear()
            opprettedeVarsler.clear()
        }
    }

    private val generasjon = generasjon(VEDTAKPERIODE_ID).also { it.registrer(vedtaksperiodeObserver) }
    private val sykefraværstilfelle = Sykefraværstilfelle(FNR, 1.januar, listOf(generasjon), emptyList())
    private val dao = mockk<ÅpneGosysOppgaverDao>(relaxed = true)

    private fun command(harTildeltOppgave: Boolean = false, skjæringstidspunkt: LocalDate = LocalDate.now()) = VurderÅpenGosysoppgave(
        UUID.randomUUID(),
        AKTØR_ID,
        dao,
        mockk<OppgaveMediator> { every { førsteOppgavedato(VEDTAKPERIODE_ID) } returns skjæringstidspunkt},
        VEDTAKPERIODE_ID,
        sykefraværstilfelle,
        harTildeltOppgave = harTildeltOppgave,
    )
    private lateinit var context: CommandContext

    private val observer = object : CommandContextObserver {
        val behov = mutableMapOf<String, Map<String, Any>>()
        override fun behov(behov: String, ekstraKontekst: Map<String, Any>, detaljer: Map<String, Any>) {
            this.behov[behov] = detaljer
        }

        override fun hendelse(hendelse: String) {}
    }

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        context.nyObserver(observer)
        clearMocks(dao)
    }

    @Test
    fun `Ber om åpne oppgaver i gosys`() {
        val skjæringstidspunkt = LocalDate.now().minusDays(17)
        assertFalse(command(skjæringstidspunkt = skjæringstidspunkt).execute(context))
        assertEquals(listOf("ÅpneOppgaver"), observer.behov.keys.toList())
        assertEquals(skjæringstidspunkt.minusYears(1), observer.behov["ÅpneOppgaver"]!!["ikkeEldreEnn"])
    }

    @Test
    fun `Mangler løsning ved resume`() {
        assertFalse(command().resume(context))
        verify(exactly = 0) { dao.persisterÅpneGosysOppgaver(any()) }
    }

    @Test
    fun `Lagrer løsning ved resume`() {
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, 0, false))
        assertTrue(command().resume(context))
        verify(exactly = 1) { dao.persisterÅpneGosysOppgaver(any()) }
    }

    @Test
    fun `Lagrer ikke varsel ved ingen åpne oppgaver og deaktiverer eventuelt eksisterende varsel`() {
        generasjon.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), VEDTAKPERIODE_ID), UUID.randomUUID())
        assertEquals(1, vedtaksperiodeObserver.opprettedeVarsler.size)
        vedtaksperiodeObserver.reset()
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, 0, false))
        assertTrue(command().resume(context))
        verify(exactly = 1) { dao.persisterÅpneGosysOppgaver(any()) }
        assertEquals(0, vedtaksperiodeObserver.opprettedeVarsler.size)
        assertTrue(vedtaksperiodeObserver.deaktiverteVarsler.contains(SB_EX_1))
    }

    @Test
    fun `Deaktiverer ikke varsel dersom oppgave er tildelt`() {
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, 0, false))
        assertTrue(command(harTildeltOppgave = true).resume(context))
        assertFalse(vedtaksperiodeObserver.deaktiverteVarsler.contains(SB_EX_1))
    }

    @Test
    fun `Lagrer varsel ved åpne oppgaver, uavhengig om eventuell oppgave er tildelt eller ikke`() {
        lagrerVarselVedÅpneOppgaver(false)
        setup()
        lagrerVarselVedÅpneOppgaver(true)
    }

    @Test
    fun `Lagrer varsel ved oppslag feilet`() {
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, null, true))
        assertTrue(command().resume(context))
        verify(exactly = 1) { dao.persisterÅpneGosysOppgaver(any()) }
        assertEquals(1, vedtaksperiodeObserver.opprettedeVarsler.size)
        assertEquals(SB_EX_3.name, vedtaksperiodeObserver.opprettedeVarsler[0])
    }

    private fun lagrerVarselVedÅpneOppgaver(harTildeltOppgave: Boolean) {
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, 1, false))
        assertTrue(command(harTildeltOppgave).resume(context))
        verify(exactly = 1) { dao.persisterÅpneGosysOppgaver(any()) }
        assertEquals(1, vedtaksperiodeObserver.opprettedeVarsler.size)
        assertEquals(SB_EX_1.name, vedtaksperiodeObserver.opprettedeVarsler[0])
    }

    private fun generasjon(vedtaksperiodeId: UUID = UUID.randomUUID()) = Generasjon(
        id = UUID.randomUUID(),
        vedtaksperiodeId = vedtaksperiodeId,
        fom = 1.januar,
        tom = 31.januar,
        skjæringstidspunkt = 1.januar
    )
}

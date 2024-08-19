package no.nav.helse.modell.gosysoppgaver

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.januar
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.mediator.meldinger.løsninger.ÅpneGosysOppgaverløsning
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.person.vedtaksperiode.Varsel
import no.nav.helse.modell.person.vedtaksperiode.VarselStatusDto
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.modell.vedtaksperiode.GenerasjonDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class VurderÅpenGosysoppgaveTest {
    private companion object {
        private const val FNR = "12345678911"
        private const val AKTØR_ID = "1234567891112"
        private val VEDTAKPERIODE_ID_AG_1 = UUID.randomUUID()
        private val VEDTAKPERIODE_ID_AG_2 = UUID.randomUUID()
    }

    private val generasjonAg1 = generasjon(VEDTAKPERIODE_ID_AG_1)
    private val generasjonAg2 = generasjon(VEDTAKPERIODE_ID_AG_2)
    private val skjæringstidspunkt = LocalDate.now().minusDays(17)
    private val sykefraværstilfelle = Sykefraværstilfelle(FNR, skjæringstidspunkt, listOf(generasjonAg1, generasjonAg2), emptyList())
    private val dao = mockk<ÅpneGosysOppgaverDao>(relaxed = true)
    private val oppgaveService = mockk<OppgaveService>(relaxed = true)

    private fun command(
        harTildeltOppgave: Boolean = false,
    ) = VurderÅpenGosysoppgave(
        UUID.randomUUID(),
        AKTØR_ID,
        dao,
        VEDTAKPERIODE_ID_AG_1,
        sykefraværstilfelle,
        harTildeltOppgave = harTildeltOppgave,
        oppgaveService = oppgaveService,
    )

    private lateinit var context: CommandContext

    private val observer =
        object : CommandContextObserver {
            val behov = mutableMapOf<String, Map<String, Any>>()

            override fun behov(
                behov: String,
                ekstraKontekst: Map<String, Any>,
                detaljer: Map<String, Any>,
            ) {
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
        assertFalse(command().execute(context))
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
        generasjonAg1.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), VEDTAKPERIODE_ID_AG_1), UUID.randomUUID())
        generasjonAg1.inspektør {
            assertEquals(1, varsler.size)
        }
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, 0, false))
        assertTrue(command().resume(context))
        verify(exactly = 1) { dao.persisterÅpneGosysOppgaver(any()) }
        generasjonAg1.inspektør {
            assertEquals(1, varsler.size)
            assertEquals("SB_EX_1", varsler.first().varselkode)
            assertEquals(VarselStatusDto.INAKTIV, varsler.first().status)
        }
        verify(exactly = 1) { oppgaveService.fjernGosysEgenskap(any()) }
    }

    @Test
    fun `Deaktiverer ikke varsel dersom oppgave er tildelt`() {
        generasjonAg1.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), VEDTAKPERIODE_ID_AG_1), UUID.randomUUID())
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, 0, false))
        assertTrue(command(harTildeltOppgave = true).resume(context))
        generasjonAg1.inspektør {
            assertEquals(1, varsler.size)
            assertEquals("SB_EX_1", varsler.first().varselkode)
            assertEquals(VarselStatusDto.AKTIV, varsler.first().status)
        }
    }

    @Test
    fun `Lagrer varsel ved åpne oppgaver, uavhengig om eventuell oppgave er tildelt eller ikke`() {
        lagrerVarselVedÅpneOppgaver(false)
        verify(exactly = 1) { oppgaveService.leggTilGosysEgenskap(any()) }
        setup()
        lagrerVarselVedÅpneOppgaver(true)
    }

    @Test
    fun `Lagrer varsel ved oppslag feilet`() {
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, null, true))
        assertTrue(command().resume(context))
        verify(exactly = 1) { dao.persisterÅpneGosysOppgaver(any()) }
        generasjonAg1.inspektør {
            assertEquals(1, varsler.size)
            assertEquals("SB_EX_3", varsler.first().varselkode)
        }
    }

    @Test
    fun `Legger ikke til egenskap for gosys dersom det er andre varsler på perioden`() {
        generasjonAg1.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_4", LocalDateTime.now(), VEDTAKPERIODE_ID_AG_1), UUID.randomUUID())
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, 1, false))
        command().resume(context)
        verify(exactly = 0) { oppgaveService.leggTilGosysEgenskap(any()) }
    }

    @Test
    fun `Legger ikke til egenskap for gosys dersom det er andre varsler på andre overlappende perioder`() {
        generasjonAg2.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_4", LocalDateTime.now(), VEDTAKPERIODE_ID_AG_2), UUID.randomUUID())
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, 1, false))
        command().resume(context)
        verify(exactly = 0) { oppgaveService.leggTilGosysEgenskap(any()) }
    }

    private fun lagrerVarselVedÅpneOppgaver(harTildeltOppgave: Boolean) {
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, 1, false))
        assertTrue(command(harTildeltOppgave).resume(context))
        verify(exactly = 1) { dao.persisterÅpneGosysOppgaver(any()) }
        generasjonAg1.inspektør {
            assertEquals(1, varsler.size)
            assertEquals("SB_EX_1", varsler.first().varselkode)
        }
    }

    private fun generasjon(vedtaksperiodeId: UUID = UUID.randomUUID()) =
        Generasjon(
            id = UUID.randomUUID(),
            vedtaksperiodeId = vedtaksperiodeId,
            fom = 1.januar,
            tom = 31.januar,
            skjæringstidspunkt = 1.januar,
        )

}

internal fun Generasjon.inspektør(block: GenerasjonDto.() -> Unit) {
    this.toDto().block()
}

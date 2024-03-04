package no.nav.helse.modell.egenansatt

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.mediator.UtgåendeMeldingerObserver
import no.nav.helse.mediator.meldinger.løsninger.EgenAnsattløsning
import no.nav.helse.modell.kommando.CommandContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class KontrollerEgenAnsattstatusTest {
    private companion object {
        private const val FNR = "12345678911"
    }

    private val dao = mockk<EgenAnsattDao>(relaxed = true)

    private val command = KontrollerEgenAnsattstatus(FNR, dao)
    private lateinit var context: CommandContext

    private val observer = object : UtgåendeMeldingerObserver {
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
    fun `ber om informasjon om egen ansatt`() {
        every { dao.erEgenAnsatt(any()) } returns null
        assertFalse(command.execute(context))
        assertEquals(listOf("EgenAnsatt"), observer.behov.keys.toList())
    }

    @Test
    fun `mangler løsning ved resume`() {
        every { dao.erEgenAnsatt(any()) } returns null
        assertFalse(command.resume(context))
        verify(exactly = 0) { dao.lagre(any(), any(), any()) }
    }

    @Test
    fun `lagrer løsning ved resume`() {
        every { dao.erEgenAnsatt(any()) } returns null
        context.add(EgenAnsattløsning(LocalDateTime.now(), FNR, false))
        assertTrue(command.resume(context))
        verify(exactly = 1) { dao.lagre(FNR, false, any()) }
    }

    @Test
    fun `sender ikke behov om informasjonen finnes`() {
        every { dao.erEgenAnsatt(any()) } returns false
        assertTrue(command.resume(context))
        assertEquals(emptyList<String>(), observer.behov.keys.toList())

        every { dao.erEgenAnsatt(any()) } returns true
        assertTrue(command.resume(context))
        assertEquals(emptyList<String>(), observer.behov.keys.toList())
    }
}

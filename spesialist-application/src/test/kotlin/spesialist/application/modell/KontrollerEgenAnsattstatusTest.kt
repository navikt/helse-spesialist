package no.nav.helse.spesialist.application.modell

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.mediator.meldinger.løsninger.EgenAnsattløsning
import no.nav.helse.modell.egenansatt.KontrollerEgenAnsattstatus
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.melding.Behov
import no.nav.helse.spesialist.application.PersonRepository
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class KontrollerEgenAnsattstatusTest {
    private companion object {
        private const val FNR = "12345678911"
    }

    private val personRepository = mockk<PersonRepository>(relaxed = true)

    private val command = KontrollerEgenAnsattstatus(FNR, personRepository)
    private lateinit var context: CommandContext

    private val observer =
        object : CommandContextObserver {
            val behov = mutableListOf<Behov>()

            override fun behov(
                behov: Behov,
                commandContextId: UUID,
                sti: List<Int>,
            ) {
                this.behov.add(behov)
            }
        }

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        context.nyObserver(observer)
        clearMocks(personRepository)
    }

    @Test
    fun `ber om informasjon om egen ansatt`() {
        every { personRepository.finn(any()) } returns lagPerson(erEgenAnsatt = null)
        assertFalse(command.execute(context))
        assertEquals(listOf(Behov.EgenAnsatt), observer.behov.toList())
    }

    @Test
    fun `mangler løsning ved resume`() {
        every { personRepository.finn(any()) } returns lagPerson(erEgenAnsatt = null)
        assertFalse(command.resume(context))
        verify(exactly = 0) { personRepository.lagre(any()) }
    }

    @Test
    fun `lagrer løsning ved resume`() {
        every { personRepository.finn(any()) } returns lagPerson(erEgenAnsatt = null)
        context.add(EgenAnsattløsning(LocalDateTime.now(), FNR, false))
        assertTrue(command.resume(context))
        verify(exactly = 1) { personRepository.lagre(any()) }
    }

    @Test
    fun `sender ikke behov om informasjonen finnes`() {
        every { personRepository.finn(any()) } returns lagPerson(erEgenAnsatt = false)
        assertTrue(command.resume(context))
        assertEquals(emptyList<Behov>(), observer.behov.toList())

        every { personRepository.finn(any()) } returns lagPerson(erEgenAnsatt = true)
        assertTrue(command.resume(context))
        assertEquals(emptyList<Behov>(), observer.behov.toList())
    }
}

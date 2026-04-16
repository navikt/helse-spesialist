package no.nav.helse.spesialist.application.kommando

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
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class KontrollerEgenAnsattstatusTest : ApplicationTest() {
    private companion object {
        private const val FNR = "12345678911"
    }

    private val personRepository = mockk<PersonRepository>(relaxed = true)

    private val command = KontrollerEgenAnsattstatus(FNR, personRepository)
    private lateinit var commandContext: CommandContext

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
        commandContext = CommandContext(UUID.randomUUID())
        commandContext.nyObserver(observer)
        clearMocks(personRepository)
    }

    @Test
    fun `ber om informasjon om egen ansatt`() {
        every { personRepository.finn(any()) } returns lagPerson(erEgenAnsatt = null)
        Assertions.assertFalse(command.execute(commandContext, sessionContext, outbox))
        Assertions.assertEquals(listOf(Behov.EgenAnsatt), observer.behov.toList())
    }

    @Test
    fun `mangler løsning ved resume`() {
        every { personRepository.finn(any()) } returns lagPerson(erEgenAnsatt = null)
        Assertions.assertFalse(command.resume(commandContext, sessionContext, outbox))
        verify(exactly = 0) { personRepository.lagre(any()) }
    }

    @Test
    fun `lagrer løsning ved resume`() {
        every { personRepository.finn(any()) } returns lagPerson(erEgenAnsatt = null)
        commandContext.add(EgenAnsattløsning(LocalDateTime.now(), FNR, false))
        Assertions.assertTrue(command.resume(commandContext, sessionContext, outbox))
        verify(exactly = 1) { personRepository.lagre(any()) }
    }

    @Test
    fun `sender ikke behov om informasjonen finnes`() {
        every { personRepository.finn(any()) } returns lagPerson(erEgenAnsatt = false)
        Assertions.assertTrue(command.resume(commandContext, sessionContext, outbox))
        Assertions.assertEquals(emptyList<Behov>(), observer.behov.toList())

        every { personRepository.finn(any()) } returns lagPerson(erEgenAnsatt = true)
        Assertions.assertTrue(command.resume(commandContext, sessionContext, outbox))
        Assertions.assertEquals(emptyList<Behov>(), observer.behov.toList())
    }
}

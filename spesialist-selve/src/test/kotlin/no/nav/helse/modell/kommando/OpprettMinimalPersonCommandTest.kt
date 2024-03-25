package no.nav.helse.modell.kommando

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.modell.person.PersonDao
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class OpprettMinimalPersonCommandTest {
    private companion object {
        private const val FNR = "12345678911"
        private const val AKTØR = "4321098765432"
    }

    private val personDao = mockk<PersonDao>(relaxed = true)
    private val command = OpprettMinimalPersonCommand(FNR, AKTØR, personDao)
    private lateinit var context: CommandContext

    private val observer = object : CommandContextObserver {
        val behov = mutableListOf<String>()
        override fun behov(behov: String, ekstraKontekst: Map<String, Any>, detaljer: Map<String, Any>) {
            this.behov.add(behov)
        }

        override fun hendelse(hendelse: String) {}
    }

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        context.nyObserver(observer)
        clearMocks(personDao)
    }

    @Test
    fun `oppretter ikke person når person finnes fra før`() {
        personFinnes()
        assertTrue(command.execute(context))
        verify(exactly = 0) { personDao.insertPerson(FNR, AKTØR) }
    }

    @Test
    fun `oppretter person`() {
        personFinnesIkke()
        assertTrue(command.execute(context))
        assertTrue(observer.behov.isEmpty())

        verify(exactly = 1) { personDao.insertPerson(FNR, AKTØR) }
        personFinnes()
    }

    private fun personFinnes() {
        every { personDao.findPersonByFødselsnummer(FNR) } returns 1
    }
    private fun personFinnesIkke() {
        every { personDao.findPersonByFødselsnummer(FNR) } returns null
    }

}

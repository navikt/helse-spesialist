package no.nav.helse.modell.kommando

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.modell.person.PersonDao
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class OpprettMinimalPersonCommandTest {
    private companion object {
        private const val FNR = "12345678911"
        private const val AKTØR = "4321098765432"
    }

    private val dao = mockk<PersonDao>(relaxed = true)
    private val command = OpprettMinimalPersonCommand(FNR, AKTØR, dao)
    private lateinit var context: CommandContext

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(dao)
    }

    @Test
    fun `oppretter ikke person når person finnes fra før`() {
        personFinnes()
        assertTrue(command.execute(context))
        verify(exactly = 0) { dao.insertPerson(FNR, AKTØR) }
    }


    @Test
    fun `oppretter person`() {
        personFinnesIkke()
        assertTrue(command.execute(context))
        assertFalse(context.harBehov())

        verify(exactly = 1) { dao.insertPerson(FNR, AKTØR) }
        personFinnes()
    }

    private fun personFinnes() {
        every { dao.findPersonByFødselsnummer(FNR) } returns 1
    }
    private fun personFinnesIkke() {
        every { dao.findPersonByFødselsnummer(FNR) } returns null
    }

}

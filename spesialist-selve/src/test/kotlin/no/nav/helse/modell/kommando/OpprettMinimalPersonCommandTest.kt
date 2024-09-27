package no.nav.helse.modell.kommando

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.helse.db.PersonRepository
import no.nav.helse.spesialist.test.lagAktørId
import no.nav.helse.spesialist.test.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class OpprettMinimalPersonCommandTest {
    private lateinit var context: CommandContext
    private val lagredePersoner = mutableListOf<MinimalPersonDto>()
    private val personRepository = mockk<PersonRepository>(relaxed = true)

    @BeforeEach
    fun setup() {
        lagredePersoner.clear()
        context = CommandContext(UUID.randomUUID())
    }

    @Test
    fun `oppretter ikke person når person finnes fra før`() {
        val aktørId = lagAktørId()
        val fødselsnummer = lagFødselsnummer()
        val command = OpprettMinimalPersonCommand(fødselsnummer, aktørId, personRepository)

        every { personRepository.lagreMinimalPerson(capture(lagredePersoner)) } just runs
        every { personRepository.finnMinimalPerson(any()) } returns null
        every { personRepository.finnMinimalPerson(fødselsnummer) } returns MinimalPersonDto(fødselsnummer, aktørId)
        assertTrue(command.execute(context))
        assertEquals(0, lagredePersoner.size)
    }

    @Test
    fun `oppretter person`() {
        every { personRepository.lagreMinimalPerson(capture(lagredePersoner)) } just runs
        every { personRepository.finnMinimalPerson(any()) } returns null
        val fødselsnummer = lagFødselsnummer()
        val aktørId = lagAktørId()
        val command = OpprettMinimalPersonCommand(fødselsnummer, aktørId, personRepository)

        assertTrue(command.execute(context))
        assertEquals(1, lagredePersoner.size)
        assertEquals(MinimalPersonDto(fødselsnummer, aktørId), lagredePersoner.single())
    }
}

package no.nav.helse.spesialist.application.kommando

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.helse.db.PersonDao
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.MinimalPersonDto
import no.nav.helse.modell.kommando.OpprettMinimalPersonCommand
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagAktørId
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class OpprettMinimalPersonCommandTest {
    private lateinit var context: CommandContext
    private val lagredePersoner = mutableListOf<MinimalPersonDto>()
    private val personDao = mockk<PersonDao>(relaxed = true)

    @BeforeEach
    fun setup() {
        lagredePersoner.clear()
        context = CommandContext(UUID.randomUUID())
    }

    @Test
    fun `oppretter ikke person når person finnes fra før`() {
        val aktørId = lagAktørId()
        val fødselsnummer = lagFødselsnummer()
        val command = OpprettMinimalPersonCommand(fødselsnummer, aktørId, personDao)

        every { personDao.lagreMinimalPerson(capture(lagredePersoner)) } just runs
        every { personDao.finnMinimalPerson(any()) } returns null
        every { personDao.finnMinimalPerson(fødselsnummer) } returns MinimalPersonDto(fødselsnummer, aktørId)
        assertTrue(command.execute(context))
        assertEquals(0, lagredePersoner.size)
    }

    @Test
    fun `oppretter person`() {
        every { personDao.lagreMinimalPerson(capture(lagredePersoner)) } just runs
        every { personDao.finnMinimalPerson(any()) } returns null
        val fødselsnummer = lagFødselsnummer()
        val aktørId = lagAktørId()
        val command = OpprettMinimalPersonCommand(fødselsnummer, aktørId, personDao)

        assertTrue(command.execute(context))
        assertEquals(1, lagredePersoner.size)
        assertEquals(MinimalPersonDto(fødselsnummer, aktørId), lagredePersoner.single())
    }
}

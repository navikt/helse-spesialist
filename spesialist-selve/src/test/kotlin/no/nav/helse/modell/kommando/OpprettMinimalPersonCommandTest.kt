package no.nav.helse.modell.kommando

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

    @BeforeEach
    fun setup() {
        lagredePersoner.clear()
        context = CommandContext(UUID.randomUUID())
    }

    @Test
    fun `oppretter ikke person når person finnes fra før`() {
        val fødselsnummer = lagFødselsnummer()
        val aktørId = lagAktørId()
        val command = lagCommand(fødselsnummer, aktørId, MinimalPersonDto(fødselsnummer, aktørId))

        assertTrue(command.execute(context))
        assertEquals(0, lagredePersoner.size)
    }

    @Test
    fun `oppretter person`() {
        val fødselsnummer = lagFødselsnummer()
        val aktørId = lagAktørId()
        val command = lagCommand(fødselsnummer, aktørId, null)

        assertTrue(command.execute(context))
        assertEquals(1, lagredePersoner.size)
        assertEquals(MinimalPersonDto(fødselsnummer, aktørId), lagredePersoner.single())
    }

    private fun lagCommand(
        fødselsnummer: String = lagFødselsnummer(),
        aktørId: String = lagAktørId(),
        minimalPersonDto: MinimalPersonDto?,
    ): OpprettMinimalPersonCommand {
        val repository =
            object : PersonRepository {
                override fun finnMinimalPerson(fødselsnummer: String): MinimalPersonDto? = minimalPersonDto

                override fun lagreMinimalPerson(minimalPerson: MinimalPersonDto) {
                    lagredePersoner.add(minimalPerson)
                }
            }
        return OpprettMinimalPersonCommand(fødselsnummer, aktørId, repository)
    }
}

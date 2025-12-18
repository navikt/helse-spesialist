package no.nav.helse.spesialist.application.kommando

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.PersonDao
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.OppdaterPersonCommand
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.person.HentInfotrygdutbetalingerløsning
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class OppdaterPersonCommandTest {
    private val personDao = mockk<PersonDao>(relaxed = true)

    val førsteKjenteDagFinner = { LocalDate.now() }

    private fun command(identitetsnummer: Identitetsnummer) = OppdaterPersonCommand(identitetsnummer.value, førsteKjenteDagFinner, personDao)

    private lateinit var context: CommandContext

    private val observer =
        object : CommandContextObserver {
            val behov = mutableListOf<Behov>()

            override fun behov(
                behov: Behov,
                commandContextId: UUID,
            ) {
                this.behov.add(behov)
            }
        }

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        context.nyObserver(observer)
        clearMocks(personDao)
    }

    @Test
    fun `oppdaterer ingenting når informasjonen er ny nok`() =
        testMedSessionContext {
            val person = lagPerson().also(it.personRepository::lagre)
            every { personDao.finnITUtbetalingsperioderSistOppdatert(person.id.value) } returns LocalDate.now()
            assertTrue(command(person.id).execute(context, it))
            verify(exactly = 0) { personDao.upsertInfotrygdutbetalinger(any(), any()) }
        }

    @Test
    fun `trenger infotrygdutbetalinger`() =
        testMedSessionContext {
            val person = lagPerson().also(it.personRepository::lagre)
            utdatertUtbetalinger(person.id)
            assertFalse(command(person.id).execute(context, it))
            assertTrue(observer.behov.isNotEmpty())
            assertEquals(
                listOf(
                    Behov.Infotrygdutbetalinger(
                        fom = førsteKjenteDagFinner().minusYears(3),
                        tom = LocalDate.now(),
                    ),
                ),
                observer.behov.toList(),
            )
        }

    @Test
    fun `oppdatere infotrygdutbetalinger`() =
        testMedSessionContext {
            val person = lagPerson().also(it.personRepository::lagre)
            utdatertUtbetalinger(person.id)
            val løsning = mockk<HentInfotrygdutbetalingerløsning>(relaxed = true)
            context.add(løsning)
            assertTrue(command(person.id).execute(context, it))
            verify(exactly = 1) { løsning.oppdater(personDao, person.id.value) }
        }

    private fun utdatertUtbetalinger(identitetsnummer: Identitetsnummer) {
        every { personDao.finnITUtbetalingsperioderSistOppdatert(identitetsnummer.value) } returns LocalDate.now().minusYears(1)
    }
}

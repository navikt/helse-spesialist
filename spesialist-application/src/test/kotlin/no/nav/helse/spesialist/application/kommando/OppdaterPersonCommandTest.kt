package no.nav.helse.spesialist.application.kommando

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.OppdaterPersonCommand
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.person.HentInfotrygdutbetalingerløsning
import no.nav.helse.spesialist.application.InMemoryInfotrygdutbetalingerRepository
import no.nav.helse.spesialist.application.InMemoryPersonRepository
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.InfotrygdUtbetalinger
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class OppdaterPersonCommandTest {
    private val personRepository = InMemoryPersonRepository()
    private val infotrygdutbetalingerRepository = InMemoryInfotrygdutbetalingerRepository()

    val førsteKjenteDagFinner = { LocalDate.now() }

    private fun command(identitetsnummer: Identitetsnummer) =
        OppdaterPersonCommand(identitetsnummer.value, førsteKjenteDagFinner, personRepository, infotrygdutbetalingerRepository)

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
    }

    @Test
    fun `oppdaterer ingenting når informasjonen er ny nok`() {
        val person = lagPerson().also(personRepository::lagre)
        initLagredeInfotrygdUtbetalinger(oppdatert = LocalDate.now(), identitetsnummer = person.id)
        assertTrue(command(person.id).execute(context))
        assertTrue(observer.behov.isEmpty())
    }

    @Test
    fun `trenger infotrygdutbetalinger`() {
        val person = lagPerson().also(personRepository::lagre)
        initLagredeInfotrygdUtbetalinger(oppdatert = LocalDate.now().minusYears(1), identitetsnummer = person.id)
        assertFalse(command(person.id).execute(context))
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
    fun `oppdatere infotrygdutbetalinger`() {
        val person = lagPerson().also(personRepository::lagre)
        initLagredeInfotrygdUtbetalinger(oppdatert = LocalDate.now().minusYears(1), identitetsnummer = person.id)
        val løsning = mockk<HentInfotrygdutbetalingerløsning>(relaxed = true)
        context.add(løsning)
        assertTrue(command(person.id).execute(context))
        verify(exactly = 1) { løsning.oppdater(infotrygdutbetalingerRepository, person.id.value) }
    }

    private fun initLagredeInfotrygdUtbetalinger(oppdatert: LocalDate, identitetsnummer: Identitetsnummer) {
        infotrygdutbetalingerRepository.lagre(
            InfotrygdUtbetalinger.Factory.fraLagring(identitetsnummer, "[]", oppdatert),
        )
    }
}

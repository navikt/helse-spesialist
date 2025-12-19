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
import no.nav.helse.modell.person.HentEnhetløsning
import no.nav.helse.modell.person.HentInfotrygdutbetalingerløsning
import no.nav.helse.spesialist.application.InMemoryPersonRepository
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
    private val personRepository = InMemoryPersonRepository()

    val førsteKjenteDagFinner = { LocalDate.now() }

    private fun command(identitetsnummer: Identitetsnummer) = OppdaterPersonCommand(identitetsnummer.value, førsteKjenteDagFinner, personDao, personRepository)

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
    fun `oppdaterer ingenting når informasjonen er ny nok`() {
        val person = lagPerson().also(personRepository::lagre)
        every { personDao.finnEnhetSistOppdatert(person.id.value) } returns LocalDate.now()
        every { personDao.finnITUtbetalingsperioderSistOppdatert(person.id.value) } returns LocalDate.now()
        assertTrue(command(person.id).execute(context))
        verify(exactly = 0) { personDao.oppdaterEnhet(any(), any()) }
        verify(exactly = 0) { personDao.upsertInfotrygdutbetalinger(any(), any()) }
    }

    @Test
    fun `trenger enhet`() {
        val person = lagPerson().also(personRepository::lagre)
        utdatertEnhet(person.id)
        assertFalse(command(person.id).execute(context))
        assertTrue(observer.behov.isNotEmpty())
        assertEquals(listOf(Behov.Enhet), observer.behov.toList())
    }

    @Test
    fun `oppdatere enhet`() {
        val person = lagPerson().also(personRepository::lagre)
        utdatertEnhet(person.id)
        val løsning = mockk<HentEnhetløsning>(relaxed = true)
        context.add(løsning)
        assertTrue(command(person.id).execute(context))
        verify(exactly = 1) { løsning.oppdater(personDao, person.id.value) }
    }

    @Test
    fun `trenger infotrygdutbetalinger`() {
        val person = lagPerson().also(personRepository::lagre)
        utdatertUtbetalinger(person.id)
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
        utdatertUtbetalinger(person.id)
        val løsning = mockk<HentInfotrygdutbetalingerløsning>(relaxed = true)
        context.add(løsning)
        assertTrue(command(person.id).execute(context))
        verify(exactly = 1) { løsning.oppdater(personDao, person.id.value) }
    }

    @Test
    fun `oppdatere alt`() {
        val person = lagPerson().also(personRepository::lagre)
        altUtdatert(person.id)
        val enhet = mockk<HentEnhetløsning>(relaxed = true)
        val utbetalinger = mockk<HentInfotrygdutbetalingerløsning>(relaxed = true)
        context.add(enhet)
        context.add(utbetalinger)
        assertTrue(command(person.id).execute(context))
        verify(exactly = 1) { enhet.oppdater(personDao, person.id.value) }
        verify(exactly = 1) { utbetalinger.oppdater(personDao, person.id.value) }
    }

    private fun utdatertEnhet(identitetsnummer: Identitetsnummer) {
        every { personDao.finnEnhetSistOppdatert(identitetsnummer.value) } returns LocalDate.now().minusYears(1)
        every { personDao.finnITUtbetalingsperioderSistOppdatert(identitetsnummer.value) } returns LocalDate.now()
    }

    private fun utdatertUtbetalinger(identitetsnummer: Identitetsnummer) {
        every { personDao.finnEnhetSistOppdatert(identitetsnummer.value) } returns LocalDate.now()
        every { personDao.finnITUtbetalingsperioderSistOppdatert(identitetsnummer.value) } returns LocalDate.now().minusYears(1)
    }

    private fun altUtdatert(identitetsnummer: Identitetsnummer) {
        every { personDao.finnEnhetSistOppdatert(identitetsnummer.value) } returns LocalDate.now().minusYears(1)
        every { personDao.finnITUtbetalingsperioderSistOppdatert(identitetsnummer.value) } returns LocalDate.now().minusYears(1)
    }
}

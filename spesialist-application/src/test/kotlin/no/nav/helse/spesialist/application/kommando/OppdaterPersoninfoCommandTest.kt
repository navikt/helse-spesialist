package no.nav.helse.spesialist.application.kommando

import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.OppdaterPersoninfoCommand
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.person.Adressebeskyttelse
import no.nav.helse.modell.person.HentPersoninfoløsning
import no.nav.helse.spesialist.application.InMemoryPersonRepository
import no.nav.helse.spesialist.domain.Personinfo
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.typer.Kjønn
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class OppdaterPersoninfoCommandTest {
    private companion object {
        private const val FORNAVN = "LITEN"
        private const val MELLOMNAVN = "STOR"
        private const val ETTERNAVN = "TRANFLASKE"
        private val FØDSELSDATO = LocalDate.EPOCH
    }

    private val personRepository = InMemoryPersonRepository()

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

    private val context =
        CommandContext(UUID.randomUUID()).also {
            it.nyObserver(observer)
        }

    @Test
    fun `mangler personinfo`() {
        val person = lagPerson(info = null).also(personRepository::lagre)
        val command = OppdaterPersoninfoCommand(person.id.value, personRepository, force = false)
        assertFalse(command.execute(context))
        assertTrue(observer.behov.isNotEmpty())
        assertEquals(listOf(Behov.Personinfo), observer.behov.toList())
    }

    @Test
    fun `utdatert personinfo`() {
        // given
        val person = lagPerson(infoSistOppdatert = LocalDate.now().minusDays(15)).also(personRepository::lagre)
        val command = OppdaterPersoninfoCommand(person.id.value, personRepository, force = false)

        // when
        val løsning = HentPersoninfoløsning(person.id.value, FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, Kjønn.Ukjent, Adressebeskyttelse.Fortrolig)
        context.add(løsning)
        assertTrue(command.execute(context))

        // then
        val funnet = personRepository.finn(person.id)
        assertNotNull(funnet)
        val personinfo = funnet.info
        assertNotNull(personinfo)
        assertEquals(FORNAVN, personinfo.fornavn)
        assertEquals(MELLOMNAVN, personinfo.mellomnavn)
        assertEquals(ETTERNAVN, personinfo.etternavn)
        assertEquals(FØDSELSDATO, personinfo.fødselsdato)
        assertEquals(Personinfo.Kjønn.Ukjent, personinfo.kjønn)
        assertEquals(Personinfo.Adressebeskyttelse.Fortrolig, personinfo.adressebeskyttelse)
    }

    @Test
    fun `oppdaterer ingenting når informasjonen er ny nok`() {
        val person = lagPerson().also(personRepository::lagre)
        val command = OppdaterPersoninfoCommand(person.id.value, personRepository, force = false)

        assertTrue(command.execute(context))
        assertTrue(observer.behov.isEmpty())
    }

    @Test
    fun `oppdaterer personinfo dersom force er satt til true`() {
        val person = lagPerson().also(personRepository::lagre)
        val command = OppdaterPersoninfoCommand(person.id.value, personRepository, force = true)
        assertFalse(command.execute(context))
        assertTrue(observer.behov.isNotEmpty())

        assertEquals(listOf(Behov.Personinfo), observer.behov.toList())
    }
}

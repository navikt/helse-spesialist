package no.nav.helse.modell.kommando

import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.person.HentEnhetløsning
import no.nav.helse.spesialist.application.kommando.testMedSessionContext
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class OppdaterEnhetCommandTest {
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
    fun `mangler enhet`() =
        testMedSessionContext {
            val person = lagPerson(enhet = null).also(it.personRepository::lagre)
            val command = OppdaterEnhetCommand(person.id.value)
            assertFalse(command.execute(context, it))
            assertTrue(observer.behov.isNotEmpty())
            assertEquals(listOf(Behov.Enhet), observer.behov.toList())
        }

    @Test
    fun `utdatert enhet`() =
        testMedSessionContext {
            // given
            val person = lagPerson(enhetSistOppdatert = LocalDate.now().minusDays(15)).also(it.personRepository::lagre)
            val command = OppdaterEnhetCommand(person.id.value)

            // when
            val løsning = HentEnhetløsning("1002")
            context.add(løsning)
            assertTrue(command.execute(context, it))

            // then
            val funnet = it.personRepository.finn(person.id)
            assertNotNull(funnet)
            val enhet = funnet.enhetRef
            assertNotNull(enhet)
            assertEquals(1002, enhet)
        }

    @Test
    fun `oppdaterer ingenting når informasjonen er ny nok`() =
        testMedSessionContext {
            val person = lagPerson().also(it.personRepository::lagre)
            val command = OppdaterEnhetCommand(person.id.value)
            assertTrue(command.execute(context, it))
            assertTrue(observer.behov.isEmpty())
        }
}

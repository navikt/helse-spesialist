package no.nav.helse.spesialist.application.kommando

import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.mediator.meldinger.løsninger.EgenAnsattløsning
import no.nav.helse.modell.egenansatt.KontrollerEgenAnsattstatus
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.melding.Behov
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class KontrollerEgenAnsattstatusTest : ApplicationTest() {
    private companion object {
        private const val FNR = "12345678911"
    }

    private val command = KontrollerEgenAnsattstatus(FNR)
    private val observer =
        object : CommandContextObserver {
            val behov = mutableListOf<Behov>()

            override fun behov(
                behov: Behov,
                commandContextId: UUID,
                sti: List<Int>,
            ) {
                this.behov.add(behov)
            }
        }
    private val commandContext = CommandContext(UUID.randomUUID()).also { it.nyObserver(observer) }

    private fun lagrePerson(erEgenAnsatt: Boolean?) {
        sessionContext.personRepository.lagre(
            lagPerson(id = Identitetsnummer.fraString(FNR), erEgenAnsatt = erEgenAnsatt),
        )
    }

    private fun egenAnsattStatus() = sessionContext.personRepository.finn(Identitetsnummer.fraString(FNR))?.egenAnsattStatus

    @Test
    fun `ber om informasjon om egen ansatt`() {
        lagrePerson(erEgenAnsatt = null)
        Assertions.assertFalse(command.execute(commandContext, sessionContext, outbox))
        Assertions.assertEquals(listOf(Behov.EgenAnsatt), observer.behov.toList())
    }

    @Test
    fun `mangler løsning ved resume`() {
        lagrePerson(erEgenAnsatt = null)
        Assertions.assertFalse(command.resume(commandContext, sessionContext, outbox))
        Assertions.assertNull(egenAnsattStatus())
    }

    @Test
    fun `lagrer løsning ved resume`() {
        lagrePerson(erEgenAnsatt = null)
        commandContext.add(EgenAnsattløsning(LocalDateTime.now(), FNR, false))
        Assertions.assertTrue(command.resume(commandContext, sessionContext, outbox))
        Assertions.assertNotNull(egenAnsattStatus())
    }

    @Test
    fun `sender ikke behov om informasjonen finnes`() {
        lagrePerson(erEgenAnsatt = false)
        Assertions.assertTrue(command.resume(commandContext, sessionContext, outbox))
        Assertions.assertEquals(emptyList<Behov>(), observer.behov.toList())

        lagrePerson(erEgenAnsatt = true)
        Assertions.assertTrue(command.resume(commandContext, sessionContext, outbox))
        Assertions.assertEquals(emptyList<Behov>(), observer.behov.toList())
    }
}

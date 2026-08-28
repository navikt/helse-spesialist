package no.nav.helse.spesialist.application.kommando

import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.varsel.VurderEnhetUtland
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*

internal class VurderEnhetUtlandTest : ApplicationTest() {
    private val commandContext: CommandContext = CommandContext(UUID.randomUUID())

    fun settOppTestdata(enhet: Int): Person {
        val person =
            lagPerson(enhet = enhet)
                .also(sessionContext.personRepository::lagre)
        val vedtaksperiode =
            lagVedtaksperiode(id = VedtaksperiodeId(vedtaksperiodeId))
                .also(sessionContext.vedtaksperiodeRepository::lagre)
        lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
            .also(sessionContext.behandlingRepository::lagre)

        return person
    }

    @Test
    fun `skal legge på varsel om utland`() {
        val person = settOppTestdata(enhet = 393)
        assertTrue(hentCommand(person).execute(commandContext, sessionContext, outbox))
        assertEquals(
            "SB_EX_5",
            sessionContext.varselRepository
                .alle()
                .single()
                .kode,
        )
    }

    @Test
    fun `legger ikke på varsel om utland for enhet som ikke er utland`() {
        val person = settOppTestdata(enhet = 1001)
        assertTrue(hentCommand(person).execute(commandContext, sessionContext, outbox))
        assertEquals(0, sessionContext.varselRepository.alle().size)
    }

    private fun hentCommand(person: Person) =
        VurderEnhetUtland(
            fødselsnummer = person.id.value,
            vedtaksperiodeId = VedtaksperiodeId(vedtaksperiodeId),
        )

    private companion object {
        private val vedtaksperiodeId = UUID.randomUUID()
    }
}

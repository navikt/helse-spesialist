package no.nav.helse.spesialist.application.kommando

import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.varsel.VurderEnhetUtland
import no.nav.helse.spesialist.domain.Varsel
import java.util.*
import kotlin.test.Test
import kotlin.test.assertTrue

internal class VurderEnhetUtlandTest : ApplicationTest() {
    private val commandContext: CommandContext = CommandContext(UUID.randomUUID())

    @Test
    fun `skal legge på varsel om utland`() {
        person.oppdaterEnhet(393)
        sessionContext.personRepository.lagre(person)
        assertTrue(
            VurderEnhetUtland(
                identitetsnummer = person.id,
                spleisBehandlingId = behandling1.spleisBehandlingId!!,
            ).execute(commandContext, sessionContext, outbox),
        )
        behandling1.assertHarVarsel("SB_EX_5", Varsel.Status.AKTIV)
    }
}

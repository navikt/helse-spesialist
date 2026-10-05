package no.nav.helse.spesialist.application.kommando

import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.varsel.VurderEnhetUtland
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.VarselId
import java.time.LocalDateTime
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

    @Test
    fun `forsøker ikke å legge på et nytt varsel dersom vi allerede har et`() {
        // given
        person.oppdaterEnhet(393)
        sessionContext.personRepository.lagre(person)
        val eksisterendeVarsel =
            Varsel.fraLagring(
                behandlingUnikId = behandling1.id,
                spleisBehandlingId = behandling1.spleisBehandlingId,
                kode = "SB_EX_5",
                id = VarselId(UUID.randomUUID()),
                status = Varsel.Status.VURDERT,
                opprettetTidspunkt = LocalDateTime.now(),
                vurdering = null,
            )
        sessionContext.varselRepository.lagre(eksisterendeVarsel)

        // when
        val resultatAvKommandokjøring =
            VurderEnhetUtland(
                identitetsnummer = person.id,
                spleisBehandlingId = behandling1.spleisBehandlingId!!,
            ).execute(commandContext, sessionContext, outbox)

        // then
        assertTrue(
            resultatAvKommandokjøring,
        )
        behandling1.assertHarVarsel("SB_EX_5", Varsel.Status.VURDERT)
    }
}

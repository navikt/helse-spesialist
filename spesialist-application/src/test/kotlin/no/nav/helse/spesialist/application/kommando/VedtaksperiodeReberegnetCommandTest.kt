package no.nav.helse.spesialist.application.kommando

import no.nav.helse.db.MeldingDao.Meldingtype.VEDTAKSPERIODE_REBEREGNET
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeReberegnetCommand
import no.nav.helse.spesialist.domain.testfixtures.lagOppgave
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID.randomUUID
import kotlin.test.assertEquals

internal class VedtaksperiodeReberegnetCommandTest : ApplicationTest() {
    @Test
    fun `avbryter kommandokjede for godkjenningsbehov når spleis reberegner perioden`() {
        // given
        val commandContextIdForGodkjenningsbehov = randomUUID()
        val oppgave =
            lagOppgave(
                vedtaksperiodeId = vedtaksperiode1.id,
                behandlingId = behandling1.spleisBehandlingId!!,
                godkjenningsbehovId = randomUUID(),
            )
        sessionContext.oppgaveRepository.lagre(oppgave)

        // when
        val contextForReberegning = CommandContext(randomUUID())
        val hendelseId = randomUUID()
        sessionContext.meldingDao.lagre(hendelseId, "{}", VEDTAKSPERIODE_REBEREGNET, vedtaksperiode1.id.value)
        sessionContext.commandContextDao.opprett(hendelseId, commandContextIdForGodkjenningsbehov)
        val kommandoFerdig =
            VedtaksperiodeReberegnetCommand(
                identitetsnummer = person.id,
                vedtaksperiodeId = vedtaksperiode1.id,
                spleisBehandlingId = behandling1.spleisBehandlingId!!,
                behandlingUnikId = behandling1.id,
            ).execute(contextForReberegning, sessionContext, outbox)

        // then
        assertTrue(kommandoFerdig)
        assertEquals(
            mapOf(commandContextIdForGodkjenningsbehov to hendelseId),
            sessionContext.commandContextDao.avbrutteKommandokjeder,
        )
    }
}

package no.nav.helse.spesialist.application.kommando

import no.nav.helse.db.MeldingDao.Meldingtype.VEDTAKSPERIODE_REBEREGNET
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeReberegnetCommand
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagOppgave
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID.randomUUID
import kotlin.test.assertEquals

internal class VedtaksperiodeReberegnetCommandTest : ApplicationTest() {

    @Test
    fun `avbryter kommandokjede for godkjenningsbehov når spleis reberegner perioden`() {
        // given
        val commandContextIdForGodkjenningsbehov = randomUUID()
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val vedtaksperiode =
            lagVedtaksperiode(identitetsnummer = person.id).also(sessionContext.vedtaksperiodeRepository::lagre)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val oppgave = lagOppgave(
            vedtaksperiodeId = vedtaksperiode.id,
            behandlingId = behandling.spleisBehandlingId!!,
            godkjenningsbehovId = randomUUID()
        )
        sessionContext.oppgaveRepository.lagre(oppgave)

        // when
        val contextForReberegning = CommandContext(randomUUID())
        val hendelseId = randomUUID()
        sessionContext.meldingDao.lagre(hendelseId, "{}", VEDTAKSPERIODE_REBEREGNET, vedtaksperiode.id.value)
        sessionContext.commandContextDao.opprett(hendelseId, commandContextIdForGodkjenningsbehov)
        val kommandoFerdig = VedtaksperiodeReberegnetCommand(
            fødselsnummer = person.id.value,
            vedtaksperiodeId = vedtaksperiode.id.value,
            spleisBehandlingId = behandling.spleisBehandlingId!!,
            spesialistBehandlingId = randomUUID(),
            periodehistorikkDao = sessionContext.periodehistorikkDao,
        ).execute(contextForReberegning, sessionContext, outbox)

        // then
        assertTrue(kommandoFerdig)
        assertEquals(
            mapOf(commandContextIdForGodkjenningsbehov to hendelseId),
            sessionContext.commandContextDao.avbrutteKommandokjeder
        )
    }
}

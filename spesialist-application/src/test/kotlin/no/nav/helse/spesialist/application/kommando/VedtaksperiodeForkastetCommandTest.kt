package no.nav.helse.spesialist.application.kommando

import no.nav.helse.db.MeldingDao.Meldingtype.VEDTAKSPERIODE_FORKASTET
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeForkastetCommand
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagOppgave
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import java.util.UUID.randomUUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class VedtaksperiodeForkastetCommandTest : ApplicationTest() {

    @Test
    fun `avbryter kommandokjede for godkjenningsbehov når perioden forkastes`() {
        // given
        val commandContextIdForGodkjenningsbehov = randomUUID()
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val vedtaksperiode =
            lagVedtaksperiode(identitetsnummer = person.id).also(sessionContext.vedtaksperiodeRepository::lagre)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)

        // when
        val contextForVedtaksperiodeForkastet = CommandContext(randomUUID())
        val hendelseId = randomUUID()
        sessionContext.meldingDao.lagre(hendelseId, "{}", VEDTAKSPERIODE_FORKASTET, vedtaksperiode.id.value)
        sessionContext.commandContextDao.opprett(hendelseId, commandContextIdForGodkjenningsbehov)
        val kommandoFerdig = VedtaksperiodeForkastetCommand(
            fødselsnummer = person.id.value,
            vedtaksperiodeId = vedtaksperiode.id.value,
            spleisBehandlingId = behandling.spleisBehandlingId!!,
            alleForkastedeVedtaksperiodeIder = emptyList(),
            oppgaveRepository = sessionContext.oppgaveRepository,
        ).execute(contextForVedtaksperiodeForkastet, sessionContext, outbox)

        // then
        assertTrue(kommandoFerdig)
        assertEquals(
            mapOf(commandContextIdForGodkjenningsbehov to hendelseId),
            sessionContext.commandContextDao.avbrutteKommandokjeder
        )
    }

    @Test
    fun `sletter også oppgave når perioden forkastes`() {
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
        val contextForVedtaksperiodeForkastet = CommandContext(randomUUID())
        val hendelseId = randomUUID()
        sessionContext.meldingDao.lagre(hendelseId, "{}", VEDTAKSPERIODE_FORKASTET, vedtaksperiode.id.value)
        sessionContext.commandContextDao.opprett(hendelseId, commandContextIdForGodkjenningsbehov)
        val kommandoFerdig = VedtaksperiodeForkastetCommand(
            fødselsnummer = person.id.value,
            vedtaksperiodeId = vedtaksperiode.id.value,
            spleisBehandlingId = behandling.spleisBehandlingId!!,
            alleForkastedeVedtaksperiodeIder = emptyList(),
            oppgaveRepository = sessionContext.oppgaveRepository,
        ).execute(contextForVedtaksperiodeForkastet, sessionContext, outbox)

        // then
        assertTrue(kommandoFerdig)
        assertEquals(
            mapOf(commandContextIdForGodkjenningsbehov to hendelseId),
            sessionContext.commandContextDao.avbrutteKommandokjeder
        )
    }

    @Test
    fun `sletter ikke oppgave for en annen vedtaksperiode`() {
        // given
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
        val contextForVedtaksperiodeForkastet = CommandContext(randomUUID())
        val hendelseId = randomUUID()
        val vedtaksperiodeIdForkastetVedtaksperiode = randomUUID()
        sessionContext.meldingDao.lagre(hendelseId, "{}", VEDTAKSPERIODE_FORKASTET, vedtaksperiodeIdForkastetVedtaksperiode)
        sessionContext.commandContextDao.opprett(hendelseId, randomUUID())
        val kommandoFerdig = VedtaksperiodeForkastetCommand(
            fødselsnummer = person.id.value,
            vedtaksperiodeId = vedtaksperiode.id.value,
            spleisBehandlingId = behandling.spleisBehandlingId!!,
            alleForkastedeVedtaksperiodeIder = emptyList(),
            oppgaveRepository = sessionContext.oppgaveRepository,
        ).execute(contextForVedtaksperiodeForkastet, sessionContext, outbox)

        // then
        assertTrue(kommandoFerdig)
        assertEquals(emptyMap(), sessionContext.commandContextDao.avbrutteKommandokjeder)
        assertEquals(vedtaksperiode.id, sessionContext.oppgaveRepository.finn(behandling.spleisBehandlingId!!)?.vedtaksperiodeId)
    }
}

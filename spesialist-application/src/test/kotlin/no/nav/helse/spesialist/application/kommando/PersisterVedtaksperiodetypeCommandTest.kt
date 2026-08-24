package no.nav.helse.spesialist.application.kommando

import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.PersisterVedtaksperiodetypeCommand
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

internal class PersisterVedtaksperiodetypeCommandTest : ApplicationTest() {
    private val HENDELSE_ID = UUID.randomUUID()

    @Test
    fun `Legger til vedtaksperiodetype`() {
        val type = Periodetype.FØRSTEGANGSBEHANDLING
        val inntektskilde = Inntektskilde.EN_ARBEIDSGIVER
        PersisterVedtaksperiodetypeCommand(HENDELSE_ID, type, inntektskilde)
            .execute(CommandContext(UUID.randomUUID()), sessionContext, outbox)
        assertEquals(type, sessionContext.vedtakDao.finnVedtaksperiodetype(HENDELSE_ID))
        assertEquals(inntektskilde, sessionContext.vedtakDao.finnInntektskilde(HENDELSE_ID))
    }
}

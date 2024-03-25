package no.nav.helse.modell.vedtaksperiode

import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.modell.kommando.CommandContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OpprettKoblingTilGenerasjonCommandTest {
    private val generasjonId = UUID.randomUUID()
    private val vedtaksperiodeId = UUID.randomUUID()
    private val utbetalingId = UUID.randomUUID()
    private val hendelseId = UUID.randomUUID()
    private val generasjon = Generasjon.nyVedtaksperiode(generasjonId, vedtaksperiodeId, 1.januar, 31.januar, 1.januar)

    private val command = OpprettKoblingTilGenerasjonCommand(
        hendelseId = hendelseId,
        utbetalingId = utbetalingId,
        gjeldendeGenerasjon = generasjon
    )

    @Test
    fun `Opprett kobling til utbetaling for generasjon`() {
        command.execute(CommandContext(UUID.randomUUID()))
        assertEquals(utbetalingId, generasjon.toDto().utbetalingId)
    }
}
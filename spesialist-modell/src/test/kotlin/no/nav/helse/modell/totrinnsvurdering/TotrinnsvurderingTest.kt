package no.nav.helse.modell.totrinnsvurdering

import no.nav.helse.modell.OppgaveAlleredeSendtBeslutter
import no.nav.helse.modell.OppgaveAlleredeSendtIRetur
import no.nav.helse.modell.OppgaveKreverVurderingAvToSaksbehandlere
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.Saksbehandler.Companion.toDto
import no.nav.helse.modell.saksbehandler.Tilgangskontroll
import no.nav.helse.modell.saksbehandler.handlinger.TilgangskontrollForTestHarIkkeTilgang
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering.Companion.gjenopprett
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering.Companion.toDto
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingTest.TotrinnsvurderingInspektør.Companion.inspektør
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.UUID
import kotlin.properties.Delegates

internal class TotrinnsvurderingTest {

    @Test
    fun `send til beslutter`() {
        val totrinnsvurdering = nyTotrinnsvurdering()
        val behandlendeSaksbehandler = nySaksbehandler()
        totrinnsvurdering.sendTilBeslutter(1L, behandlendeSaksbehandler)
        inspektør(totrinnsvurdering) {
            assertEquals(behandlendeSaksbehandler, saksbehandler)
            assertEquals(null, beslutter)
            assertEquals(false, erRetur)
            assertEquals(null, utbetalingId)
        }
    }

    @Test
    fun `send i retur`() {
        val totrinnsvurdering = nyTotrinnsvurdering()
        val behandlendeSaksbehandler = nySaksbehandler()
        val besluttendeSaksbehandler = nySaksbehandler()
        totrinnsvurdering.sendTilBeslutter(1L, behandlendeSaksbehandler)
        totrinnsvurdering.sendIRetur(1L, besluttendeSaksbehandler)
        inspektør(totrinnsvurdering) {
            assertEquals(behandlendeSaksbehandler, saksbehandler)
            assertEquals(besluttendeSaksbehandler, beslutter)
            assertEquals(true, erRetur)
            assertEquals(null, utbetalingId)
        }
    }

    @Test
    fun `send til beslutter etter retur`() {
        val totrinnsvurdering = nyTotrinnsvurdering()
        val behandlendeSaksbehandler = nySaksbehandler()
        val besluttendeSaksbehandler = nySaksbehandler()
        totrinnsvurdering.sendTilBeslutter(1L, behandlendeSaksbehandler)
        totrinnsvurdering.sendIRetur(1L, besluttendeSaksbehandler)
        totrinnsvurdering.sendTilBeslutter(1L, behandlendeSaksbehandler)
        inspektør(totrinnsvurdering) {
            assertEquals(behandlendeSaksbehandler, saksbehandler)
            assertEquals(besluttendeSaksbehandler, beslutter)
            assertEquals(false, erRetur)
            assertEquals(null, utbetalingId)
        }
    }

    @Test
    fun ferdigstill() {
        val totrinnsvurdering = nyTotrinnsvurdering()
        val behandlendeSaksbehandler = nySaksbehandler()
        totrinnsvurdering.sendTilBeslutter(1L, behandlendeSaksbehandler)
        val utbetalingId = UUID.randomUUID()
        totrinnsvurdering.ferdigstill(utbetalingId)
        inspektør(totrinnsvurdering) {
            assertEquals(behandlendeSaksbehandler, saksbehandler)
            assertEquals(false, erRetur)
            assertEquals(utbetalingId, this.utbetalingId)
        }
    }

    @Test
    fun `Kan ikke sende til beslutter hvis allerede sendt til beslutter`() {
        val totrinnsvurdering = nyTotrinnsvurdering()
        val behandlendeSaksbehandler = nySaksbehandler()
        totrinnsvurdering.sendTilBeslutter(1L, behandlendeSaksbehandler)
        assertThrows<OppgaveAlleredeSendtBeslutter> {
            totrinnsvurdering.sendTilBeslutter(1L, behandlendeSaksbehandler)
        }
    }

    @Test
    fun `Beslutter kan ikke sende til samme beslutter`() {
        val totrinnsvurdering = nyTotrinnsvurdering()
        val behandlendeSaksbehandler = nySaksbehandler()
        val beslutter = nySaksbehandler()
        totrinnsvurdering.sendTilBeslutter(1L, behandlendeSaksbehandler)
        totrinnsvurdering.sendIRetur(1L, beslutter)
        assertThrows<OppgaveKreverVurderingAvToSaksbehandlere> {
            totrinnsvurdering.sendTilBeslutter(1L, beslutter)
        }
    }

    @Test
    fun `Kan ikke sende i retur hvis allerede sendt i retur`() {
        val totrinnsvurdering = nyTotrinnsvurdering()
        val behandlendeSaksbehandler = nySaksbehandler()
        val beslutter = nySaksbehandler()
        totrinnsvurdering.sendTilBeslutter(1L, behandlendeSaksbehandler)
        totrinnsvurdering.sendIRetur(1L, beslutter)
        assertThrows<OppgaveAlleredeSendtIRetur> {
            totrinnsvurdering.sendIRetur(1L, beslutter)
        }
    }

    @Test
    fun `saksbehandler kan ikke sende i retur til samme saksbehandler`() {
        val totrinnsvurdering = nyTotrinnsvurdering()
        val behandlendeSaksbehandler = nySaksbehandler()
        totrinnsvurdering.sendTilBeslutter(1L, behandlendeSaksbehandler)
        assertThrows<OppgaveKreverVurderingAvToSaksbehandlere> {
            totrinnsvurdering.sendIRetur(1L, behandlendeSaksbehandler)
        }
    }

    @Test
    fun `fra og til dto`() {
        val totrinnsvurderingDto = TotrinnsvurderingDto(
            vedtaksperiodeId = UUID.randomUUID(),
            erRetur = false,
            saksbehandler = nySaksbehandler().toDto(),
            beslutter = nySaksbehandler().toDto(),
            utbetalingId = UUID.randomUUID(),
            opprettet = LocalDateTime.now(),
            oppdatert = LocalDateTime.now(),
        )

        val toDto = totrinnsvurderingDto.gjenopprett(TilgangskontrollForTestHarIkkeTilgang).toDto()
        assertEquals(totrinnsvurderingDto.vedtaksperiodeId, toDto.vedtaksperiodeId)
        assertEquals(totrinnsvurderingDto.erRetur, toDto.erRetur)
        assertEquals(totrinnsvurderingDto.saksbehandler, toDto.saksbehandler)
        assertEquals(totrinnsvurderingDto.beslutter, toDto.beslutter)
        assertEquals(totrinnsvurderingDto.utbetalingId, toDto.utbetalingId)
        assertEquals(totrinnsvurderingDto.opprettet.withNano(0), toDto.opprettet.withNano(0))
        assertEquals(totrinnsvurderingDto.oppdatert?.withNano(0), toDto.oppdatert?.withNano(0))
    }

    private fun nySaksbehandler(
        oid: UUID = UUID.randomUUID(),
        tilgangskontroll: Tilgangskontroll = TilgangskontrollForTestHarIkkeTilgang
    ) = Saksbehandler("epostadresse@nav.no", oid, "navn", "ident", tilgangskontroll)

    private fun nyTotrinnsvurdering(
        erRetur: Boolean = false,
        saksbehandler: Saksbehandler? = null,
        beslutter: Saksbehandler? = null,
    ) = Totrinnsvurdering(
        vedtaksperiodeId = UUID.randomUUID(),
        erRetur = erRetur,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        utbetalingId = null,
        opprettet = LocalDateTime.now(),
        oppdatert = null
    )

    private class TotrinnsvurderingInspektør private constructor(): TotrinnsvurderingVisitor {
        lateinit var vedtaksperiodeId: UUID
        var erRetur by Delegates.notNull<Boolean>()
        var saksbehandler: Saksbehandler? = null
        var beslutter: Saksbehandler? = null
        var utbetalingId: UUID? = null
        lateinit var opprettet: LocalDateTime
        var oppdatert: LocalDateTime? = null
        override fun visitTotrinnsvurdering(
            vedtaksperiodeId: UUID,
            erRetur: Boolean,
            saksbehandler: Saksbehandler?,
            beslutter: Saksbehandler?,
            utbetalingId: UUID?,
            opprettet: LocalDateTime,
            oppdatert: LocalDateTime?
        ) {
            this.vedtaksperiodeId = vedtaksperiodeId
            this.erRetur = erRetur
            this.saksbehandler = saksbehandler
            this.beslutter = beslutter
            this.utbetalingId = utbetalingId
            this.opprettet = opprettet
            this.oppdatert = oppdatert
        }

        companion object {
            fun inspektør(totrinnsvurdering: Totrinnsvurdering, block: TotrinnsvurderingInspektør.() -> Unit) {
                val inspektør = TotrinnsvurderingInspektør()
                totrinnsvurdering.accept(inspektør)
                block(inspektør)
            }
        }

    }
}

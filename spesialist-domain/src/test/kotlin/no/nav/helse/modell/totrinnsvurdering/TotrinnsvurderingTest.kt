package no.nav.helse.modell.totrinnsvurdering

import no.nav.helse.modell.OppgaveAlleredeSendtBeslutter
import no.nav.helse.modell.OppgaveAlleredeSendtIRetur
import no.nav.helse.modell.OppgaveKreverVurderingAvToSaksbehandlere
import no.nav.helse.modell.jan
import no.nav.helse.modell.lagAktørId
import no.nav.helse.modell.lagFødselsnummer
import no.nav.helse.modell.lagOrganisasjonsnummer
import no.nav.helse.modell.saksbehandler.handlinger.Overstyring
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtTidslinje
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtTidslinjedag
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random.Default.nextLong

internal class TotrinnsvurderingTest {

    @Test
    fun `send til beslutter`() {
        val totrinnsvurdering = nyTotrinnsvurdering()
        val behandlendeSaksbehandler = nySaksbehandler()
        totrinnsvurdering.sendTilBeslutter(1L, behandlendeSaksbehandler)

        assertEquals(behandlendeSaksbehandler, totrinnsvurdering.saksbehandler)
        assertEquals(null, totrinnsvurdering.beslutter)
        assertEquals(false, totrinnsvurdering.erRetur)
        assertEquals(null, totrinnsvurdering.utbetalingId)
    }

    @Test
    fun `send i retur`() {
        val totrinnsvurdering = nyTotrinnsvurdering()
        val behandlendeSaksbehandler = nySaksbehandler()
        val besluttendeSaksbehandler = nySaksbehandler()
        totrinnsvurdering.sendTilBeslutter(1L, behandlendeSaksbehandler)
        totrinnsvurdering.sendIRetur(1L, besluttendeSaksbehandler)

        assertEquals(behandlendeSaksbehandler, totrinnsvurdering.saksbehandler)
        assertEquals(besluttendeSaksbehandler, totrinnsvurdering.beslutter)
        assertEquals(true, totrinnsvurdering.erRetur)
        assertEquals(null, totrinnsvurdering.utbetalingId)
    }

    @Test
    fun `send til beslutter etter retur`() {
        val totrinnsvurdering = nyTotrinnsvurdering()
        val behandlendeSaksbehandler = nySaksbehandler()
        val besluttendeSaksbehandler = nySaksbehandler()
        totrinnsvurdering.sendTilBeslutter(1L, behandlendeSaksbehandler)
        totrinnsvurdering.sendIRetur(1L, besluttendeSaksbehandler)
        totrinnsvurdering.sendTilBeslutter(1L, behandlendeSaksbehandler)
        assertEquals(behandlendeSaksbehandler, totrinnsvurdering.saksbehandler)
        assertEquals(besluttendeSaksbehandler, totrinnsvurdering.beslutter)
        assertEquals(false, totrinnsvurdering.erRetur)
        assertEquals(null, totrinnsvurdering.utbetalingId)
    }

    @Test
    fun ferdigstill() {
        val totrinnsvurdering = nyTotrinnsvurdering()
        val behandlendeSaksbehandler = nySaksbehandler()
        totrinnsvurdering.sendTilBeslutter(1L, behandlendeSaksbehandler)
        val utbetalingId = UUID.randomUUID()
        totrinnsvurdering.ferdigstill(utbetalingId)
        assertEquals(behandlendeSaksbehandler, totrinnsvurdering.saksbehandler)
        assertEquals(false, totrinnsvurdering.erRetur)
        assertEquals(utbetalingId, totrinnsvurdering.utbetalingId)
    }

    @Test
    fun `ferdigstill ferdigstiller også overstyringer`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val totrinnsvurdering = nyTotrinnsvurdering(vedtaksperiodeId = vedtaksperiodeId, overstyringer = listOf(OverstyrtTidslinje.ny(
                vedtaksperiodeId = vedtaksperiodeId,
                aktørId = "123",
                fødselsnummer = "1234",
                organisasjonsnummer = "12345",
                dager = overstyrteDager(),
                begrunnelse = "begrunnelse",
                saksbehandlerOid = UUID.randomUUID()
        )))
        val behandlendeSaksbehandler = nySaksbehandler()
        totrinnsvurdering.sendTilBeslutter(1L, behandlendeSaksbehandler)
        val utbetalingId = UUID.randomUUID()
        totrinnsvurdering.ferdigstill(utbetalingId)
        assertEquals(behandlendeSaksbehandler, totrinnsvurdering.saksbehandler)
        assertEquals(false, totrinnsvurdering.erRetur)
        totrinnsvurdering.overstyringer.forEach {
            assertTrue(it.ferdigstilt)
        }
        assertEquals(utbetalingId, totrinnsvurdering.utbetalingId)
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
    fun `kan legge til ny overstyring`() {
        val totrinnsvurdering = nyTotrinnsvurdering()
        totrinnsvurdering.nyOverstyring(OverstyrtTidslinje.ny(
            vedtaksperiodeId = UUID.randomUUID(),
            aktørId = lagAktørId(),
            fødselsnummer = lagFødselsnummer(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            dager = emptyList(),
            begrunnelse = "begrunnelse",
            saksbehandlerOid = nySaksbehandler().value,
        ))
        assertEquals(1, totrinnsvurdering.overstyringer.size)
    }

    @Test
    fun `ferdigstilt er default false`() {
        val totrinnsvurdering = nyTotrinnsvurdering()
        assertFalse(totrinnsvurdering.ferdigstilt)
    }

    private fun nySaksbehandler(
        oid: UUID = UUID.randomUUID()
    ) = SaksbehandlerOid(oid)

    private fun nyTotrinnsvurdering(
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        erRetur: Boolean = false,
        saksbehandler: SaksbehandlerOid? = null,
        beslutter: SaksbehandlerOid? = null,
        overstyringer: List<Overstyring> = emptyList()
    ) = Totrinnsvurdering.fraLagring(
        id = TotrinnsvurderingId(nextLong()),
        vedtaksperiodeId = vedtaksperiodeId,
        erRetur = erRetur,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        utbetalingId = null,
        opprettet = LocalDateTime.now(),
        oppdatert = LocalDateTime.now(),
        overstyringer = overstyringer,
        ferdigstilt = false,
    )

    private fun overstyrteDager(): List<OverstyrtTidslinjedag> = listOf(
        OverstyrtTidslinjedag(
            dato = 1 jan 2018,
            type = "Sykedag",
            fraType = "Sykedag",
            grad = 100,
            fraGrad = 100,
            lovhjemmel = Lovhjemmel(
                paragraf = "22-13",
                ledd = "7",
                lovverk = "folketrygdloven",
                lovverksversjon = "2019-06-21",
            )
        ),
        OverstyrtTidslinjedag(
            dato = 2 jan 2018,
            type = "Sykedag",
            fraType = "Sykedag",
            grad = 100,
            fraGrad = 100,
            lovhjemmel = Lovhjemmel(
                paragraf = "22-13",
                ledd = "7",
                lovverk = "folketrygdloven",
                lovverksversjon = "2019-06-21",
            )
        ),
        OverstyrtTidslinjedag(
            dato = 3 jan 2018,
            type = "Sykedag",
            fraType = "Sykedag",
            grad = 100,
            fraGrad = 100,
            lovhjemmel = Lovhjemmel(
                paragraf = "ANNEN PARAGRAF",
                ledd = "ANNET LEDD",
                bokstav = "EN BOKSTAV",
                lovverk = "ANNEN LOV",
                lovverksversjon = "1970-01-01",
            )
        ),
        OverstyrtTidslinjedag(
            dato = 4 jan 2018,
            type = "Feriedag",
            fraType = "Sykedag",
            grad = 100,
            fraGrad = 100,
            lovhjemmel = null,
        )
    )
}

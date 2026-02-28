package no.nav.helse.modell.totrinnsvurdering

import no.nav.helse.modell.OppgaveAlleredeSendtBeslutter
import no.nav.helse.modell.OppgaveAlleredeSendtIRetur
import no.nav.helse.modell.OppgaveKreverVurderingAvToSaksbehandlere
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.Totrinnsvurdering
import no.nav.helse.spesialist.domain.TotrinnsvurderingId
import no.nav.helse.spesialist.domain.TotrinnsvurderingTilstand
import no.nav.helse.spesialist.domain.TotrinnsvurderingTilstand.AVVENTER_BESLUTTER
import no.nav.helse.spesialist.domain.TotrinnsvurderingTilstand.AVVENTER_SAKSBEHANDLER
import no.nav.helse.spesialist.domain.TotrinnsvurderingTilstand.GODKJENT
import no.nav.helse.spesialist.domain.overstyringer.Overstyring
import no.nav.helse.spesialist.domain.overstyringer.OverstyrtTidslinje
import no.nav.helse.spesialist.domain.overstyringer.OverstyrtTidslinjedag
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagAktørId
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertEquals
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
        assertEquals(AVVENTER_BESLUTTER, totrinnsvurdering.tilstand)
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
        assertEquals(AVVENTER_SAKSBEHANDLER, totrinnsvurdering.tilstand)
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
        assertEquals(AVVENTER_BESLUTTER, totrinnsvurdering.tilstand)
    }

    @Test
    fun ferdigstill() {
        val totrinnsvurdering = nyTotrinnsvurdering()
        val behandlendeSaksbehandler = nySaksbehandler()
        totrinnsvurdering.sendTilBeslutter(1L, behandlendeSaksbehandler)
        totrinnsvurdering.ferdigstill()
        assertEquals(behandlendeSaksbehandler, totrinnsvurdering.saksbehandler)
        assertEquals(GODKJENT, totrinnsvurdering.tilstand)
    }

    @Test
    fun `ferdigstiller alle overstyringer for person`() {
        val totrinnsvurdering = nyTotrinnsvurdering(
            overstyringer = listOf(
                OverstyrtTidslinje.ny(
                    vedtaksperiodeId = UUID.randomUUID(),
                    aktørId = "123",
                    fødselsnummer = "1234",
                    organisasjonsnummer = "12345",
                    dager = overstyrteDager(),
                    begrunnelse = "begrunnelse",
                    saksbehandlerOid = SaksbehandlerOid(UUID.randomUUID())
                ), OverstyrtTidslinje.ny(
                    vedtaksperiodeId = UUID.randomUUID(),
                    aktørId = "123",
                    fødselsnummer = "1234",
                    organisasjonsnummer = "12345",
                    dager = overstyrteDager(),
                    begrunnelse = "begrunnelse",
                    saksbehandlerOid = SaksbehandlerOid(UUID.randomUUID())
                )
            )
        )
        val behandlendeSaksbehandler = nySaksbehandler()
        totrinnsvurdering.sendTilBeslutter(1L, behandlendeSaksbehandler)
        totrinnsvurdering.ferdigstill()
        assertEquals(behandlendeSaksbehandler, totrinnsvurdering.saksbehandler)
        assertEquals(GODKJENT, totrinnsvurdering.tilstand)
        totrinnsvurdering.overstyringer.forEach {
            assertTrue(it.ferdigstilt)
        }
    }

    @Test
    fun `ferdigstill ferdigstiller også overstyringer`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val totrinnsvurdering = nyTotrinnsvurdering(
            overstyringer = listOf(
                OverstyrtTidslinje.ny(
                    vedtaksperiodeId = vedtaksperiodeId,
                    aktørId = "123",
                    fødselsnummer = "1234",
                    organisasjonsnummer = "12345",
                    dager = overstyrteDager(),
                    begrunnelse = "begrunnelse",
                    saksbehandlerOid = SaksbehandlerOid(UUID.randomUUID())
                ), OverstyrtTidslinje.ny(
                    vedtaksperiodeId = UUID.randomUUID(),
                    aktørId = "123",
                    fødselsnummer = "1234",
                    organisasjonsnummer = "12345",
                    dager = overstyrteDager(),
                    begrunnelse = "begrunnelse",
                    saksbehandlerOid = SaksbehandlerOid(UUID.randomUUID())
                )
            )
        )
        val behandlendeSaksbehandler = nySaksbehandler()
        totrinnsvurdering.sendTilBeslutter(1L, behandlendeSaksbehandler)
        totrinnsvurdering.ferdigstill()
        assertEquals(behandlendeSaksbehandler, totrinnsvurdering.saksbehandler)
        assertEquals(GODKJENT, totrinnsvurdering.tilstand)
        totrinnsvurdering.overstyringer.forEach {
            assertTrue(it.ferdigstilt)
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
    fun `kan legge til ny overstyring`() {
        val totrinnsvurdering = nyTotrinnsvurdering()
        totrinnsvurdering.nyOverstyring(
            OverstyrtTidslinje.ny(
                vedtaksperiodeId = UUID.randomUUID(),
                aktørId = lagAktørId(),
                fødselsnummer = lagFødselsnummer(),
                organisasjonsnummer = lagOrganisasjonsnummer(),
                dager = emptyList(),
                begrunnelse = "begrunnelse",
                saksbehandlerOid = nySaksbehandler(),
            )
        )
        assertEquals(1, totrinnsvurdering.overstyringer.size)
    }

    @Test
    fun `tilstand er default AVVENTER_SAKSBEHANDLER`() {
        val totrinnsvurdering = nyTotrinnsvurdering()
        assertEquals(AVVENTER_SAKSBEHANDLER, totrinnsvurdering.tilstand)
    }

    @Test
    fun `kan sette vedtaksperiodeForkastet til true`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val totrinnsvurdering = nyTotrinnsvurdering(overstyringer = listOf(nyOverstyring(vedtaksperiodeId)))
        totrinnsvurdering.vedtaksperiodeForkastet(listOf(vedtaksperiodeId))
        assertTrue(totrinnsvurdering.vedtaksperiodeForkastet)
    }

    private fun nySaksbehandler(
        oid: UUID = UUID.randomUUID()
    ) = SaksbehandlerOid(oid)

    private fun nyOverstyring(vedtaksperiodeId: UUID = UUID.randomUUID()) = OverstyrtTidslinje.ny(
        vedtaksperiodeId = vedtaksperiodeId,
        aktørId = "123",
        fødselsnummer = "1234",
        organisasjonsnummer = "12345",
        dager = overstyrteDager(),
        begrunnelse = "begrunnelse",
        saksbehandlerOid = SaksbehandlerOid(UUID.randomUUID())
    )

    private fun nyTotrinnsvurdering(
        tilstand: TotrinnsvurderingTilstand = AVVENTER_SAKSBEHANDLER,
        saksbehandler: SaksbehandlerOid? = null,
        beslutter: SaksbehandlerOid? = null,
        overstyringer: List<Overstyring> = emptyList()
    ) = Totrinnsvurdering.fraLagring(
        id = TotrinnsvurderingId(nextLong()),
        fødselsnummer = "1234",
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        opprettet = LocalDateTime.now(),
        oppdatert = LocalDateTime.now(),
        overstyringer = overstyringer,
        tilstand = tilstand,
        vedtaksperiodeForkastet = false,
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

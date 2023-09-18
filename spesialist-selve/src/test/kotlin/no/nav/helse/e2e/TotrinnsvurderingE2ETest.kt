package no.nav.helse.e2e

import AbstractE2ETestV2
import no.nav.helse.Testdata.ORGNR_GHOST
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.spesialist.api.overstyring.OverstyringType
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrArbeidsforholdHandlingFraApi
import org.junit.jupiter.api.Test

internal class TotrinnsvurderingE2ETest : AbstractE2ETestV2() {

    @Test
    fun `sak blir trukket til totrinnsvurdering ved overstyring av inntekt og refusjon`() {
        fremTilSaksbehandleroppgave()
        håndterOverstyrInntektOgRefusjon()
        fremTilSaksbehandleroppgave(harOppdatertMetadata = true, harRisikovurdering = true)
        assertOverstyringer(VEDTAKSPERIODE_ID, OverstyringType.Inntekt)
        assertTotrinnsvurdering(2.oppgave(VEDTAKSPERIODE_ID))
    }

    @Test
    fun `sak blir trukket til totrinnsvurdering ved overstyring av arbeidsforhold`() {
        fremTilSaksbehandleroppgave(andreArbeidsforhold = listOf(ORGNR_GHOST))
        håndterOverstyrArbeidsforhold(
            overstyrteArbeidsforhold = listOf(
                OverstyrArbeidsforholdHandlingFraApi.ArbeidsforholdFraApi(
                    orgnummer = ORGNR_GHOST,
                    deaktivert = true,
                    begrunnelse = "begrunnelse",
                    forklaring = "forklaring"
                )
            )
        )
        fremTilSaksbehandleroppgave(harOppdatertMetadata = true, harRisikovurdering = true)
        assertOverstyringer(VEDTAKSPERIODE_ID, OverstyringType.Arbeidsforhold)
        assertTotrinnsvurdering(2.oppgave(VEDTAKSPERIODE_ID))
    }

    @Test
    fun `sak blir trukket til totrinnsvurdering ved overstyring av tidslinje`() {
        fremTilSaksbehandleroppgave()
        håndterOverstyrTidslinje()
        fremTilSaksbehandleroppgave(harOppdatertMetadata = true, harRisikovurdering = true)
        assertOverstyringer(VEDTAKSPERIODE_ID, OverstyringType.Dager)
        assertTotrinnsvurdering(2.oppgave(VEDTAKSPERIODE_ID))
    }
}

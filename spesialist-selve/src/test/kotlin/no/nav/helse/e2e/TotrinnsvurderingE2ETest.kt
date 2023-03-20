package no.nav.helse.e2e

import AbstractE2ETestV2
import ToggleHelpers.disable
import ToggleHelpers.enable
import no.nav.helse.Testdata.ORGNR_GHOST
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.mediator.Toggle
import no.nav.helse.mediator.api.OverstyrArbeidsforholdDto
import no.nav.helse.spesialist.api.overstyring.OverstyringType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TotrinnsvurderingE2ETest : AbstractE2ETestV2() {

    @BeforeEach
    fun beforeEach() {
        Toggle.Totrinnsvurdering.disable()
    }

    @AfterEach
    fun afterEach() {
        Toggle.Totrinnsvurdering.enable()
    }

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
                OverstyrArbeidsforholdDto.ArbeidsforholdOverstyrt(
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

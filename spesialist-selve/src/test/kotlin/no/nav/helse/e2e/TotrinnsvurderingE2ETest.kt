package no.nav.helse.e2e

import AbstractE2ETest
import java.util.UUID
import no.nav.helse.Testdata.ORGNR_GHOST
import no.nav.helse.spesialist.api.overstyring.OverstyringType
import no.nav.helse.spesialist.api.saksbehandler.handlinger.LovhjemmelFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrArbeidsforholdHandlingFraApi
import org.junit.jupiter.api.Test

internal class TotrinnsvurderingE2ETest : AbstractE2ETest() {

    @Test
    fun `sak blir trukket til totrinnsvurdering ved overstyring av inntekt og refusjon`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        fremTilSaksbehandleroppgave()
        håndterOverstyrInntektOgRefusjon()
        fremTilSaksbehandleroppgave(
            harOppdatertMetadata = true,
            harRisikovurdering = true,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(utbetalingId = UUID.randomUUID()),
        )
        assertOverstyringer(VEDTAKSPERIODE_ID, OverstyringType.Inntekt)
        assertTotrinnsvurdering(2.oppgave(VEDTAKSPERIODE_ID))
    }

    @Test
    fun `sak blir trukket til totrinnsvurdering ved overstyring av arbeidsforhold`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        fremTilSaksbehandleroppgave(godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(
            orgnummereMedRelevanteArbeidsforhold = listOf(ORGNR_GHOST),
        ))
        håndterOverstyrArbeidsforhold(
            overstyrteArbeidsforhold = listOf(
                OverstyrArbeidsforholdHandlingFraApi.ArbeidsforholdFraApi(
                    orgnummer = ORGNR_GHOST,
                    deaktivert = true,
                    begrunnelse = "begrunnelse",
                    forklaring = "forklaring",
                    lovhjemmel = LovhjemmelFraApi("8-15", null, null, "folketrygdloven", "1998-12-18"),
                )
            )
        )
        fremTilSaksbehandleroppgave(
            harOppdatertMetadata = true,
            harRisikovurdering = true,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(utbetalingId = UUID.randomUUID()),
        )
        assertOverstyringer(VEDTAKSPERIODE_ID, OverstyringType.Arbeidsforhold)
        assertTotrinnsvurdering(2.oppgave(VEDTAKSPERIODE_ID))
    }

    @Test
    fun `sak blir trukket til totrinnsvurdering ved overstyring av tidslinje`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        fremTilSaksbehandleroppgave()
        håndterOverstyrTidslinje()
        fremTilSaksbehandleroppgave(
            harOppdatertMetadata = true,
            harRisikovurdering = true,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(utbetalingId = UUID.randomUUID()),
        )
        assertOverstyringer(VEDTAKSPERIODE_ID, OverstyringType.Dager)
        assertTotrinnsvurdering(2.oppgave(VEDTAKSPERIODE_ID))
    }
}

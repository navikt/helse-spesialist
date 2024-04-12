package no.nav.helse.e2e

import AbstractE2ETest
import no.nav.helse.Testdata.ORGNR_GHOST
import no.nav.helse.spesialist.api.overstyring.OverstyringType
import no.nav.helse.spesialist.api.saksbehandler.handlinger.LovhjemmelFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrArbeidsforholdHandlingFraApi
import org.junit.jupiter.api.Test
import java.util.UUID

internal class TotrinnsvurderingE2ETest : AbstractE2ETest() {

    @Test
    fun `sak blir trukket til totrinnsvurdering ved overstyring av inntekt og refusjon`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        håndterOverstyrInntektOgRefusjon()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(
            harRisikovurdering = true,
            harOppdatertStoppknapp = true,
            harOppdatertMetadata = true,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(utbetalingId = UUID.randomUUID()),
        )
        assertOverstyringer(VEDTAKSPERIODE_ID, OverstyringType.Inntekt)
        assertTotrinnsvurdering(2.oppgave(VEDTAKSPERIODE_ID))
    }

    @Test
    fun `sak blir trukket til totrinnsvurdering ved overstyring av arbeidsforhold`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(
                orgnummereMedRelevanteArbeidsforhold = listOf(ORGNR_GHOST),
            )
        )
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
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(
            harRisikovurdering = true,
            harOppdatertStoppknapp = true,
            harOppdatertMetadata = true,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(utbetalingId = UUID.randomUUID()),
        )
        assertOverstyringer(VEDTAKSPERIODE_ID, OverstyringType.Arbeidsforhold)
        assertTotrinnsvurdering(2.oppgave(VEDTAKSPERIODE_ID))
    }

    @Test
    fun `sak blir trukket til totrinnsvurdering ved overstyring av tidslinje`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        håndterOverstyrTidslinje()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(
            harRisikovurdering = true,
            harOppdatertStoppknapp = true,
            harOppdatertMetadata = true,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(utbetalingId = UUID.randomUUID()),
        )
        assertOverstyringer(VEDTAKSPERIODE_ID, OverstyringType.Dager)
        assertTotrinnsvurdering(2.oppgave(VEDTAKSPERIODE_ID))
    }
}

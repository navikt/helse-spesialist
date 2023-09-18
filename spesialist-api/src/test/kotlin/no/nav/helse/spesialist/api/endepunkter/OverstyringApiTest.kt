package no.nav.helse.spesialist.api.endepunkter

import io.ktor.http.HttpStatusCode
import java.util.UUID
import no.nav.helse.spesialist.api.AbstractE2ETest
import no.nav.helse.spesialist.api.februar
import no.nav.helse.spesialist.api.januar
import no.nav.helse.spesialist.api.saksbehandler.handlinger.LovhjemmelFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrArbeidsforholdHandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrArbeidsforholdHandlingFraApi.ArbeidsforholdFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrInntektOgRefusjonHandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrInntektOgRefusjonHandlingFraApi.OverstyrArbeidsgiverFraApi.RefusjonselementFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrTidslinjeHandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrTidslinjeHandlingFraApi.OverstyrDagFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.SkjønnsfastsettSykepengegrunnlagHandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.SkjønnsfastsettSykepengegrunnlagHandlingFraApi.SkjønnsfastsattArbeidsgiverFraApi.SkjønnsfastsettingstypeDto
import org.junit.jupiter.api.Test

internal class OverstyringApiTest: AbstractE2ETest() {

    @Test
    fun `overstyr tidslinje`() {
        val overstyring = OverstyrTidslinjeHandlingFraApi(
            vedtaksperiodeId = UUID.randomUUID(),
            organisasjonsnummer = ORGANISASJONSNUMMER,
            fødselsnummer = FØDSELSNUMMER,
            aktørId = AKTØR_ID,
            begrunnelse = "en begrunnelse",
            dager = listOf(
                OverstyrDagFraApi(dato = 10.januar, type = "Feriedag", fraType = "Sykedag", grad = null, fraGrad = 100, null)
            )
        )

        overstyrTidslinje(overstyring)

        assertSisteResponskode(HttpStatusCode.OK)
    }

    @Test
    fun `overstyr tidslinje til arbeidsdag`() {
        val overstyring = OverstyrTidslinjeHandlingFraApi(
            vedtaksperiodeId = UUID.randomUUID(),
            organisasjonsnummer = ORGANISASJONSNUMMER,
            fødselsnummer = FØDSELSNUMMER,
            aktørId = AKTØR_ID,
            begrunnelse = "en begrunnelse",
            dager = listOf(
                OverstyrDagFraApi(dato = 10.januar, type = "Arbeidsdag", fraType = "Sykedag", grad = null, fraGrad = 100, null)
            )
        )

        overstyrTidslinje(overstyring)

        assertSisteResponskode(HttpStatusCode.OK)
    }

    @Test
    fun `overstyr tidslinje fra arbeidsdag`() {
        val overstyring = OverstyrTidslinjeHandlingFraApi(
            vedtaksperiodeId = UUID.randomUUID(),
            organisasjonsnummer = ORGANISASJONSNUMMER,
            fødselsnummer = FØDSELSNUMMER,
            aktørId = AKTØR_ID,
            begrunnelse = "en begrunnelse",
            dager = listOf(
                OverstyrDagFraApi(dato = 10.januar, type = "Sykedag", fraType = "Arbeidsdag", grad = null, fraGrad = 100, null)
            )
        )

        overstyrTidslinje(overstyring)

        assertSisteResponskode(HttpStatusCode.OK)
    }

    @Test
    fun `overstyr arbeidsforhold`() {
        val overstyring = OverstyrArbeidsforholdHandlingFraApi(
            fødselsnummer = FØDSELSNUMMER,
            aktørId = AKTØR_ID,
            skjæringstidspunkt = 1.januar,
            overstyrteArbeidsforhold = listOf(
                ArbeidsforholdFraApi(
                    orgnummer = ORGANISASJONSNUMMER_GHOST,
                    deaktivert = true,
                    begrunnelse = "en begrunnelse",
                    forklaring = "en forklaring"
                )
            )
        )

        overstyrArbeidsforhold(overstyring)

        assertSisteResponskode(HttpStatusCode.OK)
    }

    @Test
    fun `overstyr inntekt og refusjon`() {
        val overstyring = OverstyrInntektOgRefusjonHandlingFraApi(
            fødselsnummer = FØDSELSNUMMER,
            aktørId = AKTØR_ID,
            skjæringstidspunkt = 1.januar,
            arbeidsgivere = listOf(
                OverstyrInntektOgRefusjonHandlingFraApi.OverstyrArbeidsgiverFraApi(
                    organisasjonsnummer = ORGANISASJONSNUMMER,
                    månedligInntekt = 25000.0,
                    fraMånedligInntekt = 25001.0,
                    refusjonsopplysninger = listOf(
                        RefusjonselementFraApi(1.januar, 31.januar, 25000.0),
                        RefusjonselementFraApi(1.februar, null, 24000.0),
                    ),
                    fraRefusjonsopplysninger = listOf(
                        RefusjonselementFraApi(1.januar, 31.januar, 24000.0),
                        RefusjonselementFraApi(1.februar, null, 23000.0),
                    ),
                    lovhjemmel = LovhjemmelFraApi("8-28", "3", null, "folketrygdloven", "1970-01-01"),
                    begrunnelse = "En begrunnelse",
                    forklaring = "En forklaring"
                ),
            )
        )

        overstyrInntektOgRefusjon(overstyring)

        assertSisteResponskode(HttpStatusCode.OK)
    }

    @Test
    fun `skjønnsfastsetting av sykepengegrunnlag`() {
        val skjonnsfastsetting = SkjønnsfastsettSykepengegrunnlagHandlingFraApi(
            fødselsnummer = FØDSELSNUMMER,
            aktørId = AKTØR_ID,
            skjæringstidspunkt = 1.januar,
            arbeidsgivere = listOf(
                SkjønnsfastsettSykepengegrunnlagHandlingFraApi.SkjønnsfastsattArbeidsgiverFraApi(
                    organisasjonsnummer = ORGANISASJONSNUMMER,
                    årlig = 250000.0,
                    fraÅrlig = 260000.0,
                    årsak = "En årsak",
                    type = SkjønnsfastsettingstypeDto.OMREGNET_ÅRSINNTEKT,
                    begrunnelseMal = "En begrunnelsemal",
                    begrunnelseFritekst = "begrunnelsefritekst",
                    begrunnelseKonklusjon = "En begrunnelsekonklusjon",
                    lovhjemmel = LovhjemmelFraApi("8-28", "3", null, "folketrygdloven", "1970-01-01"),
                    initierendeVedtaksperiodeId = UUID.randomUUID().toString(),
                ),
            )
        )

        skjønnsfastsettingSykepengegrunnlag(skjonnsfastsetting)

        assertSisteResponskode(HttpStatusCode.OK)
    }
}
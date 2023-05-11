package no.nav.helse.spesialist.api.endepunkter

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.AbstractE2ETest
import no.nav.helse.spesialist.api.februar
import no.nav.helse.spesialist.api.januar
import no.nav.helse.spesialist.api.overstyring.OverstyrArbeidsforholdDto
import no.nav.helse.spesialist.api.overstyring.OverstyrArbeidsforholdDto.ArbeidsforholdOverstyrt
import no.nav.helse.spesialist.api.overstyring.OverstyrArbeidsgiverDto
import no.nav.helse.spesialist.api.overstyring.OverstyrArbeidsgiverDto.RefusjonselementDto
import no.nav.helse.spesialist.api.overstyring.OverstyrArbeidsgiverDto.SubsumsjonDto
import no.nav.helse.spesialist.api.overstyring.OverstyrInntektOgRefusjonDto
import no.nav.helse.spesialist.api.overstyring.OverstyrTidslinjeDto
import no.nav.helse.spesialist.api.overstyring.OverstyrTidslinjeDto.OverstyrDagDto
import org.junit.jupiter.api.Test

internal class OverstyringApiTest: AbstractE2ETest() {

    @Test
    fun `overstyr tidslinje`() {
        val overstyring = OverstyrTidslinjeDto(
            organisasjonsnummer = ORGANISASJONSNUMMER,
            fødselsnummer = FØDSELSNUMMER,
            aktørId = AKTØR_ID,
            begrunnelse = "en begrunnelse",
            dager = listOf(
                OverstyrDagDto(dato = 10.januar, type = "Feriedag", fraType = "Sykedag", grad = null, fraGrad = 100)
            )
        )

        overstyrTidslinje(overstyring)

        assertSisteResponskode(HttpStatusCode.OK)
        assertSisteHendelse("saksbehandler_overstyrer_tidslinje")
    }

    @Test
    fun `overstyr tidslinje til arbeidsdag`() {
        val overstyring = OverstyrTidslinjeDto(
            organisasjonsnummer = ORGANISASJONSNUMMER,
            fødselsnummer = FØDSELSNUMMER,
            aktørId = AKTØR_ID,
            begrunnelse = "en begrunnelse",
            dager = listOf(
                OverstyrDagDto(dato = 10.januar, type = "Arbeidsdag", fraType = "Sykedag", grad = null, fraGrad = 100)
            )
        )

        overstyrTidslinje(overstyring)

        assertSisteResponskode(HttpStatusCode.OK)
        assertSisteHendelse("saksbehandler_overstyrer_tidslinje")
    }

    @Test
    fun `overstyr tidslinje fra arbeidsdag`() {
        val overstyring = OverstyrTidslinjeDto(
            organisasjonsnummer = ORGANISASJONSNUMMER,
            fødselsnummer = FØDSELSNUMMER,
            aktørId = AKTØR_ID,
            begrunnelse = "en begrunnelse",
            dager = listOf(
                OverstyrDagDto(dato = 10.januar, type = "Sykedag", fraType = "Arbeidsdag", grad = null, fraGrad = 100)
            )
        )

        overstyrTidslinje(overstyring)

        assertSisteResponskode(HttpStatusCode.OK)
        assertSisteHendelse("saksbehandler_overstyrer_tidslinje")
    }

    @Test
    fun `overstyr arbeidsforhold`() {
        val overstyring = OverstyrArbeidsforholdDto(
            fødselsnummer = FØDSELSNUMMER,
            aktørId = AKTØR_ID,
            skjæringstidspunkt = 1.januar,
            overstyrteArbeidsforhold = listOf(
                ArbeidsforholdOverstyrt(
                    orgnummer = ORGANISASJONSNUMMER_GHOST,
                    deaktivert = true,
                    begrunnelse = "en begrunnelse",
                    forklaring = "en forklaring"
                )
            )
        )

        overstyrArbeidsforhold(overstyring)

        assertSisteResponskode(HttpStatusCode.OK)
        assertSisteHendelse("saksbehandler_overstyrer_arbeidsforhold")
    }

    @Test
    fun `overstyr inntekt og refusjon`() {
        val overstyring = OverstyrInntektOgRefusjonDto(
            fødselsnummer = FØDSELSNUMMER,
            aktørId = AKTØR_ID,
            skjæringstidspunkt = 1.januar,
            arbeidsgivere = listOf(
                OverstyrArbeidsgiverDto(
                    organisasjonsnummer = ORGANISASJONSNUMMER,
                    månedligInntekt = 25000.0,
                    fraMånedligInntekt = 25001.0,
                    refusjonsopplysninger = listOf(
                        RefusjonselementDto(1.januar, 31.januar, 25000.0),
                        RefusjonselementDto(1.februar, null, 24000.0),
                    ),
                    fraRefusjonsopplysninger = listOf(
                        RefusjonselementDto(1.januar, 31.januar, 24000.0),
                        RefusjonselementDto(1.februar, null, 23000.0),
                    ),
                    subsumsjon = SubsumsjonDto("8-28", "3", null),
                    begrunnelse = "En begrunnelse",
                    forklaring = "En forklaring"
                ),
            )
        )

        overstyrInntektOgRefusjon(overstyring)

        assertSisteResponskode(HttpStatusCode.OK)
        assertSisteHendelse("saksbehandler_overstyrer_inntekt_og_refusjon")
    }
}
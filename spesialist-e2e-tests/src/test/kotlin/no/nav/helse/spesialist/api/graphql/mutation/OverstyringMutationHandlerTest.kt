package no.nav.helse.spesialist.api.graphql.mutation

import no.nav.helse.spesialist.api.AbstractOverstyringApiTest
import no.nav.helse.spesialist.api.februar
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidsforholdOverstyringHandling
import no.nav.helse.spesialist.api.graphql.schema.ApiInntektOgRefusjonOverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiOverstyringArbeidsforhold
import no.nav.helse.spesialist.api.graphql.schema.ApiOverstyringArbeidsgiver
import no.nav.helse.spesialist.api.graphql.schema.ApiOverstyringArbeidsgiver.ApiOverstyringRefusjonselement
import no.nav.helse.spesialist.api.graphql.schema.ApiOverstyringDag
import no.nav.helse.spesialist.api.graphql.schema.ApiTidslinjeOverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektOverstyring
import no.nav.helse.spesialist.api.januar
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.UUID

internal class OverstyringMutationHandlerTest : AbstractOverstyringApiTest() {
    @Test
    fun `overstyr tidslinje`() {
        val body =
            overstyrTidslinje(
                ApiTidslinjeOverstyring(
                    UUID.randomUUID(),
                    ORGANISASJONSNUMMER,
                    FØDSELSNUMMER,
                    AKTØRID,
                    "En begrunnelse",
                    listOf(
                        ApiOverstyringDag(10.januar, "Feriedag", "Sykedag", null, 100, null),
                    ),
                ),
            )
        Assertions.assertTrue(body["data"]["overstyrDager"].asBoolean())
    }

    @Test
    fun `overstyr tidslinje til arbeidsdag`() {
        val body =
            overstyrTidslinje(
                ApiTidslinjeOverstyring(
                    UUID.randomUUID(),
                    ORGANISASJONSNUMMER,
                    FØDSELSNUMMER,
                    AKTØRID,
                    "En begrunnelse",
                    listOf(
                        ApiOverstyringDag(10.januar, "Arbeidsdag", "Sykedag", null, 100, null),
                    ),
                ),
            )
        Assertions.assertTrue(body["data"]["overstyrDager"].asBoolean())
    }

    @Test
    fun `overstyr tidslinje fra arbeidsdag`() {
        val body =
            overstyrTidslinje(
                ApiTidslinjeOverstyring(
                    UUID.randomUUID(),
                    ORGANISASJONSNUMMER,
                    FØDSELSNUMMER,
                    AKTØRID,
                    "En begrunnelse",
                    listOf(
                        ApiOverstyringDag(10.januar, "Sykedag", "Arbeidsdag", null, 100, null),
                    ),
                ),
            )
        Assertions.assertTrue(body["data"]["overstyrDager"].asBoolean())
    }

    @Test
    fun `overstyr arbeidsforhold`() {
        val body =
            overstyrArbeidsforhold(
                ApiArbeidsforholdOverstyringHandling(
                    FØDSELSNUMMER,
                    AKTØRID,
                    10.januar,
                    listOf(
                        ApiOverstyringArbeidsforhold(ORGANISASJONSNUMMER_GHOST, true, "En begrunnelse", "En forklaring", null),
                    ),
                    vedtaksperiodeId = UUID.randomUUID(),
                ),
            )
        Assertions.assertTrue(body["data"]["overstyrArbeidsforhold"].asBoolean())
    }

    @Test
    fun `overstyr inntekt og refusjon`() {
        val body =
            overstyrInntektOgRefusjon(
                ApiInntektOgRefusjonOverstyring(
                    AKTØRID,
                    FØDSELSNUMMER,
                    9.januar,
                    listOf(
                        ApiOverstyringArbeidsgiver(
                            ORGANISASJONSNUMMER_GHOST,
                            24000.0,
                            25000.0,
                            listOf(
                                ApiOverstyringRefusjonselement(10.januar, 31.januar, 24000.0),
                                ApiOverstyringRefusjonselement(1.februar, null, 24000.0),
                            ),
                            listOf(
                                ApiOverstyringRefusjonselement(10.januar, 31.januar, 25000.0),
                                ApiOverstyringRefusjonselement(1.februar, null, 25000.0),
                            ),
                            "En begrunnelse",
                            "En forklaring",
                            null,
                            null,
                            null,
                        ),
                    ),
                    vedtaksperiodeId = UUID.randomUUID(),
                ),
            )
        Assertions.assertTrue(body["data"]["overstyrInntektOgRefusjon"].asBoolean())
    }

    @Test
    fun `overstyr tilkommen inntekt`() {
        val body =
            overstyrTilkommenInntekt(
                ApiTilkommenInntektOverstyring(
                    AKTØRID,
                    FØDSELSNUMMER,
                    vedtaksperiodeId = UUID.randomUUID(),
                    begrunnelse = "En begrunnelse",
                    lagtTilEllerEndret = listOf(
                        ApiTilkommenInntektOverstyring.ApiNyEllerEndretInntekt(
                            ORGANISASJONSNUMMER,
                            perioder = listOf(
                                ApiTilkommenInntektOverstyring.ApiNyEllerEndretInntekt.ApiPeriodeMedBeløp(1.januar, 31.januar, 24000.0),
                            )
                        )
                    ),
                    fjernet = listOf(
                        ApiTilkommenInntektOverstyring.ApiFjernetInntekt(
                            ORGANISASJONSNUMMER,
                            perioder = listOf(
                                ApiTilkommenInntektOverstyring.ApiFjernetInntekt.ApiPeriodeUtenBeløp(1.januar, 31.januar),
                            )
                        )
                    ),
                ),
            )
        Assertions.assertTrue(body["data"]["overstyrTilkommenInntekt"].asBoolean())
    }
}

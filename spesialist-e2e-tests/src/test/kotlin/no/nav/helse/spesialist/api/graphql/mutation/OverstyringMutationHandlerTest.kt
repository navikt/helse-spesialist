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
import no.nav.helse.spesialist.api.januar
import no.nav.helse.spesialist.test.lagOrganisasjonsnummer
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
                    ORGNUMMER,
                    FNR,
                    AKTØR,
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
                    ORGNUMMER,
                    FNR,
                    AKTØR,
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
                    ORGNUMMER,
                    FNR,
                    AKTØR,
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
                    FNR,
                    AKTØR,
                    10.januar,
                    listOf(
                        ApiOverstyringArbeidsforhold(lagOrganisasjonsnummer(), true, "En begrunnelse", "En forklaring", null),
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
                    AKTØR,
                    FNR,
                    9.januar,
                    listOf(
                        ApiOverstyringArbeidsgiver(
                            lagOrganisasjonsnummer(),
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
}

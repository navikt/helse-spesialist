package no.nav.helse.spesialist.api.graphql.mutation

import no.nav.helse.spesialist.api.AbstractOverstyringApiTest
import no.nav.helse.spesialist.api.februar
import no.nav.helse.spesialist.api.graphql.schema.ArbeidsforholdOverstyringHandling
import no.nav.helse.spesialist.api.graphql.schema.InntektOgRefusjonOverstyring
import no.nav.helse.spesialist.api.graphql.schema.OverstyringArbeidsforhold
import no.nav.helse.spesialist.api.graphql.schema.OverstyringArbeidsgiver
import no.nav.helse.spesialist.api.graphql.schema.OverstyringArbeidsgiver.OverstyringRefusjonselement
import no.nav.helse.spesialist.api.graphql.schema.OverstyringDag
import no.nav.helse.spesialist.api.graphql.schema.TidslinjeOverstyring
import no.nav.helse.spesialist.api.januar
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.UUID

internal class OverstyringMutationTest : AbstractOverstyringApiTest() {
    @Test
    fun `overstyr tidslinje`() {
        val body =
            overstyrTidslinje(
                TidslinjeOverstyring(
                    UUID.randomUUID(),
                    ORGANISASJONSNUMMER,
                    FØDSELSNUMMER,
                    AKTØRID,
                    "En begrunnelse",
                    listOf(
                        OverstyringDag(10.januar, "Feriedag", "Sykedag", null, 100, null),
                    ),
                ),
            )
        Assertions.assertTrue(body["data"]["overstyrDager"].asBoolean())
    }

    @Test
    fun `overstyr tidslinje til arbeidsdag`() {
        val body =
            overstyrTidslinje(
                TidslinjeOverstyring(
                    UUID.randomUUID(),
                    ORGANISASJONSNUMMER,
                    FØDSELSNUMMER,
                    AKTØRID,
                    "En begrunnelse",
                    listOf(
                        OverstyringDag(10.januar, "Arbeidsdag", "Sykedag", null, 100, null),
                    ),
                ),
            )
        Assertions.assertTrue(body["data"]["overstyrDager"].asBoolean())
    }

    @Test
    fun `overstyr tidslinje fra arbeidsdag`() {
        val body =
            overstyrTidslinje(
                TidslinjeOverstyring(
                    UUID.randomUUID(),
                    ORGANISASJONSNUMMER,
                    FØDSELSNUMMER,
                    AKTØRID,
                    "En begrunnelse",
                    listOf(
                        OverstyringDag(10.januar, "Sykedag", "Arbeidsdag", null, 100, null),
                    ),
                ),
            )
        Assertions.assertTrue(body["data"]["overstyrDager"].asBoolean())
    }

    @Test
    fun `overstyr arbeidsforhold`() {
        val body =
            overstyrArbeidsforhold(
                ArbeidsforholdOverstyringHandling(
                    FØDSELSNUMMER,
                    AKTØRID,
                    10.januar,
                    listOf(
                        OverstyringArbeidsforhold(ORGANISASJONSNUMMER_GHOST, true, "En begrunnelse", "En forklaring", null),
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
                InntektOgRefusjonOverstyring(
                    AKTØRID,
                    FØDSELSNUMMER,
                    9.januar,
                    listOf(
                        OverstyringArbeidsgiver(
                            ORGANISASJONSNUMMER_GHOST,
                            24000.0,
                            25000.0,
                            listOf(
                                OverstyringRefusjonselement(10.januar, 31.januar, 24000.0),
                                OverstyringRefusjonselement(1.februar, null, 24000.0),
                            ),
                            listOf(
                                OverstyringRefusjonselement(10.januar, 31.januar, 25000.0),
                                OverstyringRefusjonselement(1.februar, null, 25000.0),
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

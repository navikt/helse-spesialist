package no.nav.helse.spesialist.api.graphql.mutation

import no.nav.helse.TestRunner.runQuery
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidsforholdOverstyringHandling
import no.nav.helse.spesialist.api.graphql.schema.ApiInntektOgRefusjonOverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiOverstyringArbeidsforhold
import no.nav.helse.spesialist.api.graphql.schema.ApiOverstyringArbeidsgiver
import no.nav.helse.spesialist.api.graphql.schema.ApiOverstyringArbeidsgiver.ApiOverstyringRefusjonselement
import no.nav.helse.spesialist.api.graphql.schema.ApiOverstyringDag
import no.nav.helse.spesialist.api.graphql.schema.ApiTidslinjeOverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektOverstyring
import no.nav.helse.spesialist.api.testfixtures.mutation.overstyrArbeidsforholdMutation
import no.nav.helse.spesialist.api.testfixtures.mutation.overstyrInntektOgRefusjonMutation
import no.nav.helse.spesialist.api.testfixtures.mutation.overstyrTidslinjeMutation
import no.nav.helse.spesialist.api.testfixtures.mutation.overstyrTilkommenInntektMutation
import no.nav.helse.spesialist.domain.testfixtures.feb
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagAktørId
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.UUID

internal class OverstyringMutationHandlerTest {
    @Test
    fun `overstyr tidslinje`() {
        runQuery(
            whenever = overstyrTidslinjeMutation(
                ApiTidslinjeOverstyring(
                    UUID.randomUUID(),
                    lagOrganisasjonsnummer(),
                    lagFødselsnummer(),
                    lagAktørId(),
                    "En begrunnelse",
                    listOf(
                        ApiOverstyringDag(10 jan 2018, "Feriedag", "Sykedag", null, 100, null),
                    ),
                )
            ),
            then = { _, body, _ ->
                Assertions.assertTrue(body["data"]["overstyrDager"].asBoolean())
            },
        )
    }

    @Test
    fun `overstyr tidslinje til arbeidsdag`() {
        runQuery(
            whenever = overstyrTidslinjeMutation(
                ApiTidslinjeOverstyring(
                    UUID.randomUUID(),
                    lagOrganisasjonsnummer(),
                    lagFødselsnummer(),
                    lagAktørId(),
                    "En begrunnelse",
                    listOf(
                        ApiOverstyringDag(10 jan 2018, "Arbeidsdag", "Sykedag", null, 100, null),
                    ),
                )
            ),
            then = { _, body, _ ->
                Assertions.assertTrue(body["data"]["overstyrDager"].asBoolean())
            },
        )
    }

    @Test
    fun `overstyr tidslinje fra arbeidsdag`() {
        runQuery(
            whenever = overstyrTidslinjeMutation(
                ApiTidslinjeOverstyring(
                    UUID.randomUUID(),
                    lagOrganisasjonsnummer(),
                    lagFødselsnummer(),
                    lagAktørId(),
                    "En begrunnelse",
                    listOf(
                        ApiOverstyringDag(10 jan 2018, "Sykedag", "Arbeidsdag", null, 100, null),
                    ),
                )
            ),
            then = { _, body, _ ->
                Assertions.assertTrue(body["data"]["overstyrDager"].asBoolean())
            },
        )
    }

    @Test
    fun `overstyr arbeidsforhold`() {
        runQuery(
            whenever = overstyrArbeidsforholdMutation(
                ApiArbeidsforholdOverstyringHandling(
                    lagFødselsnummer(),
                    lagAktørId(),
                    10 jan 2018,
                    listOf(
                        ApiOverstyringArbeidsforhold(
                            lagOrganisasjonsnummer(),
                            true,
                            "En begrunnelse",
                            "En forklaring",
                            null
                        ),
                    ),
                    vedtaksperiodeId = UUID.randomUUID(),
                )
            ),
            then = { _, body, _ ->
                Assertions.assertTrue(body["data"]["overstyrArbeidsforhold"].asBoolean())
            },
        )
    }

    @Test
    fun `overstyr inntekt og refusjon`() {
        runQuery(
            whenever = overstyrInntektOgRefusjonMutation(
                ApiInntektOgRefusjonOverstyring(
                    lagAktørId(),
                    lagFødselsnummer(),
                    9 jan 2018,
                    listOf(
                        ApiOverstyringArbeidsgiver(
                            lagOrganisasjonsnummer(),
                            24000.0,
                            25000.0,
                            listOf(
                                ApiOverstyringRefusjonselement(10 jan 2018, 31 jan 2018, 24000.0),
                                ApiOverstyringRefusjonselement(1 feb 2018, null, 24000.0),
                            ),
                            listOf(
                                ApiOverstyringRefusjonselement(10 jan 2018, 31 jan 2018, 25000.0),
                                ApiOverstyringRefusjonselement(1 feb 2018, null, 25000.0),
                            ),
                            "En begrunnelse",
                            "En forklaring",
                            null,
                            null,
                            null,
                        ),
                    ),
                    vedtaksperiodeId = UUID.randomUUID(),
                )
            ),
            then = { _, body, _ ->
                Assertions.assertTrue(body["data"]["overstyrInntektOgRefusjon"].asBoolean())
            },
        )
    }

    @Test
    fun `overstyr tilkommen inntekt`() {
        val organisasjonsnummer = lagOrganisasjonsnummer()
        runQuery(
            whenever = overstyrTilkommenInntektMutation(
                ApiTilkommenInntektOverstyring(
                    lagAktørId(),
                    lagFødselsnummer(),
                    vedtaksperiodeId = UUID.randomUUID(),
                    notatTilBeslutter = "En begrunnelse",
                    lagtTilEllerEndret = listOf(
                        ApiTilkommenInntektOverstyring.ApiNyEllerEndretInntekt(
                            organisasjonsnummer,
                            perioder = listOf(
                                ApiTilkommenInntektOverstyring.ApiNyEllerEndretInntekt.ApiPeriodeMedBeløp(
                                    1 jan 2018,
                                    31 jan 2018,
                                    24000.0
                                ),
                            )
                        )
                    ),
                    fjernet = listOf(
                        ApiTilkommenInntektOverstyring.ApiFjernetInntekt(
                            organisasjonsnummer,
                            perioder = listOf(
                                ApiTilkommenInntektOverstyring.ApiFjernetInntekt.ApiPeriodeUtenBeløp(
                                    1 jan 2018,
                                    31 jan 2018
                                ),
                            )
                        )
                    ),
                )
            ),
            then = { _, body, _ ->
                Assertions.assertTrue(body["data"]["overstyrTilkommenInntekt"].asBoolean())
            },
        )
    }
}

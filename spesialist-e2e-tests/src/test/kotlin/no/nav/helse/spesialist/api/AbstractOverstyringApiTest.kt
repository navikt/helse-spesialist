package no.nav.helse.spesialist.api

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidsforholdOverstyringHandling
import no.nav.helse.spesialist.api.graphql.schema.ApiInntektOgRefusjonOverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiTidslinjeOverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektOverstyring
import no.nav.helse.spesialist.api.testfixtures.mutation.overstyrArbeidsforholdMutation
import no.nav.helse.spesialist.api.testfixtures.mutation.overstyrInntektOgRefusjonMutation
import no.nav.helse.spesialist.api.testfixtures.mutation.overstyrTidslinjeMutation
import no.nav.helse.spesialist.api.testfixtures.mutation.overstyrTilkommenInntektMutation

internal abstract class AbstractOverstyringApiTest : AbstractGraphQLApiTest() {
    fun overstyrTidslinje(overstyring: ApiTidslinjeOverstyring): JsonNode =
        runQuery(
            overstyrTidslinjeMutation(overstyring),
        )

    fun overstyrArbeidsforhold(overstyring: ApiArbeidsforholdOverstyringHandling): JsonNode =
        runQuery(
            overstyrArbeidsforholdMutation(overstyring),
        )

    fun overstyrInntektOgRefusjon(overstyring: ApiInntektOgRefusjonOverstyring): JsonNode =
        runQuery(
            overstyrInntektOgRefusjonMutation(overstyring),
        )

    fun overstyrTilkommenInntekt(overstyring: ApiTilkommenInntektOverstyring): JsonNode =
        runQuery(
            overstyrTilkommenInntektMutation(overstyring),
        )
}

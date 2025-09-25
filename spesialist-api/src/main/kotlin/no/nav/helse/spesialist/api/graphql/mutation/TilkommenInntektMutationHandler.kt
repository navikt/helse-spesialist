package no.nav.helse.spesialist.api.graphql.mutation

import graphql.schema.DataFetchingEnvironment
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektInput
import java.util.UUID

class TilkommenInntektMutationHandler : TilkommenInntektMutationSchema {
    override fun leggTilTilkommenInntekt(
        fodselsnummer: String,
        verdier: ApiTilkommenInntektInput,
        notatTilBeslutter: String,
        env: DataFetchingEnvironment,
    ) = error("Kall til denne er ikke forventet - den eksisterer kun midlertidig for typegenerering")

    override fun endreTilkommenInntekt(
        tilkommenInntektId: UUID,
        endretTil: ApiTilkommenInntektInput,
        notatTilBeslutter: String,
        env: DataFetchingEnvironment,
    ) = error("Kall til denne er ikke forventet - den eksisterer kun midlertidig for typegenerering")

    override fun fjernTilkommenInntekt(
        tilkommenInntektId: UUID,
        notatTilBeslutter: String,
        env: DataFetchingEnvironment,
    ) = error("Kall til denne er ikke forventet - den eksisterer kun midlertidig for typegenerering")

    override fun gjenopprettTilkommenInntekt(
        tilkommenInntektId: UUID,
        endretTil: ApiTilkommenInntektInput,
        notatTilBeslutter: String,
        env: DataFetchingEnvironment,
    ) = error("Kall til denne er ikke forventet - den eksisterer kun midlertidig for typegenerering")
}

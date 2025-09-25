package no.nav.helse.spesialist.api.graphql.query

import graphql.schema.DataFetchingEnvironment

class TilkommenInntektQueryHandler : TilkommenInntektQuerySchema {
    override suspend fun tilkomneInntektskilder(
        aktorId: String,
        env: DataFetchingEnvironment,
    ) = error("Kall til denne er ikke forventet - den eksisterer kun midlertidig for typegenerering")

    override suspend fun tilkomneInntektskilderV2(
        fodselsnummer: String,
        env: DataFetchingEnvironment,
    ) = error("Kall til denne er ikke forventet - den eksisterer kun midlertidig for typegenerering")
}

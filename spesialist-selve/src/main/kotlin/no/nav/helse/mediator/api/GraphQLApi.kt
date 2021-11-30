package no.nav.helse.mediator.api

import com.apurebase.kgraphql.GraphQL
import io.ktor.application.*
import io.ktor.auth.*

internal fun Application.installGraphQLApi() {
    install(GraphQL) {
        endpoint = "/api/graphql"
        playground = true

        wrap {
            authenticate("oidc", build = it)
        }

        schema {
            query("person") {
                resolver { fnr: String ->
                    // Opprett et personobjekt og populer med data fra databasen.
                }
            }
        }
    }
}

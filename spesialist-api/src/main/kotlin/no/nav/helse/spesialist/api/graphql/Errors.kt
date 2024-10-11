package no.nav.helse.spesialist.api.graphql

import graphql.GraphQLError
import graphql.GraphqlErrorException

internal fun forbiddenError(fødselsnummer: String): GraphQLError =
    GraphqlErrorException.newErrorException()
        .message("Har ikke tilgang til person med fødselsnummer $fødselsnummer")
        .extensions(mapOf("code" to 403, "field" to "person"))
        .build()

internal fun notFoundError(identifikator: String? = null): GraphQLError =
    GraphqlErrorException.newErrorException()
        .message("Finner ikke data for person med identifikator $identifikator")
        .extensions(mapOf("code" to 404, "field" to "person"))
        .build()

internal fun personNotReadyError(
    fødselsnummer: String,
    aktørId: String,
    personKlargjøres: Boolean,
): GraphQLError =
    GraphqlErrorException.newErrorException()
        .message("Person med fødselsnummer $fødselsnummer er ikke klar for visning ennå")
        .extensions(
            mutableMapOf("code" to 409, "field" to "person").apply {
                if (personKlargjøres) this["persondata_hentes_for"] = aktørId
            }.toMap(),
        )
        .build()

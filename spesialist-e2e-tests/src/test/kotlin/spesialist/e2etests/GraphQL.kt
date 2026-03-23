package no.nav.helse.spesialist.e2etests

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import kotlin.test.assertNull

object GraphQL {
    fun call(
        operationName: String,
        saksbehandler: Saksbehandler,
        tilganger: Set<Tilgang>,
        brukerroller: Set<Brukerrolle>,
        variables: Map<String, Any>,
    ): JsonNode =
        REST.post(
            relativeUrl = "graphql",
            saksbehandler = saksbehandler,
            tilganger = tilganger,
            brukerroller = brukerroller,
            request = mapOf(
                "query" to (
                        this::class.java.getResourceAsStream("/graphql/$operationName.graphql")
                            ?.use { it.reader().readText() }
                            ?: error("Fant ikke $operationName.graphql")
                        ),
                "operationName" to operationName,
                "variables" to variables,
            ),
        )!!.also { assertNull(it["errors"], "Fikk feil i GraphQL-response") }
}

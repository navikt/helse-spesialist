package no.nav.helse.db

import tools.jackson.databind.JsonNode
import java.util.UUID

interface DokumentDao {
    fun lagre(
        fødselsnummer: String,
        dokumentId: UUID,
        dokument: JsonNode,
    )

    fun hent(
        fødselsnummer: String,
        dokumentId: UUID,
    ): JsonNode?

    fun slettGamleDokumenter(): Int
}

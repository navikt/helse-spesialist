package no.nav.helse.spesialist.application

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.DokumentDao
import java.util.UUID

class InMemoryDokumentDao : DokumentDao {
    val dokumenter = mutableMapOf<Pair<String, UUID>, JsonNode>()

    override fun lagre(
        fødselsnummer: String,
        dokumentId: UUID,
        dokument: JsonNode
    ) {
        dokumenter[fødselsnummer to dokumentId] = dokument
    }

    override fun hent(
        fødselsnummer: String,
        dokumentId: UUID
    ): JsonNode? {
        return dokumenter[fødselsnummer to dokumentId]
    }

    override fun slettGamleDokumenter(): Int {
        TODO("Not yet implemented")
    }
}

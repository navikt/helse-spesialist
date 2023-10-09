package no.nav.helse.mediator.dokument

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.UUID
import no.nav.helse.spesialist.api.Dokumenthåndterer

class DokumentMediator: Dokumenthåndterer {
    override fun håndter(fødselsnummer: String, dokumentId: UUID): JsonNode {
        // TODO: Implementer denne!!
        return ObjectMapper().createObjectNode()
    }
}
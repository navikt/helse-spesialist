package no.nav.helse.spesialist.api

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

interface Dokumenthåndterer {
    fun håndter(fødselsnummer: String, dokumentId: UUID, dokumentType: String): JsonNode
}


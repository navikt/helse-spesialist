package no.nav.helse.spesialist.api

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.DokumentDao
import java.util.UUID

interface Dokumenthåndterer {
    fun håndter(
        fødselsnummer: String,
        dokumentId: UUID,
        dokumentType: String,
    ): JsonNode

    fun håndter(
        dokumentDao: DokumentDao,
        fødselsnummer: String,
        dokumentId: UUID,
        dokumentType: String,
    ): JsonNode
}

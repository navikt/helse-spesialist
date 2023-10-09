package no.nav.helse.spesialist.api

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
import no.nav.helse.spesialist.api.vedtak.GodkjenningDto

interface Dokumenthåndterer {
    fun håndter(fødselsnummer: String, dokumentId: UUID): JsonNode
}


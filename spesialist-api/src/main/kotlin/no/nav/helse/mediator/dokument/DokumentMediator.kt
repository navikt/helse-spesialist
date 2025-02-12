package no.nav.helse.mediator.dokument

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.helse.MeldingPubliserer
import no.nav.helse.db.DokumentDao
import no.nav.helse.modell.melding.HentDokument
import no.nav.helse.spesialist.api.Dokumenthåndterer
import no.nav.helse.spesialist.api.objectMapper
import java.util.UUID

class DokumentMediator(
    private val dokumentDao: DokumentDao,
    private val publiserer: MeldingPubliserer,
    private val retries: Int = 50,
) : Dokumenthåndterer {
    override fun håndter(
        fødselsnummer: String,
        dokumentId: UUID,
        dokumentType: String,
    ): JsonNode {
        return dokumentDao.hent(fødselsnummer, dokumentId).let { dokument ->
            val erTom = dokument?.size() == 0
            val error = dokument?.path("error")?.takeUnless { it.isMissingNode || it.isNull }?.asInt()
            if (dokument == null || erTom || (error != null && error != 404)) {
                sendHentDokument(fødselsnummer, dokumentId, dokumentType)

                val response =
                    runBlocking {
                        delay(100)
                        hentDokument(fødselsnummer, dokumentId, retries)
                    }
                response
            } else {
                dokument
            }
        }
    }

    private fun hentDokument(
        fødselsnummer: String,
        dokumentId: UUID,
        retries: Int,
    ): JsonNode {
        if (retries == 0) return objectMapper.createObjectNode().put("error", 408)

        val response =
            runBlocking {
                val dokument = dokumentDao.hent(fødselsnummer, dokumentId)
                if (dokument == null) {
                    delay(100)
                    hentDokument(fødselsnummer, dokumentId, retries - 1)
                } else {
                    dokument
                }
            }
        return response
    }

    private fun sendHentDokument(
        fødselsnummer: String,
        dokumentId: UUID,
        dokumentType: String,
    ) {
        publiserer.publiser(
            fødselsnummer = fødselsnummer,
            hendelse = HentDokument(dokumentId = dokumentId, dokumentType = dokumentType),
            årsak = "forespørsel om $dokumentType fra saksbehandler",
        )
    }
}

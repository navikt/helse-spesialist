package no.nav.helse.mediator.dokument

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.helse.MeldingPubliserer
import no.nav.helse.db.DokumentDao
import no.nav.helse.modell.melding.HentDokument
import java.util.UUID

class DokumentMediator(
    private val dokumentDao: DokumentDao,
    private val publiserer: MeldingPubliserer,
    private val retries: Int = 50,
) {
    fun håndter(
        fødselsnummer: String,
        dokumentId: UUID,
        dokumentType: String,
    ): JsonNode = håndter(dokumentDao, fødselsnummer, dokumentId, dokumentType)

    fun håndter(
        dokumentDao: DokumentDao,
        fødselsnummer: String,
        dokumentId: UUID,
        dokumentType: String,
    ): JsonNode {
        val dokument = dokumentDao.hent(fødselsnummer, dokumentId)

        if (dokument == null || dokument.isEmpty || dokument.harFeil()) {
            sendHentDokument(fødselsnummer, dokumentId, dokumentType)
            return hentDokumentMedRetry(fødselsnummer, dokumentId, retries)
        }
        return dokument
    }

    private fun JsonNode.harFeil(): Boolean {
        val errorNode = this.get("error") ?: return false
        return errorNode.asInt() != 404
    }

    private fun hentDokumentMedRetry(
        fødselsnummer: String,
        dokumentId: UUID,
        retries: Int,
    ): JsonNode =
        runBlocking {
            repeat(retries) {
                delay(100)
                dokumentDao.hent(fødselsnummer, dokumentId)?.let { return@runBlocking it }
            }
            error("Fant ikke dokument med id $dokumentId")
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

    enum class DokumentType {
        SØKNAD,
        INNTEKTSMELDING,
    }
}

package no.nav.helse.spesialist.api.rest

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.MeldingPubliserer
import no.nav.helse.db.DokumentDao
import no.nav.helse.modell.melding.HentDokument
import java.util.UUID

class DokumentMediator(
    private val publiserer: MeldingPubliserer,
    private val retries: Int = 50,
) {
    fun hentDokument(
        dokumentDao: DokumentDao,
        fødselsnummer: String,
        dokumentId: UUID,
        dokumentType: DokumentType,
    ): JsonNode? {
        val dokument = dokumentDao.hent(fødselsnummer, dokumentId)

        if (dokument == null || dokument.isEmpty || dokument.harFeil()) {
            sendHentDokument(fødselsnummer, dokumentId, dokumentType)
            return hentDokumentMedRetry(dokumentDao, fødselsnummer, dokumentId, retries)
        }
        return dokument
    }

    private fun JsonNode.harFeil(): Boolean = get("error") != null

    private fun hentDokumentMedRetry(
        dokumentDao: DokumentDao,
        fødselsnummer: String,
        dokumentId: UUID,
        retries: Int,
    ): JsonNode? {
        repeat(retries) {
            Thread.sleep(100L)
            val dokument = dokumentDao.hent(fødselsnummer, dokumentId)
            if (dokument != null && !dokument.isEmpty && !dokument.harFeil()) {
                return dokument
            }
        }
        return null
    }

    private fun sendHentDokument(
        fødselsnummer: String,
        dokumentId: UUID,
        dokumentType: DokumentType,
    ) {
        publiserer.publiser(
            fødselsnummer = fødselsnummer,
            hendelse = HentDokument(dokumentId = dokumentId, dokumentType = dokumentType.name),
            årsak = "forespørsel om ${dokumentType.name.lowercase()} fra saksbehandler",
        )
    }

    enum class DokumentType {
        SØKNAD,
        INNTEKTSMELDING,
    }
}

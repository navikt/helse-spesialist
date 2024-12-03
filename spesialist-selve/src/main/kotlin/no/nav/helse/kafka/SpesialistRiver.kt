package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import org.slf4j.LoggerFactory

internal sealed interface SpesialistRiver : River.PacketListener {
    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
        private val logg = LoggerFactory.getLogger(this::class.java)
    }

    fun preconditions(): River.PacketValidation

    fun validations(): River.PacketValidation

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        logg.error("Melding passerte ikke validering i river {}. Se sikkerlogg for mer informasjon", this::class.simpleName)
        sikkerlogg.error("Meldingen passerte ikke validering i river {}. {}", this::class.simpleName, problems.toExtendedReport())
    }
}

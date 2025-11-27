package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.application.logg.sikkerlogg

sealed interface SpesialistRiver : River.PacketListener {
    fun preconditions(): River.PacketValidation

    fun validations(): River.PacketValidation

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        logg.error("Melding passerte ikke validering i river {}. Se sikkerlogg for mer informasjon", this::class.simpleName)
        sikkerlogg.error("Meldingen passerte ikke validering i river {}. {}", this::class.simpleName, problems.toExtendedReport())
        error("Melding passerte ikke validering i river ${this::class.simpleName}, ${problems.toExtendedReport()}")
    }
}

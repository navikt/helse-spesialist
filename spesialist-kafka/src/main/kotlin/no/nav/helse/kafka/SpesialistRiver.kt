package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import no.nav.helse.spesialist.application.logg.loggErrorWithNoThrowable

sealed interface SpesialistRiver : River.PacketListener {
    fun preconditions(): River.PacketValidation

    fun validations(): River.PacketValidation

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        loggErrorWithNoThrowable("Melding passerte ikke validering i river ${this::class.simpleName}", "problems:\n${problems.toExtendedReport()}")
        error("Melding passerte ikke validering i river ${this::class.simpleName}, ${problems.toExtendedReport()}")
    }
}

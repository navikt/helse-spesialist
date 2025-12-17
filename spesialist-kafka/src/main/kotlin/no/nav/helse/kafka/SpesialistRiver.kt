package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.loggErrorWithNoThrowable
import java.util.UUID

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

sealed class TransaksjonellRiver : SpesialistRiver {
    abstract fun transaksjonellOnPacket(
        packet: JsonMessage,
        outbox: Outbox,
        transaksjon: SessionContext,
        eventMetadata: EventMetadata,
    )

    final override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        error("Ikke støttet ved bruk av transaksjonell river")
    }
}

data class EventMetadata(
    val name: EventName,
    val `@id`: UUID,
)

sealed interface EventName {
    class Behov(
        val behovene: List<String>,
    ) : EventName {
        override fun toString(): String = "behov: " + behovene.joinToString(", ", prefix = "[", postfix = "]")
    }

    class Hendelse(
        val navn: String,
    ) : EventName {
        override fun toString(): String = navn
    }
}

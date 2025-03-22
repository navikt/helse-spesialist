package no.nav.helse.spesialist.e2etests

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.spesialist.application.logg.logg

abstract class AbstractMockRiver : River.PacketListener {
    abstract fun precondition(jsonMessage: JsonMessage)

    abstract val validateKeys: Set<String>

    abstract fun responseFor(packet: JsonMessage): String

    fun registerOn(rapid: SimulatingTestRapid) {
        River(rapid)
            .precondition(::precondition)
            .validate { validateKeys.forEach { key -> it.requireKey(key) } }
            .register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        val response = responseFor(packet)
        logg.info("${this.javaClass.simpleName} publiserer simulert svarmelding fra ekstern tjeneste: $response")
        context.publish(response)
    }
}

package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import org.slf4j.LoggerFactory

class MinuttRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    private companion object {
        private val logg = LoggerFactory.getLogger(this::class.java)
    }

    override fun preconditions(): River.PacketValidation =
        River.PacketValidation {
            it.requireAny("@event_name", listOf("minutt"))
        }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("@id")
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val antallOppdaterteMedArbeidstaker = mediator.oppdaterBehandlingMedYrkesaktivitetArbeidstaker()
        val antallOppdaterteMedSelvstendig = mediator.oppdaterBehandlingMedYrkesaktivitetSelvstendig()
        logg.info("Oppdatert $antallOppdaterteMedArbeidstaker oppgaver med egenskap ARBEIDSTAKER og $antallOppdaterteMedSelvstendig med SELVSTENDIG")
    }
}

package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.oppgave.OppgaveRepository
import no.nav.helse.spesialist.application.logg.logg

private const val CHUNK_SIZE = 1000

class MinuttRiver(
    private val oppgaveRepository: OppgaveRepository,
) : SpesialistRiver {
    override fun preconditions(): River.PacketValidation =
        River.PacketValidation {
            it.requireAny("@event_name", listOf("minutt"))
        }

    override fun validations() = River.PacketValidation { }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        logg.info("Finner opptil $CHUNK_SIZE oppgaver uten første_opprettet")
        val oppgaver = oppgaveRepository.finnFlereAvventerSaksbehandlerUtenFørsteOpprettet(CHUNK_SIZE)
        logg.info("Hentet ${oppgaver.size} oppgaver uten første_opprettet")
        oppgaver.forEach {
            it.førsteOpprettet = oppgaveRepository.førsteOpprettetForBehandlingId(it.behandlingId) ?: it.opprettet
            oppgaveRepository.lagre(it)
        }
        logg.info("Oppdaterte ${oppgaver.size} oppgaver med første_opprettet")
    }
}

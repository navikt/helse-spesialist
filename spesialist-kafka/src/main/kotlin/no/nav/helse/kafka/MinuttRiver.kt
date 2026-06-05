package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.db.SessionFactory
import no.nav.helse.modell.person.vedtaksperiode.Varselkode
import no.nav.helse.spesialist.application.logg.loggInfo

class MinuttRiver(
    private val sessionFactory: SessionFactory,
) : SpesialistRiver {
    override fun preconditions(): River.PacketValidation =
        River.PacketValidation {
            it.requireValue("@event_name", "minutt")
        }

    override fun validations(): River.PacketValidation = River.PacketValidation {}

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val fødselsnumre =
            sessionFactory.transactionalSessionScope { sessionContext ->
                sessionContext.oppgaveRepository.finnFødselsnumreForÅpneOppgaverMedAktivtVarsel(Varselkode.SB_EX_3.name)
            }

        if (fødselsnumre.isEmpty()) return

        loggInfo("Trigger sjekk av Gosys-oppgaver på nytt for ${fødselsnumre.size} oppgave(r) med varsel om feil ved oppslag av Gosys-oppgaver")
        fødselsnumre.forEach { fødselsnummer ->
            val melding =
                JsonMessage
                    .newMessage(
                        "gosys_oppgave_endret",
                        mapOf("fødselsnummer" to fødselsnummer),
                    ).toJson()
            context.publish(fødselsnummer, melding)
        }
    }
}

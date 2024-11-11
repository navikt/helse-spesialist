package no.nav.helse.kafka

import kotliquery.TransactionalSession
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.meldinger.Melding
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.toUUID
import java.util.UUID

internal class HentArbeidsgivernavnRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    override fun validations() =
        River.PacketValidation {
            it.demandValue("@event_name", "innhent_arbeidsgivernavn")
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        mediator.behandleInnhentArbeidsgivernavn(InnhentArbeidsgivernavn(packet), context)
    }
}

internal class InnhentArbeidsgivernavn(private val packet: JsonMessage) : Melding {
    override val id: UUID = packet.id.toUUID()

    fun behandle(
        kommandostarter: Kommandostarter,
        transactionalSession: TransactionalSession,
    ) {
        kommandostarter { innhentArbeidsgivernavn(transactionalSession) }
    }

    override fun toJson() = packet.toJson()
}

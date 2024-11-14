package no.nav.helse.kafka

import kotliquery.TransactionalSession
import no.nav.helse.kafka.InnhentArbeidsgivernavn.Companion.opprettFra
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.meldinger.Melding
import no.nav.helse.modell.CommandData
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
            it.requireKey("batchSize")
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        mediator.behandleInnhentArbeidsgivernavn(opprettFra(packet), context)
    }
}

internal class InnhentArbeidsgivernavn
    private constructor(override val id: UUID, private val data: String, val batchSize: Int) : Melding, CommandData {
        fun behandle(
            kommandostarter: Kommandostarter,
            transactionalSession: TransactionalSession,
        ) {
            kommandostarter { innhentArbeidsgivernavn(batchSize, transactionalSession) }
        }

        override fun toJson() = data

        override fun data() = data

        companion object {
            fun opprettFra(packet: JsonMessage) =
                InnhentArbeidsgivernavn(packet.id.toUUID(), packet.toJson(), packet["batchSize"].intValue())
        }
    }

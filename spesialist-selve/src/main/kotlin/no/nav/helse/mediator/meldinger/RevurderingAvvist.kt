package no.nav.helse.mediator.meldinger

import no.nav.helse.abonnement.OpptegnelseType
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.opptegnelse.OpptegnelseDao
import no.nav.helse.modell.opptegnelse.RevurderingAvvistPayload
import no.nav.helse.rapids_rivers.*
import no.nav.helse.rapids_rivers.River.PacketListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

internal class RevurderingAvvist(
    override val id: UUID,
    private val fødselsnummer: String,
    private val errors: List<String>,
    private val json: String,
    private val opptegnelseDao: OpptegnelseDao
) : Hendelse, Command {

    override fun fødselsnummer(): String = fødselsnummer
    override fun vedtaksperiodeId(): UUID? = null
    override fun toJson(): String = json

    internal class River(
        rapidsConnection: RapidsConnection,
        private val mediator: HendelseMediator
    ) : PacketListener {
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "revurdering_avvist")
                    it.requireKey(
                        "fødselsnummer", "errors"
                    )
                }
            }.register(this)
        }

        override fun onError(problems: MessageProblems, context: MessageContext) {
            sikkerLogg.error("Forstod ikke revurdering_avvist:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            sikkerLogg.info("Mottok melding om revurdering avvist")

            val fødselsnummer = packet["fødselsnummer"].asText()
            val errors = packet["errors"].map { it.asText() }
            mediator.revurderingAvvist(fødselsnummer, errors, packet.toJson(), context)
        }
    }

    override fun execute(context: CommandContext): Boolean {
        opptegnelseDao.opprettOpptegnelse(
            fødselsnummer,
            RevurderingAvvistPayload(id, errors),
            OpptegnelseType.REVURDERING_AVVIST
        )
        return true
    }
}

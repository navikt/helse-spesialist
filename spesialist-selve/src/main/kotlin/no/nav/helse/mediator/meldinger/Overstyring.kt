package no.nav.helse.mediator.meldinger

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.kommando.*
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.overstyring.OverstyringDagDto
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.reservasjon.ReservasjonDao
import no.nav.helse.saksbehandler.SaksbehandlerDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Tar vare på overstyring fra saksbehandler og sletter den opprinnelige oppgaven i påvente av nytt
 * godkjenningsbehov fra spleis.
 *
 * Det er primært spleis som håndterer dette eventet.
 */
internal class Overstyring(
    override val id: UUID,
    private val fødselsnummer: String,
    oid: UUID,
    navn: String,
    epost: String,
    ident: String,
    orgnummer: String,
    begrunnelse: String,
    overstyrteDager: List<OverstyringDagDto>,
    private val json: String,
    reservasjonDao: ReservasjonDao,
    saksbehandlerDao: SaksbehandlerDao,
    overstyringDao: OverstyringDao
) : Hendelse, MacroCommand() {
    override val commands: List<Command> = listOf(
        OpprettSaksbehandlerCommand(
            oid = oid,
            navn = navn,
            epost = epost,
            ident = ident,
            saksbehandlerDao = saksbehandlerDao
        ),
        ReserverPersonCommand(oid, fødselsnummer, reservasjonDao),
        PersisterOverstyringCommand(
            oid = oid,
            eventId = id,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = orgnummer,
            begrunnelse = begrunnelse,
            overstyrteDager = overstyrteDager,
            overstyringDao = overstyringDao
        ),
        InvaliderSaksbehandlerOppgaveCommand(fødselsnummer, orgnummer, saksbehandlerDao)
    )

    override fun fødselsnummer() = fødselsnummer
    override fun toJson() = json

    internal class OverstyringRiver(
        rapidsConnection: RapidsConnection,
        private val mediator: HendelseMediator
    ) : River.PacketListener {
        private val logg = LoggerFactory.getLogger(this::class.java)
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "overstyr_tidslinje")
                    it.requireKey("aktørId")
                    it.requireKey("fødselsnummer")
                    it.requireKey("organisasjonsnummer")
                    it.requireKey("dager")
                    it.requireKey("@id")
                }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val hendelseId = UUID.fromString(packet["@id"].asText())
            logg.info(
                "Mottok overstyring med {}",
                keyValue("eventId", hendelseId)
            )
            sikkerLogg.info(
                "Mottok overstyring med {}, {}",
                keyValue("hendelseId", hendelseId),
                keyValue("hendelse", packet.toJson())
            )
            mediator.overstyring(packet, hendelseId, packet["fødselsnummer"].asText(), context)
        }
    }
}

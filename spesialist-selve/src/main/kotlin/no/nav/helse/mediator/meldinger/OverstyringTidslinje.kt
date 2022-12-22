package no.nav.helse.mediator.meldinger

import java.time.LocalDateTime
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.mediator.Hendelsefabrikk.Companion.toOverstyrteDagerDto
import no.nav.helse.mediator.OverstyringMediator
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.InvaliderSaksbehandlerOppgaveCommand
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OpprettSaksbehandlerCommand
import no.nav.helse.modell.kommando.PersisterOverstyringTidslinjeCommand
import no.nav.helse.modell.kommando.PubliserOverstyringCommand
import no.nav.helse.modell.kommando.ReserverPersonCommand
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spesialist.api.overstyring.OverstyringDagDto
import no.nav.helse.spesialist.api.reservasjon.ReservasjonDao
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Tar vare på overstyring fra saksbehandler og sletter den opprinnelige oppgaven i påvente av nytt
 * godkjenningsbehov fra spleis.
 *
 * Det er primært spleis som håndterer dette eventet.
 */
internal class OverstyringTidslinje(
    override val id: UUID,
    private val fødselsnummer: String,
    oid: UUID,
    navn: String,
    epost: String,
    ident: String,
    orgnummer: String,
    begrunnelse: String,
    overstyrteDager: List<OverstyringDagDto>,
    opprettet: LocalDateTime,
    private val json: String,
    reservasjonDao: ReservasjonDao,
    saksbehandlerDao: SaksbehandlerDao,
    overstyringDao: OverstyringDao,
    overstyringMediator: OverstyringMediator,
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
        PersisterOverstyringTidslinjeCommand(
            oid = oid,
            hendelseId = id,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = orgnummer,
            begrunnelse = begrunnelse,
            overstyrteDager = overstyrteDager,
            overstyringDao = overstyringDao,
            opprettet = opprettet,
        ),
        InvaliderSaksbehandlerOppgaveCommand(fødselsnummer, saksbehandlerDao),
        PubliserOverstyringCommand(
            eventName = "overstyr_tidslinje",
            hendelseId = id,
            json = json,
            overstyringMediator = overstyringMediator,
            overstyringDao = overstyringDao,
        )
    )

    override fun fødselsnummer() = fødselsnummer
    override fun toJson() = json

    internal class OverstyringTidslinjeRiver(
        rapidsConnection: RapidsConnection,
        private val mediator: HendelseMediator
    ) : River.PacketListener {
        private val logg = LoggerFactory.getLogger(this::class.java)
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "saksbehandler_overstyrer_tidslinje")
                    it.requireKey("@opprettet")
                    it.requireKey("aktørId")
                    it.requireKey("fødselsnummer")
                    it.requireKey("organisasjonsnummer")
                    it.requireKey("dager")
                    it.requireKey("@id")
                    it.requireKey("saksbehandlerOid")
                    it.requireKey("saksbehandlerNavn")
                    it.requireKey("saksbehandlerIdent")
                    it.requireKey("saksbehandlerEpost")
                    it.requireKey("begrunnelse")
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
            mediator.overstyringTidslinje(
                id = UUID.fromString(packet["@id"].asText()),
                fødselsnummer = packet["fødselsnummer"].asText(),
                oid = UUID.fromString(packet["saksbehandlerOid"].asText()),
                navn = packet["saksbehandlerNavn"].asText(),
                ident = packet["saksbehandlerIdent"].asText(),
                epost = packet["saksbehandlerEpost"].asText(),
                orgnummer = packet["organisasjonsnummer"].asText(),
                begrunnelse = packet["begrunnelse"].asText(),
                overstyrteDager = packet["dager"].toOverstyrteDagerDto(),
                opprettet = packet["@opprettet"].asLocalDateTime(),
                json = packet.toJson(),
                context = context,
            )
        }
    }
}

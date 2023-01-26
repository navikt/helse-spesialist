package no.nav.helse.mediator.meldinger

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.mediator.OverstyringMediator
import no.nav.helse.mediator.api.Arbeidsgiver
import no.nav.helse.mediator.api.Refusjonselement
import no.nav.helse.mediator.api.SubsumsjonDto
import no.nav.helse.mediator.api.arbeidsgiverelementer
import no.nav.helse.mediator.api.refusjonselementer
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.InvaliderSaksbehandlerOppgaveCommand
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OpprettSaksbehandlerCommand
import no.nav.helse.modell.kommando.PersisterOverstyringInntektCommand
import no.nav.helse.modell.kommando.PersisterOverstyringInntektOgRefusjonCommand
import no.nav.helse.modell.kommando.PubliserOverstyringCommand
import no.nav.helse.modell.kommando.ReserverPersonCommand
import no.nav.helse.modell.oppgave.OppgaveDao
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spesialist.api.reservasjon.ReservasjonDao
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Tar vare på overstyring av inntekt fra saksbehandler og sletter den opprinnelige oppgaven i påvente av nytt
 * godkjenningsbehov fra spleis.
 *
 * Det er primært spleis som håndterer dette eventet.
 */
internal class OverstyringInntektOgRefusjon(
    override val id: UUID,
    private val fødselsnummer: String,
    oid: UUID,
    navn: String,
    epost: String,
    ident: String,
    arbeidsgiver: List<Arbeidsgiver>,
    skjæringstidspunkt: LocalDate,
    opprettet: LocalDateTime,
    private val json: String,
    reservasjonDao: ReservasjonDao,
    saksbehandlerDao: SaksbehandlerDao,
    oppgaveDao: OppgaveDao,
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
        PersisterOverstyringInntektOgRefusjonCommand(
            oid = oid,
            hendelseId = id,
            fødselsnummer = fødselsnummer,
            arbeidsgiver = arbeidsgiver,
            skjæringstidspunkt = skjæringstidspunkt,
            opprettet = opprettet,
            overstyringDao = overstyringDao
        ),
        InvaliderSaksbehandlerOppgaveCommand(fødselsnummer, oppgaveDao),
        PubliserOverstyringCommand(
            eventName = "overstyr_inntekt_og_refusjon",
            hendelseId = id,
            json = json,
            overstyringMediator = overstyringMediator,
            overstyringDao = overstyringDao,
        )
    )

    override fun fødselsnummer() = fødselsnummer
    override fun toJson() = json

    internal class OverstyringInntektOgRefusjonRiver(
        rapidsConnection: RapidsConnection,
        private val mediator: HendelseMediator
    ) : River.PacketListener {
        private val logg = LoggerFactory.getLogger(this::class.java)
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "saksbehandler_overstyrer_inntekt_og_refusjon")
                    it.requireKey("@opprettet")
                    it.requireKey("aktørId")
                    it.requireKey("fødselsnummer")
                    it.requireKey("arbeidsgiver")
                    it.requireKey("skjæringstidspunkt")
                    it.requireKey("saksbehandlerIdent")
                    it.requireKey("saksbehandlerOid")
                    it.requireKey("saksbehandlerNavn")
                    it.requireKey("saksbehandlerEpost")
                    it.requireKey("@id")
                }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val hendelseId = UUID.fromString(packet["@id"].asText())
            logg.info(
                "Mottok overstyring av inntekt og refusjon med {}",
                keyValue("eventId", hendelseId)
            )
            sikkerLogg.info(
                "Mottok overstyring av inntekt og refusjon med {}, {}",
                keyValue("hendelseId", hendelseId),
                keyValue("hendelse", packet.toJson())
            )

            mediator.overstyringInntektOgRefusjon(
                id = UUID.fromString(packet["@id"].asText()),
                fødselsnummer = packet["fødselsnummer"].asText(),
                oid = UUID.fromString(packet["saksbehandlerOid"].asText()),
                navn = packet["saksbehandlerNavn"].asText(),
                ident = packet["saksbehandlerIdent"].asText(),
                epost = packet["saksbehandlerEpost"].asText(),
                arbeidsgiver = packet["arbeidsgiver"].arbeidsgiverelementer(),
                skjæringstidspunkt = packet["skjæringstidspunkt"].asLocalDate(),
                opprettet = packet["@opprettet"].asLocalDateTime(),
                json = packet.toJson(),
                context = context
            )
        }
    }
}

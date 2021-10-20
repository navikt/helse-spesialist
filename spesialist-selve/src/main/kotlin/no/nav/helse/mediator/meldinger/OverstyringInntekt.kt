package no.nav.helse.mediator.meldinger

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.FeatureToggle
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.kommando.*
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.rapids_rivers.*
import no.nav.helse.reservasjon.ReservasjonDao
import no.nav.helse.saksbehandler.SaksbehandlerDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

/**
 * Tar vare på overstyring av inntekt fra saksbehandler og sletter den opprinnelige oppgaven i påvente av nytt
 * godkjenningsbehov fra spleis.
 *
 * Det er primært spleis som håndterer dette eventet.
 */
internal class OverstyringInntekt(
    override val id: UUID,
    private val fødselsnummer: String,
    oid: UUID,
    navn: String,
    epost: String,
    ident: String,
    orgnummer: String,
    begrunnelse: String,
    forklaring: String,
    månedligInntekt: Double,
    skjæringstidspunkt: LocalDate,
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
        PersisterOverstyringInntektCommand(
            oid = oid,
            eventId = id,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = orgnummer,
            begrunnelse = begrunnelse,
            forklaring = forklaring,
            månedligInntekt = månedligInntekt,
            skjæringstidspunkt = skjæringstidspunkt,
            overstyringDao = overstyringDao
        ),
        InvaliderSaksbehandlerOppgaveCommand(fødselsnummer, orgnummer, saksbehandlerDao)
    )

    override fun fødselsnummer() = fødselsnummer
    override fun toJson() = json

    internal class OverstyringInntektRiver(
        rapidsConnection: RapidsConnection,
        private val mediator: HendelseMediator
    ) : River.PacketListener {
        private val logg = LoggerFactory.getLogger(this::class.java)
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "overstyr_inntekt")
                    it.requireKey("aktørId")
                    it.requireKey("fødselsnummer")
                    it.requireKey("organisasjonsnummer")
                    it.requireKey("månedligInntekt")
                    it.requireKey("begrunnelse")
                    it.requireKey("forklaring")
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
                "Mottok overstyring av inntekt med {}",
                keyValue("eventId", hendelseId)
            )
            sikkerLogg.info(
                "Mottok overstyring av inntekt med {}, {}",
                keyValue("hendelseId", hendelseId),
                keyValue("hendelse", packet.toJson())
            )

            mediator.overstyringInntekt(
                id = UUID.fromString(packet["@id"].asText()),
                fødselsnummer = packet["fødselsnummer"].asText(),
                oid = UUID.fromString(packet["saksbehandlerOid"].asText()),
                navn = packet["saksbehandlerNavn"].asText(),
                ident = packet["saksbehandlerIdent"].asText(),
                epost = packet["saksbehandlerEpost"].asText(),
                orgnummer = packet["organisasjonsnummer"].asText(),
                begrunnelse = packet["begrunnelse"].asText(),
                forklaring = packet["forklaring"].asText(),
                månedligInntekt = packet["månedligInntekt"].asDouble(),
                skjæringstidspunkt = packet["skjæringstidspunkt"].asLocalDate(),
                json = packet.toJson(),
                context = context
            )
        }
    }
}

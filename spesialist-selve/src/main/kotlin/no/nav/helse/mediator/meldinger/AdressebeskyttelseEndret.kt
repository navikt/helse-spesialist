package no.nav.helse.mediator.meldinger

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.kommando.AvvisVedStrengtFortroligAdressebeskyttelseCommand
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OppdaterPersoninfoCommand
import no.nav.helse.modell.oppgave.OppgaveDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class AdressebeskyttelseEndret(
    override val id: UUID,
    private val fødselsnummer: String,
    private val json: String,
    personDao: PersonDao,
    oppgaveDao: OppgaveDao,
    hendelseDao: HendelseDao,
    godkjenningMediator: GodkjenningMediator,
    utbetalingDao: UtbetalingDao
) : Hendelse, MacroCommand() {
    override val commands: List<Command> = listOf(
        OppdaterPersoninfoCommand(fødselsnummer, personDao, force = true),
        AvvisVedStrengtFortroligAdressebeskyttelseCommand(
            fødselsnummer = fødselsnummer,
            personDao = personDao,
            oppgaveDao = oppgaveDao,
            hendelseDao = hendelseDao,
            utbetalingDao = utbetalingDao,
            godkjenningMediator = godkjenningMediator
        )
    )

    override fun fødselsnummer(): String = fødselsnummer

    override fun toJson(): String = json

    internal class AdressebeskyttelseEndretRiver(
        rapidsConnection: RapidsConnection,
        private val mediator: HendelseMediator
    ) : River.PacketListener {
        private val logg = LoggerFactory.getLogger("adressebeskyttelse_endret_river")
        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "adressebeskyttelse_endret")
                    it.requireKey("@id", "fødselsnummer")
                }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val hendelseId = UUID.fromString(packet["@id"].asText())
            // Unngå å logge mer informasjon enn id her, vi ønsker ikke å lekke informasjon om adressebeskyttelse
            logg.info(
                "Mottok adressebeskyttelse_endret med {}",
                StructuredArguments.keyValue("hendelseId", hendelseId)
            )
            mediator.adressebeskyttelseEndret(
                packet,
                hendelseId,
                packet["fødselsnummer"].asText(),
                context
            )
        }
    }
}

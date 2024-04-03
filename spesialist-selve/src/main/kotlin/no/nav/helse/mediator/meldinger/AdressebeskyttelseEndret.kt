package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.Kommandofabrikk
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.modell.kommando.AvvisVedStrengtFortroligAdressebeskyttelseCommand
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OppdaterPersoninfoCommand
import no.nav.helse.modell.person.Person
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.rapids_rivers.JsonMessage

internal class AdressebeskyttelseEndret private constructor(
    override val id: UUID,
    private val fødselsnummer: String,
    private val json: String
): Personmelding {

    internal constructor(packet: JsonMessage): this(
        id = UUID.fromString(packet["@id"].asText()),
        fødselsnummer = packet["fødselsnummer"].asText(),
        json = packet.toJson()
    )

    internal constructor(jsonNode: JsonNode): this(
        id = jsonNode["@id"].asUUID(),
        fødselsnummer = jsonNode["fødselsnummer"].asText(),
        json = jsonNode.toString()
    )

    override fun fødselsnummer(): String = fødselsnummer
    override fun toJson(): String = json
    override fun behandle(person: Person, kommandofabrikk: Kommandofabrikk) {
        kommandofabrikk.iverksettAdressebeskyttelseEndret(this)
    }
}

internal class AdressebeskyttelseEndretCommand(
    fødselsnummer: String,
    personDao: PersonDao,
    oppgaveDao: OppgaveDao,
    godkjenningMediator: GodkjenningMediator
): MacroCommand() {
    override val commands: List<Command> = listOf(
        OppdaterPersoninfoCommand(fødselsnummer, personDao, force = true),
        AvvisVedStrengtFortroligAdressebeskyttelseCommand(
            fødselsnummer = fødselsnummer,
            personDao = personDao,
            oppgaveDao = oppgaveDao,
            godkjenningMediator = godkjenningMediator
        )
    )
}
package no.nav.helse.modell.person

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.Kommandofabrikk
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.mediator.oppgave.OppgaveMediator
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.ikkesuspenderendeCommand
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDateTime
import java.time.LocalDateTime
import java.util.UUID

internal class EndretEgenAnsattStatus private constructor(
    override val id: UUID,
    private val fødselsnummer: String,
    val erEgenAnsatt: Boolean,
    val opprettet: LocalDateTime,
    private val json: String,
) : Personmelding {
    internal constructor(packet: JsonMessage) : this(
        id = UUID.fromString(packet["@id"].asText()),
        fødselsnummer = packet["fødselsnummer"].asText(),
        erEgenAnsatt = packet["skjermet"].asBoolean(),
        opprettet = packet["@opprettet"].asLocalDateTime(),
        json = packet.toJson(),
    )
    internal constructor(jsonNode: JsonNode) : this(
        id = UUID.fromString(jsonNode["@id"].asText()),
        fødselsnummer = jsonNode["fødselsnummer"].asText(),
        erEgenAnsatt = jsonNode["skjermet"].asBoolean(),
        opprettet = jsonNode["@opprettet"].asLocalDateTime(),
        json = jsonNode.toString(),
    )

    override fun behandle(
        person: Person,
        kommandofabrikk: Kommandofabrikk,
    ) {
        kommandofabrikk.iverksettEndretAnsattStatus(this)
    }

    override fun fødselsnummer(): String = fødselsnummer

    override fun toJson(): String = json
}

internal class EndretEgenAnsattStatusCommand(
    private val fødselsnummer: String,
    erEgenAnsatt: Boolean,
    opprettet: LocalDateTime,
    egenAnsattDao: EgenAnsattDao,
    oppgaveMediator: OppgaveMediator,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            ikkesuspenderendeCommand("lagreEgenAnsattStatus") {
                egenAnsattDao.lagre(fødselsnummer, erEgenAnsatt, opprettet)
            },
            ikkesuspenderendeCommand("endretEgenAnsattStatus") {
                oppgaveMediator.endretEgenAnsattStatus(erEgenAnsatt, fødselsnummer)
            },
        )
}

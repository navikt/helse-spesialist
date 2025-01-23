package no.nav.helse.modell.person

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.TransactionalSession
import no.nav.helse.db.EgenAnsattDao
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.ikkesuspenderendeCommand
import java.time.LocalDateTime
import java.util.UUID

class EndretEgenAnsattStatus(
    override val id: UUID,
    private val fødselsnummer: String,
    val erEgenAnsatt: Boolean,
    val opprettet: LocalDateTime,
    private val json: String,
) : Personmelding {
    constructor(jsonNode: JsonNode) : this(
        id = UUID.fromString(jsonNode["@id"].asText()),
        fødselsnummer = jsonNode["fødselsnummer"].asText(),
        erEgenAnsatt = jsonNode["skjermet"].asBoolean(),
        opprettet = jsonNode["@opprettet"].asText().let(LocalDateTime::parse),
        json = jsonNode.toString(),
    )

    override fun behandle(
        person: Person,
        kommandostarter: Kommandostarter,
        transactionalSession: TransactionalSession,
    ) {
        kommandostarter { endretEgenAnsattStatus(this@EndretEgenAnsattStatus, transactionalSession) }
    }

    override fun fødselsnummer(): String = fødselsnummer

    override fun toJson(): String = json
}

internal class EndretEgenAnsattStatusCommand(
    private val fødselsnummer: String,
    erEgenAnsatt: Boolean,
    opprettet: LocalDateTime,
    egenAnsattDao: EgenAnsattDao,
    oppgaveService: OppgaveService,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            ikkesuspenderendeCommand("lagreEgenAnsattStatus") {
                egenAnsattDao.lagre(fødselsnummer, erEgenAnsatt, opprettet)
            },
            ikkesuspenderendeCommand("endretEgenAnsattStatus") {
                oppgaveService.endretEgenAnsattStatus(erEgenAnsatt, fødselsnummer)
            },
        )
}

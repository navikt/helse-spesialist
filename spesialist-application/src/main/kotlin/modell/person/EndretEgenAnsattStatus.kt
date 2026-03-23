package no.nav.helse.modell.person

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.ikkesuspenderendeCommand
import no.nav.helse.spesialist.application.PersonRepository
import no.nav.helse.spesialist.domain.Identitetsnummer
import java.time.LocalDateTime
import java.time.ZoneId
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
        person: LegacyPerson,
        kommandostarter: Kommandostarter,
        sessionContext: SessionContext,
    ) {
        kommandostarter { endretEgenAnsattStatus(this@EndretEgenAnsattStatus, sessionContext) }
    }

    override fun fødselsnummer(): String = fødselsnummer

    override fun toJson(): String = json
}

internal class EndretEgenAnsattStatusCommand(
    private val fødselsnummer: String,
    erEgenAnsatt: Boolean,
    opprettet: LocalDateTime,
    personRepository: PersonRepository,
    oppgaveService: OppgaveService,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            ikkesuspenderendeCommand("lagreEgenAnsattStatus") {
                val person = personRepository.finn(Identitetsnummer.fraString(fødselsnummer)) ?: return@ikkesuspenderendeCommand
                person.oppdaterEgenAnsattStatus(
                    erEgenAnsatt = erEgenAnsatt,
                    oppdatertTidspunkt = opprettet.atZone(ZoneId.of("Europe/Oslo")).toInstant(),
                )
                personRepository.lagre(person)
            },
            ikkesuspenderendeCommand("endretEgenAnsattStatus") {
                oppgaveService.endretEgenAnsattStatus(erEgenAnsatt, fødselsnummer)
            },
        )
}

package no.nav.helse.modell.person

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.mediator.oppgave.tilUtgåendeHendelse
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.ikkesuspenderendeCommand
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.loggInfo
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
        kommandostarter { endretEgenAnsattStatus(this@EndretEgenAnsattStatus) }
    }

    override fun fødselsnummer(): String = fødselsnummer

    override fun toJson(): String = json
}

internal class EndretEgenAnsattStatusCommand(
    private val fødselsnummer: String,
    erEgenAnsatt: Boolean,
    opprettet: LocalDateTime,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            ikkesuspenderendeCommand("lagreEgenAnsattStatus") { sessionContext: SessionContext, _: Outbox ->
                val person = sessionContext.personRepository.finn(Identitetsnummer.fraString(fødselsnummer)) ?: return@ikkesuspenderendeCommand
                person.oppdaterEgenAnsattStatus(
                    erEgenAnsatt = erEgenAnsatt,
                    oppdatertTidspunkt = opprettet.atZone(ZoneId.of("Europe/Oslo")).toInstant(),
                )
                sessionContext.personRepository.lagre(person)
            },
            ikkesuspenderendeCommand("endretEgenAnsattStatus") { sessionContext: SessionContext, outbox: Outbox ->
                val identitetsnummer = Identitetsnummer.fraString(fødselsnummer)
                val oppgave = sessionContext.oppgaveRepository.finnAktivForPerson(identitetsnummer) ?: return@ikkesuspenderendeCommand
                if (erEgenAnsatt) {
                    loggInfo("Legger til egenskap EGEN_ANSATT", "fødselsnummer" to fødselsnummer, "oppgaveId" to oppgave.id.value)
                    oppgave.leggTilEgenAnsatt()
                } else {
                    loggInfo("Fjerner egenskap EGEN_ANSATT", "fødselsnummer" to fødselsnummer, "oppgaveId" to oppgave.id.value)
                    oppgave.fjernEgenAnsatt()
                }
                oppgave.konsumerHendelser().forEach {
                    outbox.leggTil(identitetsnummer, it.tilUtgåendeHendelse(), "endret egenansatt-status")
                }
                sessionContext.oppgaveRepository.lagre(oppgave)
            },
        )
}

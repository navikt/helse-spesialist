package no.nav.helse.modell.person

import java.util.UUID
import no.nav.helse.mediator.meldinger.Personhendelse
import no.nav.helse.mediator.oppgave.OppgaveMediator
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.ikkesuspenderendeCommand

internal class EndretEgenAnsattStatus(
    override val id: UUID,
    private val fødselsnummer: String,
    val erEgenAnsatt: Boolean,
    private val json: String,
) : Personhendelse {

    override fun fødselsnummer(): String = fødselsnummer
    override fun toJson(): String = json
}

internal class EndretEgenAnsattStatusCommand(
    private val fødselsnummer: String,
    erEgenAnsatt: Boolean,
    oppgaveMediator: OppgaveMediator,
): MacroCommand() {
    override val commands: List<Command> = listOf(
        ikkesuspenderendeCommand("endretEgenAnsattStatus") {
            oppgaveMediator.endretEgenAnsattStatus(erEgenAnsatt, fødselsnummer)
        },
    )
}

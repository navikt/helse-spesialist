package no.nav.helse.modell.person

import java.util.UUID
import no.nav.helse.mediator.meldinger.Hendelse
import no.nav.helse.mediator.oppgave.OppgaveMediator
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.ikkesuspenderendeCommand

internal class EndretSkjermetinfo(
    override val id: UUID,
    private val fødselsnummer: String,
    erEgenAnsatt: Boolean,
    private val json: String,
    oppgaveMediator: OppgaveMediator,
) : Hendelse, MacroCommand() {
    override val commands: List<Command> = listOf(
        ikkesuspenderendeCommand("endretEgenAnsattStatus") {
            oppgaveMediator.endretEgenAnsattStatus(erEgenAnsatt, fødselsnummer)
        },
    )

    override fun fødselsnummer(): String = fødselsnummer
    override fun vedtaksperiodeId(): UUID? = null
    override fun toJson(): String = json
}

package no.nav.helse.mediator.meldinger

import java.util.UUID
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.kommando.AvvisVedStrengtFortroligAdressebeskyttelseCommand
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OppdaterPersoninfoCommand
import no.nav.helse.modell.oppgave.OppgaveDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.utbetaling.UtbetalingDao

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

}

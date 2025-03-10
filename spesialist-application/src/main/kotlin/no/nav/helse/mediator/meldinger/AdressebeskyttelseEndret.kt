package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.PersonDao
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.kommando.AvvisVedStrengtFortroligAdressebeskyttelseCommand
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OppdaterPersoninfoCommand
import no.nav.helse.modell.person.Person
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import java.util.UUID

class AdressebeskyttelseEndret(
    override val id: UUID,
    private val fødselsnummer: String,
    private val json: String,
) : Personmelding {
    constructor(jsonNode: JsonNode) : this(
        id = jsonNode["@id"].asUUID(),
        fødselsnummer = jsonNode["fødselsnummer"].asText(),
        json = jsonNode.toString(),
    )

    override fun fødselsnummer(): String = fødselsnummer

    override fun toJson(): String = json

    override fun behandle(
        person: Person,
        kommandostarter: Kommandostarter,
        sessionContext: SessionContext,
    ) {
        kommandostarter {
            adressebeskyttelseEndret(
                this@AdressebeskyttelseEndret,
                finnOppgavedata(fødselsnummer, sessionContext),
                sessionContext,
            )
        }
    }
}

internal class AdressebeskyttelseEndretCommand(
    fødselsnummer: String,
    personDao: PersonDao,
    oppgaveDao: OppgaveDao,
    godkjenningMediator: GodkjenningMediator,
    godkjenningsbehov: GodkjenningsbehovData?,
    utbetaling: Utbetaling?,
) : MacroCommand() {
    override val commands: List<Command> =
        buildList {
            add(OppdaterPersoninfoCommand(fødselsnummer, personDao, force = true))
            if (godkjenningsbehov != null) {
                check(utbetaling != null) { "Forventer å finne utbetaling for godkjenningsbehov med id=${godkjenningsbehov.id}" }
                add(
                    AvvisVedStrengtFortroligAdressebeskyttelseCommand(
                        personDao = personDao,
                        oppgaveDao = oppgaveDao,
                        godkjenningMediator = godkjenningMediator,
                        godkjenningsbehov = godkjenningsbehov,
                        utbetaling = utbetaling,
                    ),
                )
            }
        }
}

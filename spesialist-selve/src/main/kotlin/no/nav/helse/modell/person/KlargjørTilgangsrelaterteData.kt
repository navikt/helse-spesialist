package no.nav.helse.modell.person

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.EgenAnsattDao
import no.nav.helse.db.OpptegnelseDao
import no.nav.helse.db.PersonDao
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.egenansatt.KontrollerEgenAnsattstatus
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OppdaterPersonCommand.OppdaterEnhetCommand
import no.nav.helse.modell.kommando.OppdaterPersoninfoCommand
import no.nav.helse.modell.kommando.ikkesuspenderendeCommand
import no.nav.helse.spesialist.api.abonnement.PersonKlarTilVisning
import java.util.UUID

class KlargjørTilgangsrelaterteData(
    override val id: UUID,
    private val fødselsnummer: String,
    private val json: String,
) : Personmelding {
    constructor(jsonNode: JsonNode) : this(
        id = UUID.fromString(jsonNode["@id"].asText()),
        fødselsnummer = jsonNode["fødselsnummer"].asText(),
        json = jsonNode.toString(),
    )

    override fun behandle(
        person: Person,
        kommandostarter: Kommandostarter,
        sessionContext: SessionContext,
    ) {
        kommandostarter { klargjørTilgangsrelaterteData(this@KlargjørTilgangsrelaterteData, sessionContext) }
    }

    override fun fødselsnummer(): String = fødselsnummer

    override fun toJson(): String = json
}

internal class KlargjørTilgangsrelaterteDataCommand(
    fødselsnummer: String,
    personDao: PersonDao,
    egenAnsattDao: EgenAnsattDao,
    opptegnelseDao: OpptegnelseDao,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            OppdaterPersoninfoCommand(
                fødselsnummer = fødselsnummer,
                personDao = personDao,
                force = false,
            ),
            OppdaterEnhetCommand(fødselsnummer, personDao),
            KontrollerEgenAnsattstatus(
                fødselsnummer = fødselsnummer,
                egenAnsattDao = egenAnsattDao,
            ),
            ikkesuspenderendeCommand("opprettOptegnelse") {
                opptegnelseDao.opprettOpptegnelse(
                    fødselsnummer = fødselsnummer,
                    payload = PersonKlarTilVisning.toJson(),
                    type = OpptegnelseDao.OpptegnelseType.PERSON_KLAR_TIL_BEHANDLING,
                )
            },
            ikkesuspenderendeCommand("ferdigstillKlargjøring") {
                personDao.personKlargjort(fødselsnummer)
            },
        )
}

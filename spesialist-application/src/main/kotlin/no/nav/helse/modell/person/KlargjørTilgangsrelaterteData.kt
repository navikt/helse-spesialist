package no.nav.helse.modell.person

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.EgenAnsattDao
import no.nav.helse.db.PersonDao
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.egenansatt.KontrollerEgenAnsattstatus
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OppdaterEnhetCommand
import no.nav.helse.modell.kommando.OppdaterPersoninfoCommand
import no.nav.helse.modell.kommando.ikkesuspenderendeCommand
import no.nav.helse.spesialist.application.OpptegnelseRepository
import no.nav.helse.spesialist.application.PersonRepository
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Opptegnelse
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
        person: LegacyPerson,
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
    personRepository: PersonRepository,
    egenAnsattDao: EgenAnsattDao,
    opptegnelseRepository: OpptegnelseRepository,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            OppdaterPersoninfoCommand(
                identitetsnummer = Identitetsnummer.fraString(fødselsnummer),
                personRepository = personRepository,
                force = false,
            ),
            OppdaterEnhetCommand(fødselsnummer, personRepository),
            KontrollerEgenAnsattstatus(
                fødselsnummer = fødselsnummer,
                egenAnsattDao = egenAnsattDao,
            ),
            ikkesuspenderendeCommand("opprettOptegnelse") {
                val opptegnelse =
                    Opptegnelse.ny(
                        identitetsnummer = Identitetsnummer.fraString(fødselsnummer),
                        type = Opptegnelse.Type.PERSON_KLAR_TIL_BEHANDLING,
                    )
                opptegnelseRepository.lagre(opptegnelse)
            },
            ikkesuspenderendeCommand("ferdigstillKlargjøring") {
                personDao.personKlargjort(fødselsnummer)
            },
        )
}

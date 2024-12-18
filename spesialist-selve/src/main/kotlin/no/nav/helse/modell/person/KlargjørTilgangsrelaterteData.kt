package no.nav.helse.modell.person

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.TransactionalSession
import no.nav.helse.db.EgenAnsattRepository
import no.nav.helse.db.OpptegnelseRepository
import no.nav.helse.db.PersonRepository
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.egenansatt.KontrollerEgenAnsattstatus
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OppdaterPersonCommand.OppdaterEnhetCommand
import no.nav.helse.modell.kommando.OppdaterPersoninfoCommand
import no.nav.helse.modell.kommando.ikkesuspenderendeCommand
import no.nav.helse.spesialist.api.abonnement.OpptegnelseType.PERSON_KLAR_TIL_BEHANDLING
import no.nav.helse.spesialist.api.abonnement.PersonKlarTilVisning
import java.util.UUID

internal class KlargjørTilgangsrelaterteData(
    override val id: UUID,
    private val fødselsnummer: String,
    private val json: String,
) : Personmelding {
    internal constructor(jsonNode: JsonNode) : this(
        id = UUID.fromString(jsonNode["@id"].asText()),
        fødselsnummer = jsonNode["fødselsnummer"].asText(),
        json = jsonNode.toString(),
    )

    override fun behandle(
        person: Person,
        kommandostarter: Kommandostarter,
        transactionalSession: TransactionalSession,
    ) {
        kommandostarter { klargjørTilgangsrelaterteData(this@KlargjørTilgangsrelaterteData, transactionalSession) }
    }

    override fun fødselsnummer(): String = fødselsnummer

    override fun toJson(): String = json
}

internal class KlargjørTilgangsrelaterteDataCommand(
    fødselsnummer: String,
    personRepository: PersonRepository,
    egenAnsattRepository: EgenAnsattRepository,
    opptegnelseRepository: OpptegnelseRepository,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            OppdaterPersoninfoCommand(
                fødselsnummer = fødselsnummer,
                personRepository = personRepository,
                force = false,
            ),
            OppdaterEnhetCommand(fødselsnummer, personRepository),
            KontrollerEgenAnsattstatus(
                fødselsnummer = fødselsnummer,
                egenAnsattRepository = egenAnsattRepository,
            ),
            ikkesuspenderendeCommand("opprettOptegnelse") {
                opptegnelseRepository.opprettOpptegnelse(fødselsnummer, payload = PersonKlarTilVisning, type = PERSON_KLAR_TIL_BEHANDLING)
            },
            ikkesuspenderendeCommand("ferdigstillKlargjøring") {
                personRepository.personKlargjort(fødselsnummer)
            },
        )
}

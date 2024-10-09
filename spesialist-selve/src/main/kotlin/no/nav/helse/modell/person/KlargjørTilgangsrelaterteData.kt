package no.nav.helse.modell.person

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.EgenAnsattRepository
import no.nav.helse.db.PersonRepository
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.egenansatt.KontrollerEgenAnsattstatus
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OppdaterPersoninfoCommand
import no.nav.helse.rapids_rivers.JsonMessage
import java.util.UUID

internal class KlargjørTilgangsrelaterteData private constructor(
    override val id: UUID,
    private val fødselsnummer: String,
    private val json: String,
) : Personmelding {
    internal constructor(packet: JsonMessage) : this(
        id = packet["@id"].asUUID(),
        fødselsnummer = packet["fødselsnummer"].asText(),
        json = packet.toJson(),
    )

    internal constructor(jsonNode: JsonNode) : this(
        id = UUID.fromString(jsonNode["@id"].asText()),
        fødselsnummer = jsonNode["fødselsnummer"].asText(),
        json = jsonNode.toString(),
    )

    override fun behandle(
        person: Person,
        kommandostarter: Kommandostarter,
    ) {
        kommandostarter { klargjørTilgangsrelaterteData(this@KlargjørTilgangsrelaterteData) }
    }

    override fun fødselsnummer(): String = fødselsnummer

    override fun toJson(): String = json
}

internal class KlargjørTilgangsrelaterteDataCommand(
    fødselsnummer: String,
    personRepository: PersonRepository,
    egenAnsattRepository: EgenAnsattRepository,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            OppdaterPersoninfoCommand(
                fødselsnummer,
                personRepository = personRepository,
                force = false,
            ),
            KontrollerEgenAnsattstatus(
                fødselsnummer = fødselsnummer,
                egenAnsattRepository = egenAnsattRepository,
            ),
        )
}

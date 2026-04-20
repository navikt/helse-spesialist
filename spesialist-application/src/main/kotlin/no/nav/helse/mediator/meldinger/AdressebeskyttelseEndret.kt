package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.kommando.AvvisVedStrengtFortroligAdressebeskyttelseCommand
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OppdaterPersoninfoCommand
import no.nav.helse.modell.person.LegacyPerson
import no.nav.helse.spesialist.domain.Identitetsnummer
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
        person: LegacyPerson,
        kommandostarter: Kommandostarter,
        sessionContext: SessionContext,
    ) {
        kommandostarter {
            adressebeskyttelseEndret(
                this@AdressebeskyttelseEndret,
            )
        }
    }
}

internal class AdressebeskyttelseEndretCommand(
    identitetsnummer: Identitetsnummer,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            OppdaterPersoninfoCommand(identitetsnummer, force = true),
            AvvisVedStrengtFortroligAdressebeskyttelseCommand(
                identitetsnummer = identitetsnummer,
            ),
        )
}

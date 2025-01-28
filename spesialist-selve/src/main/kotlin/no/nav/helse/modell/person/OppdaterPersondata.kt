package no.nav.helse.modell.person

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.OpptegnelseDao
import no.nav.helse.db.PersonDao
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OppdaterInfotrygdutbetalingerHardt
import no.nav.helse.modell.kommando.ikkesuspenderendeCommand
import no.nav.helse.spesialist.api.abonnement.PersonOppdatertPayload
import java.time.LocalDate
import java.util.UUID

class OppdaterPersondata(
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
        kommandostarter { oppdaterPersondata(this@OppdaterPersondata, sessionContext) }
    }

    override fun fødselsnummer(): String = fødselsnummer

    override fun toJson(): String = json
}

internal class OppdaterPersondataCommand(
    fødselsnummer: String,
    førsteKjenteDagFinner: () -> LocalDate,
    personDao: PersonDao,
    opptegnelseDao: OpptegnelseDao,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            OppdaterInfotrygdutbetalingerHardt(fødselsnummer, personDao, førsteKjenteDagFinner),
            ikkesuspenderendeCommand("opprettOpptegnelse") {
                opptegnelseDao.opprettOpptegnelse(
                    fødselsnummer,
                    PersonOppdatertPayload.toJson(),
                    OpptegnelseDao.OpptegnelseType.PERSONDATA_OPPDATERT,
                )
            },
        )
}

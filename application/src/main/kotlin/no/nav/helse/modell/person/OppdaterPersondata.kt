package no.nav.helse.modell.person

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.PersonDao
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OppdaterInfotrygdutbetalingerHardt
import no.nav.helse.modell.kommando.ikkesuspenderendeCommand
import no.nav.helse.spesialist.application.OpptegnelseRepository
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Opptegnelse
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
        person: LegacyPerson,
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
    førsteKjenteDagFinner: () -> LocalDate?,
    personDao: PersonDao,
    opptegnelseRepository: OpptegnelseRepository,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            OppdaterInfotrygdutbetalingerHardt(fødselsnummer, personDao, førsteKjenteDagFinner),
            ikkesuspenderendeCommand("opprettOpptegnelse") {
                val opptegnelse =
                    Opptegnelse.ny(
                        identitetsnummer = Identitetsnummer.fraString(fødselsnummer),
                        type = Opptegnelse.Type.PERSONDATA_OPPDATERT,
                    )
                opptegnelseRepository.lagre(opptegnelse)
            },
        )
}
